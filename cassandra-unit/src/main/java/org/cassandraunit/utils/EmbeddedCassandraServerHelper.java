package org.cassandraunit.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.YamlConfigurationLoader;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Jeremy Sevellec
 */
public class EmbeddedCassandraServerHelper {

    private static Logger log = LoggerFactory.getLogger(EmbeddedCassandraServerHelper.class);

    public static final long DEFAULT_STARTUP_TIMEOUT = 20000;
    /**
     * Where the embedded server keeps its data when no tmpDir is given.
     * <p>
     * Resolved against {@code java.io.tmpdir} rather than the previous
     * {@code "target/embeddedCassandra"}, which hardcoded Maven's layout into the library and
     * was simply wrong anywhere else - Gradle uses {@code build/}, and a CWD-relative path is
     * wrong whenever tests are not run from the module directory. The directory is deleted at
     * the start of every run, so it does not accumulate.
     */
    public static final String DEFAULT_TMP_DIR =
            Paths.get(System.getProperty("java.io.tmpdir"), "cassandra-unit").toString();
    /** Default configuration file. Starts embedded cassandra under the well known ports */
    public static final String DEFAULT_CASSANDRA_YML_FILE = "cu-cassandra.yaml";
    /** Configuration file which starts the embedded cassandra on a random free port */
    public static final String CASSANDRA_RNDPORT_YML_FILE = "cu-cassandra-rndport.yaml";
    private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";
    private static final String INTERNAL_CASSANDRA_AUTH_KEYSPACE = "system_auth";
    private static final String INTERNAL_CASSANDRA_DISTRIBUTED_KEYSPACE = "system_distributed";
    private static final String INTERNAL_CASSANDRA_SCHEMA_KEYSPACE = "system_schema";
    private static final String INTERNAL_CASSANDRA_TRACES_KEYSPACE = "system_traces";
    /** Virtual keyspaces, added in Cassandra 4.0. Not user-modifiable: dropping one fails. */
    private static final String INTERNAL_CASSANDRA_VIEWS_KEYSPACE = "system_views";
    private static final String INTERNAL_CASSANDRA_VIRTUAL_SCHEMA_KEYSPACE = "system_virtual_schema";

    private static final Set<String> systemKeyspaces = new HashSet<>(Arrays.asList(INTERNAL_CASSANDRA_KEYSPACE,
            INTERNAL_CASSANDRA_AUTH_KEYSPACE, INTERNAL_CASSANDRA_DISTRIBUTED_KEYSPACE,
            INTERNAL_CASSANDRA_SCHEMA_KEYSPACE, INTERNAL_CASSANDRA_TRACES_KEYSPACE,
            INTERNAL_CASSANDRA_VIEWS_KEYSPACE, INTERNAL_CASSANDRA_VIRTUAL_SCHEMA_KEYSPACE));

    public static Predicate<String> nonSystemKeyspaces() {
        return keyspace -> !systemKeyspaces.contains(keyspace);
    }

    /*
     * One embedded Cassandra per JVM is a permanent design invariant, not something waiting to
     * be fixed: DatabaseDescriptor, Schema and StorageService hold static state that cannot be
     * reset in-process. These fields are therefore JVM-global by design. They are volatile
     * because the daemon is activated on a separate thread, and the "already started" checks
     * read them from the caller's thread.
     */
    private static volatile CassandraDaemon cassandraDaemon = null;
    private static volatile String launchedYamlFile;
    private static volatile CqlSession session;
    /** Zero means no timeout, which is the driver's own encoding. */
    private static volatile Duration requestTimeout = Duration.ZERO;

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

    public static synchronized void startEmbeddedCassandra(String yamlFile, String tmpDir, long timeout) throws IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        if (!StringUtils.startsWith(yamlFile, "/")) {
            yamlFile = "/" + yamlFile;
        }

        rmdir(tmpDir);
        File file = copy(yamlFile, tmpDir).toFile();
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
    public static synchronized void startEmbeddedCassandra(File file, String tmpDir, long timeout) throws IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        checkConfigNameForRestart(file.getAbsolutePath());

        log.debug("Starting cassandra...");
        log.debug("Initialization needed");

        System.setProperty("cassandra.config", file.toPath().toUri().toString());
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.unsafesystem", "true"); // disable fsync for a massive speedup on old platters

        // Load the yaml, then relocate the storage directories and resolve any request for a
        // random port, by mutating the Config object rather than by rewriting the file as text.
        DatabaseDescriptor.daemonInitialization(() -> {
            Config config = new YamlConfigurationLoader().loadConfig();
            relocateStorageDirectories(config, tmpDir);
            assignFreePorts(config);
            return config;
        });

        cleanupAndLeaveDirs();
        final CountDownLatch startupLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // runManaged=true is essential. CassandraDaemon.deactivate() ends with
            // `if (!runManaged) System.exit(0)`, and activate() likewise exits the JVM on a
            // startup error. With the default constructor, calling stopEmbeddedCassandra()
            // terminated the whole test JVM - surefire just reported the fork vanishing - and a
            // configuration error killed the build instead of throwing something diagnosable.
            cassandraDaemon = new CassandraDaemon(true);
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
     * Deactivates the embedded daemon, stopping the native transport.
     * <p>
     * Read this before calling it. It does <strong>not</strong> return the JVM to a state where
     * another embedded Cassandra can be started: Cassandra's {@code DatabaseDescriptor},
     * {@code Schema} and {@code StorageService} keep static state that cannot be reset
     * in-process, so there is no "stop and start again with a different configuration". If that
     * is what you are after, give each configuration its own JVM - surefire's
     * {@code reuseForks=false} does it.
     * <p>
     * Calling this is almost never necessary: the daemon dies with the JVM, and every test class
     * in a fresh fork gets a fresh daemon anyway.
     * <p>
     * The previous javadoc claimed this was "an empty method". It was not. The body dereferenced
     * the daemon, so it threw NullPointerException when nothing had been started, and when
     * something had been started it terminated the JVM outright, because the daemon was created
     * unmanaged and {@code CassandraDaemon.deactivate()} ends in {@code System.exit(0)}. The
     * daemon is now created with {@code runManaged=true}, so this returns normally.
     */
    public static synchronized void stopEmbeddedCassandra() {
        CassandraDaemon daemon = cassandraDaemon;
        if (daemon == null) {
            log.warn("stopEmbeddedCassandra() called but no embedded Cassandra was started; ignoring.");
            return;
        }
        daemon.deactivate();
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
            CqlOperations.truncateKeyspace(session, keyspace, excludedTables);
        }
    }

    /**
     * Sets the request timeout applied to the shared session. Has to be called before the
     * session is first built - there is only one session per JVM, so it cannot be changed
     * afterwards, and a late call is ignored with a warning rather than silently doing nothing.
     */
    public static synchronized void setRequestTimeout(Duration timeout) {
        if (session != null) {
            log.warn("The shared CqlSession already exists, so a request timeout of {} cannot be "
                    + "applied. Set it before the first getSession() call.", timeout);
            return;
        }
        requestTimeout = timeout;
    }

    public static CqlSession getSession() {
        initSession();
        return session;
    }

    private static synchronized void initSession() {
        if (session == null) {
            DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, requestTimeout)
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

    /*
     * Schema discovery below queries system_schema directly rather than going through
     * session.getMetadata().
     *
     * Not because the metadata is wrong, but because it answers a different question. The
     * driver's default reference.conf sets
     *
     *   refreshed-keyspaces = [ "!system", "!/^system_.*!/", "!/^dse_.*!/", ... ]
     *
     * so getKeyspaces() deliberately never reports a system keyspace. That made the
     * nonSystemKeyspaces() filter redundant, and it meant the set of keyspaces we iterate
     * depended on driver configuration rather than on the server. Reading system_schema makes
     * the source of truth the server, and makes the allowlist actually load-bearing.
     */

    private static void dropKeyspaces() {
        session.execute("SELECT keyspace_name FROM system_schema.keyspaces")
                .all().stream()
                .map(row -> row.getString("keyspace_name"))
                .filter(nonSystemKeyspaces())
                .map(CqlOperations::quote)
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
            // Note: FSWriteError's File overload takes org.apache.cassandra.io.util.File as of
            // Cassandra 4.1, not java.io.File. Pass the Path to stay off that moving target.
            throw new FSWriteError(t, dir.toPath());
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
            // A directory that is not there is already in the state this method wants it in.
            // Throwing here meant a fresh tmpDir could fail the startup it was meant to enable
            // (#316); deleteRecursive is a no-op for a path that does not exist.
            rmdir(dirName);
        }
    }

    public static void mkdirs() {
        DatabaseDescriptor.createAllDirectories();
    }

    /**
     * Points every storage directory at {@code tmpDir}.
     * <p>
     * This is what makes the {@code tmpDir} argument mean something. Previously the yaml was
     * rewritten as text with a regex that only matched lines of the form
     * {@code ^([a-z_]+)_port:}, so the data, commitlog, saved_caches, hints and cdc paths kept
     * whatever the shipped yaml said - {@code target/embeddedCassandra/*} - and passing a
     * tmpDir relocated nothing but the copy of the yaml itself (issues #265, #316).
     */
    private static void relocateStorageDirectories(Config config, String tmpDir) {
        Path root = Paths.get(tmpDir).toAbsolutePath();
        config.data_file_directories = new String[]{ root.resolve("data").toString() };
        config.commitlog_directory = root.resolve("commitlog").toString();
        config.saved_caches_directory = root.resolve("saved_caches").toString();
        config.hints_directory = root.resolve("hints").toString();
        config.cdc_raw_directory = root.resolve("cdc_raw").toString();
    }

    /**
     * Replaces any port set to 0 with a free one, which is how cu-cassandra-rndport.yaml asks
     * for a random port. All four are handled - ssl_storage_port used to be left at its fixed
     * value, so the "random port" configuration could still collide.
     * <p>
     * Note this is inherently racy: the port is probed by opening and closing a socket, and is
     * only bound later when the daemon starts. Nothing can fully close that window short of
     * Cassandra accepting a pre-bound socket.
     */
    private static void assignFreePorts(Config config) {
        try {
            if (config.storage_port == 0) {
                config.storage_port = findUnusedLocalPort();
            }
            if (config.ssl_storage_port == 0) {
                config.ssl_storage_port = findUnusedLocalPort();
            }
            if (config.native_transport_port == 0) {
                config.native_transport_port = findUnusedLocalPort();
            }
        } catch (IOException e) {
            throw new ConfigurationException("Could not find a free port for the embedded Cassandra", e);
        }
    }

    private static int findUnusedLocalPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
}
