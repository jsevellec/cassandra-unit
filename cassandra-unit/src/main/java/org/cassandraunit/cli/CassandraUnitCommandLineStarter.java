package org.cassandraunit.cli;

import com.datastax.driver.core.Cluster;
import org.apache.commons.cli.*;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.io.File;
import java.io.IOException;

public class CassandraUnitCommandLineStarter {

    private static final String LOCALHOST = "localhost";
    private final static CommandLineParser commandLineParser = new PosixParser();
    private final static Options options = new Options();
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
        String file = commandLine.getOptionValue("f");
        String yamlFile = commandLine.getOptionValue("y");
        String timeout = commandLine.getOptionValue("t");

        try {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(new File(yamlFile), "temp", Long.parseLong(timeout));
            if (file != null) {
                dataSetLoad(LOCALHOST, port, file);
            }
        } catch (TTransportException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void dataSetLoad(String host, String port, String file) {
        Cluster cluster = Cluster.builder()
                .addContactPoints(host)
                .withPort(Integer.parseInt(port))
                .build();
        CQLDataLoader dataLoader = new CQLDataLoader(cluster.connect());
        dataLoader.load(new FileCQLDataSet(file, false));
        System.out.println("Loading completed");
    }

    private static boolean containBadReplicationFactorArgumentValue() {
        String replicationFactor = commandLine.getOptionValue("r");
        if (replicationFactor != null && !replicationFactor.trim().isEmpty()) {
            try {
                Integer.parseInt(replicationFactor);
                return false;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage(String message) {
        System.out.println(message);
        printUsage();
    }

    private static void initOptions() {
        options.addOption(OptionBuilder.withLongOpt("file").hasArg().withDescription("dataset to load").create("f"));
        options.addOption(OptionBuilder.withLongOpt("port").hasArg().withDescription("target port").create("p"));
        options.addOption(OptionBuilder.withLongOpt("yaml").hasArg().withDescription("yaml file (required)").create("y"));
        options.addOption(OptionBuilder.withLongOpt("timeout").hasArg().withDescription("start up timeout (required)").create("t"));
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("CassandraUnitStarter is a tool to start a cassandra instance", options);
    }
}
