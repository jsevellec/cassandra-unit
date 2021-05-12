package org.cassandraunit.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String INTERNAL_CASSANDRA_VIEWS_KEYSPACE = "system_views";
    private static final String INTERNAL_CASSANDRA_VIRTUAL_SCHEMA_KEYSPACE = "system_virtual_schema";

    private static final Set<String> systemKeyspaces = new HashSet<>(Arrays.asList(INTERNAL_CASSANDRA_KEYSPACE,
            INTERNAL_CASSANDRA_AUTH_KEYSPACE, INTERNAL_CASSANDRA_DISTRIBUTED_KEYSPACE,
            INTERNAL_CASSANDRA_SCHEMA_KEYSPACE, INTERNAL_CASSANDRA_TRACES_KEYSPACE,
            INTERNAL_CASSANDRA_VIEWS_KEYSPACE, INTERNAL_CASSANDRA_VIRTUAL_SCHEMA_KEYSPACE));

    public static Predicate<String> nonSystemKeyspaces() {
        return keyspace -> !systemKeyspaces.contains(keyspace);
    }

    private static CassandraDaemon cassandraDaemon = null;
    private static String launchedYamlFile;
    private static CqlSession session;

    public static void startEmbeddedCassandra() throws IOException, InterruptedException, ConfigurationException {
        startEmbeddedCassandra(DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(long timeout) throws ConfigurationException, IOException {
        startEmbeddedCassandra(DEFAULT_CASSANDRA_YML_FILE, timeout);
    }

    public static void startEmbeddedCassandra(String yamlFile) throws IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(String yamlFile, long timeout) throws IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, DEFAULT_TMP_DIR, timeout);
    }

    public static void startEmbeddedCassandra(String yamlFile, String tmpDir) throws IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, tmpDir, DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(String yamlFile, String tmpDir, long timeout) throws IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        if (!StringUtils.startsWith(yamlFile, "/")) {
            yamlFile = "/" + yamlFile;
        }

        rmdir(tmpDir);
        File file = copy(yamlFile, tmpDir).toFile();
        readAndAdaptYaml(file);
        startEmbeddedCassandra(file, tmpDir, timeout);
    }

    public static void startEmbeddedCassandra(File file, long timeout) throws IOException, ConfigurationException {
        startEmbeddedCassandra(file, DEFAULT_TMP_DIR, timeout);
    }
        /**
         * Set embedded cassandra up and spawn it in a new thread.
         *
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

        String cassandraConfigFilePath = file.getAbsolutePath();
        cassandraConfigFilePath = (cassandraConfigFilePath.startsWith("/") ? "file://" : "file:/") + cassandraConfigFilePath;
        System.setProperty("cassandra.config", cassandraConfigFilePath);
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "false"); // JNA doesnt cope with relocated netty
        System.setProperty("cassandra.unsafesystem", "true"); // disable fsync for a massive speedup on old platters

        // If there is no log4j config set already, set the default config
        if (System.getProperty("log4j.configuration") == null) {
            copy(DEFAULT_LOG4J_CONFIG_FILE, tmpDir);
            String log4jConfiguration = "file:/" + tmpDir + DEFAULT_LOG4J_CONFIG_FILE;
            System.setProperty("log4j.configuration", log4jConfiguration);
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
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (session != null) session.close();
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
        if (session != null) {
            dropKeyspaces();
        }
    }

    /**
     * truncate data in keyspace, except specified tables
     */
    public static void cleanDataEmbeddedCassandra(String keyspace, String... excludedTables) {
            if (session != null) {
                cleanDataWithNativeDriver(keyspace, excludedTables);
            }
    }

    public static CqlSession getSession() {
        initSession();
        return session;
    }

    private static synchronized void initSession() {
        if (session == null) {
            DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(0))
                    .withInt(DefaultDriverOption.METADATA_SCHEMA_MAX_EVENTS, 1)
                    .build();
            session = CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(EmbeddedCassandraServerHelper.getHost(), EmbeddedCassandraServerHelper.getNativeTransportPort()))
                    .withConfigLoader(configLoader)
                    .withLocalDatacenter("datacenter1")
                    .build();
        }
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
     * Get embedded cassandra native transport port.
     *
     * @return the cassandra native transport port.
     */
    public static int getNativeTransportPort() {
        return DatabaseDescriptor.getNativeTransportPort();
    }

    private static void cleanDataWithNativeDriver(String keyspace, String... excludedTables) {
        HashSet<String> excludedTableList = new HashSet<>(Arrays.asList(excludedTables));

        session.getMetadata().getKeyspace(keyspace).get().getTables().values().stream()
                .map(table -> table.getName())
                .filter(tableName -> !excludedTableList.contains(tableName))
                .map(tableName -> keyspace + "." + tableName)
                .forEach(CqlOperations.truncateTable(session));
    }

    private static void dropKeyspaces() {
            dropKeyspacesWithNativeDriver();
    }

    private static void dropKeyspacesWithNativeDriver() {
        session.getMetadata().getKeyspaces().values().stream()
                .map(keyspaceMetadata -> keyspaceMetadata.getName().toString())
                .filter(nonSystemKeyspaces())
                .forEach(CqlOperations.dropKeyspace(session));
    }

    private static void deleteRecursive(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        try {
            Files.delete(dir.toPath());
        } catch (Throwable t) {
            throw new FSWriteError(t, dir);
        }
    }
    
    private static void rmdir(String dir) {
        deleteRecursive(new File(dir));
    }

    /**
     * Copies a resource from within the jar to a directory.
     *
     * @param resource
     * @param directory
     * @throws IOException
     */
    private static Path copy(String resource, String directory) throws IOException {
        mkdir(directory);
        String fileName = resource.substring(resource.lastIndexOf("/") + 1);
        InputStream from = EmbeddedCassandraServerHelper.class.getResourceAsStream(resource);
        Path copyName = Paths.get(directory, fileName);
        Files.copy(from, copyName);
        return copyName;
    }

    /**
     * Creates a directory
     *
     * @param dir
     */
    private static void mkdir(String dir) {
        File dirFile = new File(dir);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            throw new FSWriteError(new IOException("Failed to mkdirs " + dir), dir);
        }
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
            rmdir(dirName);
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
