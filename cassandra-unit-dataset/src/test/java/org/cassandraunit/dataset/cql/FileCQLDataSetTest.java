package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Jeremy Sevellec
 */
class FileCQLDataSetTest {

    @TempDir
    Path tmp;

    /**
     * Writes its own file rather than copying one off the classpath. What is being tested is that
     * FileCQLDataSet reads a path from the filesystem, so any real file will do - and depending on
     * a shared fixture would mean either duplicating it into this module or reaching across to
     * another one.
     */
    private String aCqlFileOnDisk() throws IOException {
        Path file = tmp.resolve("simple.cql");
        Files.writeString(file, """
                CREATE TABLE IF NOT EXISTS testCQLTable (id uuid, value varchar, PRIMARY KEY(id));
                INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570737,'Cql loaded string');
                """);
        return file.toString();
    }

    @Test
    void shouldGetACQLDataSet() throws IOException {
        CQLDataSet dataSet = new FileCQLDataSet(aCqlFileOnDisk());

        assertThat(dataSet).isNotNull();
        assertThat(dataSet.getCQLStatements()).hasSize(2);
    }

    @Test
    void shouldNotGetACQLDataSetBecauseNull() {
        assertThatThrownBy(() -> new FileCQLDataSet(null))
                .isInstanceOf(ParseException.class);
    }

    @Test
    void shouldNotGetACQLDataSetBecauseOfFileNotFound() {
        assertThatThrownBy(() -> new FileCQLDataSet("/notfound.cql"))
                .isInstanceOf(ParseException.class);
    }
}
