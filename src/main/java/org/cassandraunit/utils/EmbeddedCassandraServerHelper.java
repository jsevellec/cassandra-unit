package org.cassandraunit.utils;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.commons.lang.StringUtils;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraServerHelper {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCassandraServerHelper.class);

    public static final String DEFAULT_TMP_DIR = "target/embeddedCassandra";
    public static final String DEFAULT_CASSANDRA_YML_FILE = "cu-cassandra.yaml";
    public static final String DEFAULT_LOG4J_CONFIG_FILE = "/log4j-embedded-cassandra.properties";
    private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";

    private static CassandraDaemon cassandraDaemon = null;
    static ExecutorService executor;
    private static String launchedYamlFile;

    public static void startEmbeddedCassandra() throws TTransportException, IOException, InterruptedException,
            ConfigurationException {
        startEmbeddedCassandra(DEFAULT_CASSANDRA_YML_FILE);
    }

    public static void startEmbeddedCassandra(String yamlFile) throws TTransportException, IOException, ConfigurationException {
        startEmbeddedCassandra(yamlFile, DEFAULT_TMP_DIR);
    }

    public static void startEmbeddedCassandra(String yamlFile, String tmpDir) throws TTransportException, IOException, ConfigurationException {
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
        startEmbeddedCassandra(file, tmpDir);
    }

    /**
     * Set embedded cassandra up and spawn it in a new thread.
     *
     * @throws TTransportException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void startEmbeddedCassandra(File file, String tmpDir) throws TTransportException, IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        checkConfigNameForRestart(file.getAbsolutePath());

        log.debug("Starting cassandra...");
        log.debug("Initialization needed");

        System.setProperty("cassandra.config", "file:" + file.getAbsolutePath());
        System.setProperty("cassandra-foreground", "true");

        // If there is no log4j config set already, set the default config
        if (System.getProperty("log4j.configuration") == null) {
            copy(DEFAULT_LOG4J_CONFIG_FILE, tmpDir);
            System.setProperty("log4j.configuration", "file:" + tmpDir + DEFAULT_LOG4J_CONFIG_FILE);
        }

        cleanupAndLeaveDirs();
        final CountDownLatch startupLatch = new CountDownLatch(1);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                cassandraDaemon = new CassandraDaemon();
                cassandraDaemon.activate();
                startupLatch.countDown();
            }
        });
        try {
            startupLatch.await(10, SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted waiting for Cassandra daemon to start:", e);
            throw new AssertionError(e);
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

    private static void dropKeyspaces() {
        String host = DatabaseDescriptor.getRpcAddress().getHostName();
        int port = DatabaseDescriptor.getRpcPort();
        log.debug("Cleaning cassandra keyspaces on " + host + ":" + port);
        Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator(host + ":" + port));
        /* get all keyspace */
        List<KeyspaceDefinition> keyspaces = cluster.describeKeyspaces();

        /* drop all keyspace except internal cassandra keyspace */
        for (KeyspaceDefinition keyspaceDefinition : keyspaces) {
            String keyspaceName = keyspaceDefinition.getName();

            if (!INTERNAL_CASSANDRA_KEYSPACE.equals(keyspaceName)) {
                cluster.dropKeyspace(keyspaceName);
            }
        }
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
        InputStream is = EmbeddedCassandraServerHelper.class.getResourceAsStream(resource);
        String fileName = resource.substring(resource.lastIndexOf("/") + 1);
        File file = new File(directory + System.getProperty("file.separator") + fileName);
        OutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        is.close();
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
        try {
            DatabaseDescriptor.createAllDirectories();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
