package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.rows.TableNames.QualifiedTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The precedence rules for working out which keyspace a table belongs to.
 * <p>
 * Worth testing directly rather than only through a load: both the load path and any read-back
 * path resolve names here, and if they ever disagreed a dataset would write to one table and read
 * from another.
 */
class TableNamesTest {

    private static final String ORIGIN = "classpath:rows/widget.yaml";

    @Test
    void aKeyspaceWrittenIntoTheTableNameShouldWin() {
        QualifiedTable resolved = TableNames.resolve("other.widget", "datasetks",
                CqlIdentifier.fromInternal("sessionks"), ORIGIN);

        assertThat(resolved).isEqualTo(new QualifiedTable("other", "widget"));
        assertThat(resolved.asCql()).isEqualTo("other.widget");
    }

    @Test
    void theDatasetKeyspaceShouldBeatTheSessionKeyspace() {
        QualifiedTable resolved = TableNames.resolve("widget", "datasetks",
                CqlIdentifier.fromInternal("sessionks"), ORIGIN);

        assertThat(resolved).isEqualTo(new QualifiedTable("datasetks", "widget"));
    }

    @Test
    void theSessionKeyspaceShouldBeTheLastResort() {
        QualifiedTable resolved = TableNames.resolve("widget", null,
                CqlIdentifier.fromInternal("sessionks"), ORIGIN);

        assertThat(resolved).isEqualTo(new QualifiedTable("sessionks", "widget"));
    }

    @Test
    void noKeyspaceAnywhereShouldSayWhatToDoAboutIt() {
        assertThatThrownBy(() -> TableNames.resolve("widget", null, null, ORIGIN))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining(ORIGIN)
                .hasMessageContaining("no keyspace")
                .hasMessageContaining("keyspace.table");
    }

    @Test
    void mixedCaseNamesShouldStayQuotedSoTheyStillResolve() {
        QualifiedTable resolved = TableNames.resolve("MyKeyspace.MyTable", null, null, ORIGIN);

        assertThat(resolved.asCql()).isEqualTo("\"MyKeyspace\".\"MyTable\"");
        assertThat(resolved.keyspaceId()).isEqualTo(CqlIdentifier.fromInternal("MyKeyspace"));
        assertThat(resolved.tableId()).isEqualTo(CqlIdentifier.fromInternal("MyTable"));
    }

    @Test
    void anOrdinaryLowercaseNameShouldNotBeQuoted() {
        assertThat(TableNames.identifier("widget")).isEqualTo("widget");
    }

    /**
     * A leading dot is not a keyspace separator - {@code indexOf('.') > 0}, not {@code >= 0} - so
     * the whole thing stays a (strange, but quoted) table name rather than resolving to an empty
     * keyspace.
     */
    @Test
    void aLeadingDotShouldNotBeReadAsAnEmptyKeyspace() {
        QualifiedTable resolved = TableNames.resolve(".widget", "datasetks", null, ORIGIN);

        assertThat(resolved).isEqualTo(new QualifiedTable("datasetks", ".widget"));
    }
}
