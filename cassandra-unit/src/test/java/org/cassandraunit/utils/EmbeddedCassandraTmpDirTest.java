package org.cassandraunit.utils;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code tmpDir} argument actually relocates Cassandra's storage.
 * <p>
 * It previously did not. The yaml was adapted by a regex that only matched
 * {@code ^([a-z_]+)_port:} lines, so every directory kept the value hardcoded in the shipped
 * yaml ({@code target/embeddedCassandra/*}) and {@code tmpDir} moved nothing but the copy of
 * the yaml file. Issues #265 and #316 are both this bug, and nothing caught it because no test
 * ever asserted where the data went.
 */
public class EmbeddedCassandraTmpDirTest {

    private static Path tmpDir;

    @BeforeClass
    public static void startCassandraInItsOwnDirectory() throws Exception {
        tmpDir = Files.createTempDirectory("cu-tmpdir-test");
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(
                EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE, tmpDir.toString(), 60_000L);
    }

    @Test
    public void shouldPlaceEveryStorageDirectoryUnderTheGivenTmpDir() {
        String root = tmpDir.toAbsolutePath().toString();

        assertThat(DatabaseDescriptor.getAllDataFileLocations()).hasSize(1);
        assertThat(DatabaseDescriptor.getAllDataFileLocations()[0]).startsWith(root);
        assertThat(DatabaseDescriptor.getCommitLogLocation()).startsWith(root);
        assertThat(DatabaseDescriptor.getSavedCachesLocation()).startsWith(root);
        assertThat(DatabaseDescriptor.getHintsDirectory().toString()).startsWith(root);
    }

    @Test
    public void shouldActuallyWriteDataThere() throws Exception {
        EmbeddedCassandraServerHelper.getSession().execute(
                "CREATE KEYSPACE IF NOT EXISTS tmpdir_probe WITH replication = "
                        + "{'class':'SimpleStrategy','replication_factor':1}");
        EmbeddedCassandraServerHelper.getSession()
                .execute("CREATE TABLE tmpdir_probe.t (id int PRIMARY KEY)");

        // Cassandra creates a <keyspace>-<tableid> directory under the data location when a
        // table is created, so its presence proves the relocated path is the one in use.
        Path data = Path.of(DatabaseDescriptor.getAllDataFileLocations()[0]);
        assertThat(data).isDirectoryContaining(p -> p.getFileName().toString().startsWith("tmpdir_probe"));
    }
}
