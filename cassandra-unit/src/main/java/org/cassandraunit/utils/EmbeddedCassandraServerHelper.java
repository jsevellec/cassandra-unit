package org.cassandraunit.utils;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraServerHelper {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCassandraServerHelper.class);

    public static final long DEFAULT_STARTUP_TIMEOUT = 20000;
    public static final String DEFAULT_TMP_DIR = "target/embeddedCassandra";
    /** Default configuration file. Starts embedded cassandra under the well known ports */
    public static final String DEFAULT_CASSANDRA_YML_FILE = "cu-cassandra.yaml";
    /** Configuration file which starts the embedded cassandra on a random free port */
    public static final String CASSANDRA_RNDPORT_YML_FILE = "cu-cassandra-rndport.yaml";
    public static final String DEFAULT_LOG4J_CONFIG_FILE = "/log4j-embedded-cassandra.properties";
    private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";
    private static final String INTERNAL_CASSANDRA_AUTH_KEYSPACE = "system_auth";
    private static final String INTERNAL_CASSANDRA_DISTRIBUTED_KEYSPACE = "system_distributed";
    private static final String INTERNAL_CASSANDRA_SCHEMA_KEYSPACE = "system_schema";
    private static final String INTERNAL_CASSANDRA_TRACES_KEYSPACE = "system_traces";

    private static final Set<String> systemKeyspaces = new HashSet<>(Arrays.asList(INTERNAL_CASSANDRA_KEYSPACE,
            INTERNAL_CASSANDRA_AUTH_KEYSPACE, INTERNAL_CASSANDRA_DISTRIBUTED_KEYSPACE,
            INTERNAL_CASSANDRA_SCHEMA_KEYSPACE, INTERNAL_CASSANDRA_TRACES_KEYSPACE));

    public static Predicate<KeyspaceMetadata> nonSystemKeyspaces() {
        return metadata -> !systemKeyspaces.contains(metadata.getName());
    }

    private static CassandraDaemon cassandraDaemon = null;
    private static String launchedYamlFile;
    private static com.datastax.driver.core.Cluster cluster;
    private static Session session;

    public static void startEmbeddedCassandra() throws TTransportException, IOException, InterruptedException, ConfigurationException {
        startEmbeddedCassandra(DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(long timeout) throws TTransportException, ConfigurationException, IOException {
        startEmbeddedCassandra(DEFAULT_CASSANDRA_YML_FILE, timeout);
    }

    public static void startEmbeddedCassandra(String yamlFile) throws TTransportException, IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(String yamlFile, long timeout) throws TTransportException, IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, DEFAULT_TMP_DIR, timeout);
    }

    public static void startEmbeddedCassandra(String yamlFile, String tmpDir) throws TTransportException, IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, tmpDir, DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(String yamlFile, String tmpDir, long timeout) throws TTransportException, IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        if (!StringUtils.startsWith(yamlFile, "/")) {
            yamlFile = "/" + yamlFile;
        }

        rmdir(tmpDir);
        copy(yamlFile, tmpDir);
        File file = new File(tmpDir + yamlFile);
        readAndAdaptYaml(file);
        startEmbeddedCassandra(file, tmpDir, timeout);
    }

    public static void startEmbeddedCassandra(File file, long timeout) throws TTransportException, IOException, ConfigurationException {
        startEmbeddedCassandra(file, DEFAULT_TMP_DIR, timeout);
    }
        /**
         * Set embedded cassandra up and spawn it in a new thread.
         *
         * @throws TTransportException
         * @throws IOException
         * @throws ConfigurationException
         */
    public static void startEmbeddedCassandra(File file, String tmpDir, long timeout) throws IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        checkConfigNameForRestart(file.getAbsolutePath());

        log.debug("Starting cassandra...");
        log.debug("Initialization needed");

        System.setProperty("cassandra.config", "file:" + file.getAbsolutePath());
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "false"); // JNA doesnt cope with relocated netty
        System.setProperty("cassandra.unsafesystem", "true"); // disable fsync for a massive speedup on old platters

        // If there is no log4j config set already, set the default config
        if (System.getProperty("log4j.configuration") == null) {
            copy(DEFAULT_LOG4J_CONFIG_FILE, tmpDir);
            System.setProperty("log4j.configuration", "file:" + tmpDir + DEFAULT_LOG4J_CONFIG_FILE);
        }

        DatabaseDescriptor.daemonInitialization();

        cleanupAndLeaveDirs();
        final CountDownLatch startupLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            cassandraDaemon = new CassandraDaemon();
            cassandraDaemon.activate();
            startupLatch.countDown();
        });
        try {
            if (!startupLatch.await(timeout, MILLISECONDS)) {
                log.error("Cassandra daemon did not start after " + timeout + " ms. Consider increasing the timeout");
                throw new AssertionError("Cassandra daemon did not start within timeout");
            }

            QueryOptions queryOptions = new QueryOptions();
            queryOptions.setRefreshSchemaIntervalMillis(0);
            queryOptions.setRefreshNodeIntervalMillis(0);
            queryOptions.setRefreshNodeListIntervalMillis(0);
            cluster = com.datastax.driver.core.Cluster.builder()
                .addContactPoints(EmbeddedCassandraServerHelper.getHost())
                .withPort(EmbeddedCassandraServerHelper.getNativeTransportPort())
                .withQueryOptions(queryOptions)
                .build();

            session = cluster.connect();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                session.close();
                cluster.close();
            }));
        } catch (InterruptedException e) {
            log.error("Interrupted waiting for Cassandra daemon to start:", e);
            throw new AssertionError(e);
        } finally {
            executor.shutdown();
        }
    }

    private static void checkConfigNameForRestart(String yamlFile) {
        boolean wasPreviouslyLaunched = launchedYamlFile != null;
        if (wasPreviouslyLaunched && !launchedYamlFile.equals(yamlFile)) {
            throw new UnsupportedOperationException("We can't launch two Cassandra configurations in the same JVM instance");
        }
        launchedYamlFile = yamlFile;
    }

    /**
     * Now deprecated, previous version was not fully operating.
     * This is now an empty method, will be pruned in future versions.
     */
    @Deprecated
    public static void stopEmbeddedCassandra() {
        log.warn("EmbeddedCassandraServerHelper.stopEmbeddedCassandra() is now deprecated, " +
                "previous version was not fully operating");
        cassandraDaemon.deactivate();
    }

    /**
     * drop all keyspaces (expect system)
     */
    public static void cleanEmbeddedCassandra() {
        dropKeyspaces();
    }

    /**
     * truncate data in keyspace, except specified tables
     */
    public static void cleanDataEmbeddedCassandra(String keyspace, String... excludedTables) {
            cleanDataWithNativeDriver(keyspace, excludedTables);
    }

    public static com.datastax.driver.core.Cluster getCluster() {
        return cluster;
    }

    public static Session getSession() {
        return session;
    }

    /**
     * Get the embedded cassandra cluster name
     * 
     * @return the cluster name
     */
    public static String getClusterName() {
        return DatabaseDescriptor.getClusterName();
    }
    
    /**
     * Get embedded cassandra host.
     * 
     * @return the cassandra host
     */
    public static String getHost() {
        return DatabaseDescriptor.getRpcAddress().getHostName();
    }
    
    /**
     * Get embedded cassandra RPC port.
     *
     * @return the cassandra RPC port
     */
    public static int getRpcPort() {
        return DatabaseDescriptor.getRpcPort();
    }

    /**
     * Get embedded cassandra native transport port.
     *
     * @return the cassandra native transport port.
     */
    public static int getNativeTransportPort() {
        return DatabaseDescriptor.getNativeTransportPort();
    }

    private static void cleanDataWithNativeDriver(String keyspace, String... excludedTables) {
        HashSet<String> excludedTableList = new HashSet<>(Arrays.asList(excludedTables));
        cluster.getMetadata().getKeyspace(keyspace).getTables().stream()
                .map(table -> table.getName())
                .filter(tableName -> !excludedTableList.contains(tableName))
                .forEach(tableName -> session.execute("truncate table " + keyspace + "." + tableName));
    }

    private static void dropKeyspaces() {
            dropKeyspacesWithNativeDriver();
    }

    private static void dropKeyspacesWithNativeDriver() {
        cluster.getMetadata().getKeyspaces().stream()
                .filter(nonSystemKeyspaces())
                .forEach(keyspace -> session.execute("DROP KEYSPACE " + keyspace.getName()));
    }
    
    private static void rmdir(String dir) {
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            FileUtils.deleteRecursive(dirFile);
        }
    }

    /**
     * Copies a resource from within the jar to a directory.
     *
     * @param resource
     * @param directory
     * @throws IOException
     */
    private static void copy(String resource, String directory) throws IOException {
        mkdir(directory);
        String fileName = resource.substring(resource.lastIndexOf("/") + 1);
        InputStream from = EmbeddedCassandraServerHelper.class.getResourceAsStream(resource);
        Files.copy(from, Paths.get(directory + System.getProperty("file.separator") + fileName));
    }

    /**
     * Creates a directory
     *
     * @param dir
     * @throws IOException
     */
    private static void mkdir(String dir) {
        FileUtils.createDirectory(dir);
    }

    private static void cleanupAndLeaveDirs() throws IOException {
        mkdirs();
        cleanup();
        mkdirs();
        CommitLog commitLog = CommitLog.instance;
        commitLog.resetUnsafe(true); // cleanup screws w/ CommitLog, this brings it back to safe state
    }

    private static void cleanup() {
        // clean up commitlog and data directory which are stored as data directory/table/data files
        List<String> directories = new ArrayList<>(Arrays.asList(DatabaseDescriptor.getAllDataFileLocations()));
        directories.add(DatabaseDescriptor.getCommitLogLocation());
        for (String dirName : directories) {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }
    }

    public static void mkdirs() {
        DatabaseDescriptor.createAllDirectories();
    }

    private static void readAndAdaptYaml(File cassandraConfig) throws IOException {
        String yaml = readYamlFileToString(cassandraConfig);

        // read the ports and replace them if zero. dump back the changed string, preserving comments (thus no snakeyaml)
        Pattern portPattern = Pattern.compile("^([a-z_]+)_port:\\s*([0-9]+)\\s*$", Pattern.MULTILINE);
        Matcher portMatcher = portPattern.matcher(yaml);
        StringBuffer sb = new StringBuffer();
        boolean replaced = false;
        while (portMatcher.find()) {
            String portName = portMatcher.group(1);
            int portValue = Integer.parseInt(portMatcher.group(2));
            String replacement;
            if (portValue == 0) {
                portValue = findUnusedLocalPort();
                replacement = portName + "_port: " + portValue;
                replaced = true;
            } else {
                replacement = portMatcher.group(0);
            }
            portMatcher.appendReplacement(sb, replacement);
        }
        portMatcher.appendTail(sb);

        if (replaced) {
            writeStringToYamlFile(cassandraConfig, sb.toString());
        }
    }
    
    private static String readYamlFileToString(File yamlFile) throws IOException {
        // using UnicodeReader to read the correct encoding according to BOM
        try (UnicodeReader reader = new UnicodeReader(new FileInputStream(yamlFile))) {
            StringBuilder sb = new StringBuilder();
            char[] cbuf = new char[1024];

            int readden = reader.read(cbuf);
            while(readden >= 0) {
                sb.append(cbuf, 0, readden);
                readden = reader.read(cbuf);
            }
            return sb.toString();
        }
    }

    private static void writeStringToYamlFile(File yamlFile, String yaml) throws IOException {
        // write utf-8 without BOM
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(yamlFile), "utf-8")) {
            writer.write(yaml);
        }
    }

    private static int findUnusedLocalPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
