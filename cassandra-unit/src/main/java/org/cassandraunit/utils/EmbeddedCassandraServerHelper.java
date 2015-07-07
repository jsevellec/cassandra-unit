package org.cassandraunit.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;

/**
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraServerHelper {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCassandraServerHelper.class);

    public static final long DEFAULT_STARTUP_TIMEOUT = 10000;
    public static final String DEFAULT_TMP_DIR = "target/embeddedCassandra";
    /** Default configuration file. Starts embedded cassandra under the well known ports */
    public static final String DEFAULT_CASSANDRA_YML_FILE = "cu-cassandra.yaml";
    /** Configuration file which starts the embedded cassandra on a random free port */
    public static final String CASSANDRA_RNDPORT_YML_FILE = "cu-cassandra-rndport.yaml";
    public static final String DEFAULT_LOG4J_CONFIG_FILE = "/log4j-embedded-cassandra.properties";
    private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";
    private static final String INTERNAL_CASSANDRA_AUTH_KEYSPACE = "system_auth";
    private static final String INTERNAL_CASSANDRA_TRACES_KEYSPACE = "system_traces";

    private static CassandraDaemon cassandraDaemon = null;
    private static String launchedYamlFile;

    public static void startEmbeddedCassandra() throws TTransportException, IOException, InterruptedException, ConfigurationException {
        startEmbeddedCassandra(DEFAULT_STARTUP_TIMEOUT);
    }

    public static void startEmbeddedCassandra(long timeout) throws TTransportException, IOException, InterruptedException, ConfigurationException {
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

    /**
     * Set embedded cassandra up and spawn it in a new thread.
     *
     * @throws TTransportException
     * @throws IOException
     * @throws ConfigurationException
     */
    public static void startEmbeddedCassandra(File file, String tmpDir, long timeout) throws TTransportException, IOException, ConfigurationException {
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

        // If there is no log4j config set already, set the default config
        if (System.getProperty("log4j.configuration") == null) {
            copy(DEFAULT_LOG4J_CONFIG_FILE, tmpDir);
            System.setProperty("log4j.configuration", "file:" + tmpDir + DEFAULT_LOG4J_CONFIG_FILE);
        }

        cleanupAndLeaveDirs();
        final CountDownLatch startupLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                cassandraDaemon = new CassandraDaemon();
                cassandraDaemon.activate();
                startupLatch.countDown();
            }
        });
        try {
            if (!startupLatch.await(timeout, MILLISECONDS)) {
                log.error("Cassandra daemon did not start after " + timeout + "ms. Consider increasing the timeout");
                throw new AssertionError("Cassandra daemon did not start within timeout");
            }
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
    }

     /**
     * drop all keyspaces (expect system)
     */
    public static void cleanEmbeddedCassandra() {
        dropKeyspaces();
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

   private static void dropKeyspaces() {
        if (hasHector()) {
            dropKeyspacesWithHector();
        } else {
            dropKeyspacesWithNativeDriver();
        }
    }

    private static boolean hasHector() {
        boolean hector = false;
        try {
            new CassandraHostConfigurator("");
            hector = true;
        } catch(NoClassDefFoundError err) {
            hector = false;
        }
        return hector;
    }
    
    private static void dropKeyspacesWithHector() {
        String host = DatabaseDescriptor.getRpcAddress().getHostName();
        int port = DatabaseDescriptor.getRpcPort();
        log.debug("Cleaning cassandra keyspaces on " + host + ":" + port);
        Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator(host + ":" + port));
        /* get all keyspace */
        List<KeyspaceDefinition> keyspaces = cluster.describeKeyspaces();

        /* drop all keyspace except internal cassandra keyspace */
        for (KeyspaceDefinition keyspaceDefinition : keyspaces) {
            String keyspaceName = keyspaceDefinition.getName();

            if (!isSystemKeyspaceName(keyspaceName)) {
                cluster.dropKeyspace(keyspaceName);
            }
        }
    }

    private static void dropKeyspacesWithNativeDriver() {
        String host = DatabaseDescriptor.getRpcAddress().getHostName();
        int port = DatabaseDescriptor.getNativeTransportPort();
        try (com.datastax.driver.core.Cluster cluster =
             com.datastax.driver.core.Cluster.builder().addContactPoint(host).withPort(port).build();
             com.datastax.driver.core.Session session = cluster.connect()) {
            List<String> keyspaces = new ArrayList<String>();
            for (com.datastax.driver.core.KeyspaceMetadata keyspace : cluster.getMetadata().getKeyspaces()) {
                if (!isSystemKeyspaceName(keyspace.getName())) {
                    keyspaces.add(keyspace.getName());
                }
            }
            for (String keyspace : keyspaces) {
                session.execute("DROP KEYSPACE " + keyspace);
            }
        }
    }
    
    private static boolean isSystemKeyspaceName(String keyspaceName) {
        return    INTERNAL_CASSANDRA_KEYSPACE.equals(keyspaceName) 
               || INTERNAL_CASSANDRA_AUTH_KEYSPACE.equals(keyspaceName)
               || INTERNAL_CASSANDRA_TRACES_KEYSPACE.equals(keyspaceName);
    }
    

    private static void rmdir(String dir) throws IOException {
        File dirFile = new File(dir);
        if (dirFile.exists()) {
            FileUtils.deleteRecursive(new File(dir));
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
        File file = new File(directory + System.getProperty("file.separator") + fileName);
        try (
            InputStream is = EmbeddedCassandraServerHelper.class.getResourceAsStream(resource);
            OutputStream out = new FileOutputStream(file)
        ) {
            byte buf[] = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
        }
    }

    /**
     * Creates a directory
     *
     * @param dir
     * @throws IOException
     */
    private static void mkdir(String dir) throws IOException {
        FileUtils.createDirectory(dir);
    }

    private static void cleanupAndLeaveDirs() throws IOException {
        mkdirs();
        cleanup();
        mkdirs();
        CommitLog.instance.resetUnsafe(); // cleanup screws w/ CommitLog, this
        // brings it back to safe state
    }

    private static void cleanup() throws IOException {
        // clean up commitlog
        String[] directoryNames = {DatabaseDescriptor.getCommitLogLocation(),};
        for (String dirName : directoryNames) {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }

        // clean up data directory which are stored as data directory/table/data
        // files
        for (String dirName : DatabaseDescriptor.getAllDataFileLocations()) {
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
            StringBuffer sb = new StringBuffer();
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
