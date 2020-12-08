package org.cassandraunit.cli;

import com.datastax.oss.driver.api.core.CqlSession;
import org.apache.commons.cli.*;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class CassandraUnitCommandLineStarter {

    private static final String LOCALHOST = "localhost";
    private final static CommandLineParser commandLineParser = new PosixParser();
    private final static Options options = new Options();
    private static final String CASSANDRA_YAML_TEMPLATE = "directory/samples/cassandra.yaml";
    private static final String CASSANDRA_YAML = "cassandra.yaml";
    private static CommandLine commandLine = null;

    public static void main(String[] args) {
        boolean exit = parseCommandLine(args);
        if (exit) {
            System.exit(1);
        } else {
            load();
        }
    }

    private static boolean parseCommandLine(String[] args) {
        initOptions();
        boolean exit = false;
        try {
            commandLine = commandLineParser.parse(options, args);
            if (commandLine.getOptions().length == 0) {
                exit = true;
                printUsage();
            } else {
                if (containBadReplicationFactorArgumentValue()) {
                    printUsage("Bad argument value for option r");
                    exit = true;
                }
            }
        } catch (ParseException e) {
            printUsage(e.getMessage());
            exit = true;
        }

        return exit;
    }

    protected static void load() {
        System.out.println("Starting Cassandra...");
        String port = commandLine.getOptionValue("p");
        String schemaOption = commandLine.getOptionValue("s");
        String installationFolder = commandLine.getOptionValue("d");
        String timeout = commandLine.getOptionValue("t");
        String tmpDir = installationFolder + "/temp";

        if (!hasValidValue(timeout)) {
            timeout = "20000";
        }

        Path cassandraYamlPath = Paths.get(CASSANDRA_YAML_TEMPLATE.replaceFirst("directory", installationFolder));
        try (Stream<String> input = Files.lines(cassandraYamlPath);
             PrintWriter output = new PrintWriter(new File(installationFolder, CASSANDRA_YAML), "UTF-8")) {
            input.map(line -> line.replace("9042", port))
                    .map(line -> line.replace("temp/", tmpDir + "/"))
                    .forEachOrdered(output::println);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(new File(installationFolder, CASSANDRA_YAML), tmpDir, Long.parseLong(timeout));
            if (hasValidValue(schemaOption)) {
                String[] schemas = schemaOption.split(",");
                for (String schema : schemas) {
                    dataSetLoad(LOCALHOST, port, schema);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void dataSetLoad(String host, String port, String file) {
        CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, Integer.parseInt(port)))
                .withLocalDatacenter("datacenter1")
                .build();
        CQLDataLoader dataLoader = new CQLDataLoader(session);
        dataLoader.load(new FileCQLDataSet(file));
        System.out.println("Loading completed");
    }

    private static boolean containBadReplicationFactorArgumentValue() {
        String replicationFactor = commandLine.getOptionValue("r");
        if (hasValidValue(replicationFactor)) {
            try {
                Integer.parseInt(replicationFactor);
                return false;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValidValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void printUsage(String message) {
        System.out.println(message);
        printUsage();
    }

    private static void initOptions() {
        options.addOption(OptionBuilder.withLongOpt("schema").hasArg().withDescription("schema to load").create("s"));
        options.addOption(OptionBuilder.withLongOpt("port").hasArg().withDescription("target port").create("p"));
        options.addOption(OptionBuilder.withLongOpt("timeout").hasArg().withDescription("start up timeout").create("t"));
        options.addOption(OptionBuilder.withLongOpt("directory").hasArg().withDescription("installation directory").create("d"));
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("CassandraUnitStarter is a tool to start a cassandra instance", options);
    }
}
