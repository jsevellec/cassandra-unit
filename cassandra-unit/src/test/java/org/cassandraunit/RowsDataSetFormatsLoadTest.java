package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The same rows in JSON, XML and CSV, against the same schema, to show the formats agree.
 * <p>
 * Loaded the two-dataset way - schema through the extension, rows through the loader - which is
 * the other supported shape and the one {@code docs/datasets.md} leads with. Note the rows are
 * loaded with {@code false, false}: a second dataset that dropped the keyspace would destroy the
 * schema the first one just created.
 *
 * @author Jeremy Sevellec
 */
class RowsDataSetFormatsLoadTest {

    private static final String KEYSPACE = "formatskeyspace";

    @RegisterExtension
    static CassandraUnitExtension cassandra = new CassandraUnitExtension(
            new ClassPathCQLDataSet("cql/rowsSchema.cql", KEYSPACE));

    private static void loadRows(String location) {
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession())
                .load(CQLDataSetFactory.fromClassPath(location, false, false, KEYSPACE));
    }

    private static Row widget(CqlSession session, String id) {
        return session.execute("SELECT * FROM " + KEYSPACE + ".widget WHERE id = " + id).one();
    }

    @Test
    void jsonShouldLoadNativeTypesAndAnExplicitNull(CqlSession session) {
        loadRows("rows/widget.json");

        Row row = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570801");
        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(row.getInstant("created")).isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
        assertThat(row.getLong("quantity")).isEqualTo(42L);
        assertThat(row.getMap("props", String.class, Integer.class)).isEqualTo(Map.of("a", 1, "b", 2));

        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570802").getString("label")).isNull();
    }

    @Test
    void xmlShouldLoadValuesThatAreAllStringsInTheFile(CqlSession session) {
        loadRows("rows/widget.xml");

        Row row = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570901");
        // Everything in the XML was text, including "1" and "42"; the column types decided what
        // each one became.
        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(row.getInstant("created")).isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
        assertThat(row.getLong("quantity")).isEqualTo(42L);
        assertThat(row.getMap("props", String.class, Integer.class)).isEqualTo(Map.of("a", 1, "b", 2));

        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570902").getString("label")).isNull();
    }

    @Test
    void csvShouldTakeItsTableFromTheFileNameAndSplitCollections(CqlSession session) {
        loadRows("rows/widget.csv");

        Row row = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570a01");
        assertThat(row.getString("label")).isEqualTo("1");
        assertThat(row.getSet("tags", String.class)).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(row.getLong("quantity")).isEqualTo(42L);
    }

    @Test
    void aCsvCollectionWithOneElementShouldStillBeACollection(CqlSession session) {
        // There is no separator in the field to split on, so the parser reports a bare string and
        // the binder has to read it as a collection of one.
        loadRows("rows/widget.csv");

        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570a02").getSet("tags", String.class))
                .containsExactly("gamma");
    }

    @Test
    void anEmptyCsvFieldShouldLeaveTheColumnUnset(CqlSession session) {
        loadRows("rows/widget.csv");

        // quantity is empty in that row. Unset, so the column is simply never written.
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570a02").isNull("quantity")).isTrue();
    }

    @Test
    void aDecimalWrittenToATextColumnShouldBeRefusedRatherThanRounded(CqlSession session) {
        // 1.10 unquoted is the double 1.1, and storing "1.1" would silently contradict the file.
        assertThatThrownBy(() -> loadRows("rows/badDecimalLabel.yaml"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("widget.label")
                .hasMessageContaining("Quote it");
    }
}
