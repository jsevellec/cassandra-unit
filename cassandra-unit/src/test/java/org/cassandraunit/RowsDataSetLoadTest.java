package org.cassandraunit;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.TupleValue;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.type.DataTypes;
import org.cassandraunit.dataset.CQLDataSetFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type conversions, end to end against a real schema. This is where a declarative dataset
 * either earns its keep or quietly writes the wrong thing.
 * <p>
 * The schema is a {@code .cql} script and the rows are YAML, loaded as one composite so the
 * keyspace is dropped and created once for the pair - the shape a user actually writes.
 *
 * @author Jeremy Sevellec
 */
class RowsDataSetLoadTest {

    @RegisterExtension
    static CassandraUnitExtension cassandra = new CassandraUnitExtension(
            CQLDataSetFactory.fromClassPathAll("rowskeyspace", "cql/rowsSchema.cql", "rows/widget.yaml"));

    private static Row widget(CqlSession session, String id) {
        return session.execute("SELECT * FROM rowskeyspace.widget WHERE id = " + id).one();
    }

    @Test
    void aTextColumnHoldingAQuotedNumberShouldStayText(CqlSession session) {
        // The headline bug of the old XML/JSON/YAML support: "1" in a text column rendered as
        // VALUES (1). The value has to come back as the string, not as a number.
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getString("label"))
                .isEqualTo("1");
    }

    @Test
    void aTextColumnHoldingAnUnquotedNumberShouldAlsoStayText(CqlSession session) {
        // `label: 1` unquoted: YAML hands over an Integer, for a text column. The fixture plainly
        // means the text "1", and the loader has to agree rather than fail on a technicality.
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570702").getString("label"))
                .isEqualTo("1");
    }

    @Test
    void anApostropheInATextValueShouldSurvive(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570703").getString("label"))
                .isEqualTo("it's fine");
    }

    @Test
    void aUuidShouldRoundTrip(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getUuid("id"))
                .isEqualTo(UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570701"));
    }

    @Test
    void aTimestampShouldBeReadFromAnIsoString(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getInstant("created"))
                .isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
    }

    @Test
    void aTimestampShouldAlsoBeReadFromEpochMillis(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570703").getInstant("created"))
                .isEqualTo(Instant.ofEpochMilli(1758276000000L));
    }

    @Test
    void anUnquotedYamlTimestampShouldBeReadToo(CqlSession session) {
        // snakeyaml resolves this one to a java.util.Date rather than a String, which no codec
        // accepts - the loader normalises it.
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570705").getInstant("created"))
                .isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
    }

    @Test
    void aBlobShouldBeReadFromItsHexLiteral(CqlSession session) {
        ByteBuffer payload = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getByteBuffer("payload");

        byte[] bytes = new byte[payload.remaining()];
        payload.duplicate().get(bytes);
        assertThat(bytes).containsExactly(0x0a, 0x0b, 0x0c);
    }

    @Test
    void aSetShouldBeReadFromAYamlList(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getSet("tags", String.class))
                .containsExactlyInAnyOrder("alpha", "beta");
    }

    @Test
    void aMapShouldBeReadWithBothItsKeyAndValueTypes(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701")
                .getMap("props", String.class, Integer.class))
                .isEqualTo(Map.of("a", 1, "b", 2));
    }

    @Test
    void anIntegerShouldWidenForABigintColumn(CqlSession session) {
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701").getLong("quantity"))
                .isEqualTo(42L);
    }

    @Test
    void aDoubleAndAnInetShouldRoundTrip(CqlSession session) {
        Row row = widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570701");

        assertThat(row.getDouble("ratio")).isEqualTo(1.5d);
        assertThat(row.getInetAddress("addr")).hasToString("/127.0.0.1");
    }

    @Test
    void anExplicitNullShouldBeWritten(CqlSession session) {
        // Present in the file with a null value: the column is in the INSERT, bound to null.
        assertThat(widget(session, "1690e8da-5bf8-49e8-9583-4dff8a570704").getString("label")).isNull();
    }

    @Test
    void aUdtShouldBeReadFromAMapKeyedByFieldName(CqlSession session) {
        UdtValue addr = session.execute(
                        "SELECT addr FROM rowskeyspace.place WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570d01")
                .one().getUdtValue("addr");

        // Each field converts against its own type: street is text, zip is int, and the YAML gave
        // a string and a number respectively.
        assertThat(addr.getString("street")).isEqualTo("1 Main St");
        assertThat(addr.getInt("zip")).isEqualTo(75001);
    }

    /**
     * A dataset built in code can hand over the value it already has, and the two structured types
     * are the ones where that is not free: a UDT would otherwise be rejected for not being a map,
     * and a tuple for not being a CQL literal. Tested here rather than in {@code BuiltDataSetTest}
     * because this is the schema with a UDT and a tuple in it.
     */
    @Test
    void aBuilderShouldPassRealUdtAndTupleValuesStraightThrough(CqlSession session) {
        UUID id = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570d99");
        UdtValue address = session.getMetadata().getKeyspace("rowskeyspace").orElseThrow()
                .getUserDefinedType("address").orElseThrow()
                .newValue().setString("street", "2 Other St").setInt("zip", 75002);
        TupleValue coordinate = DataTypes.tupleOf(DataTypes.INT, DataTypes.TEXT)
                .newValue().setInt(0, 2).setString(1, "south");

        new CQLDataLoader(session).load(CQLDataSetFactory.builder("rowskeyspace")
                .table("place").columns("id", "addr", "coord")
                    .row(id, address, coordinate)
                .build());

        Row place = session.execute("SELECT * FROM rowskeyspace.place WHERE id = " + id).one();
        assertThat(place.getUdtValue("addr").getString("street")).isEqualTo("2 Other St");
        assertThat(place.getUdtValue("addr").getInt("zip")).isEqualTo(75002);
        assertThat(place.getTupleValue("coord").getInt(0)).isEqualTo(2);
        assertThat(place.getTupleValue("coord").getString(1)).isEqualTo("south");
    }

    @Test
    void aTupleShouldBeReadFromItsCqlLiteral(CqlSession session) {
        // Tuples have no natural shape in YAML, so they fall through to the driver's own literal
        // parser - which is why the documented form is the CQL one.
        TupleValue coord = session.execute(
                        "SELECT coord FROM rowskeyspace.place WHERE id = 1690e8da-5bf8-49e8-9583-4dff8a570d01")
                .one().getTupleValue("coord");

        assertThat(coord.getInt(0)).isEqualTo(1);
        assertThat(coord.getString(1)).isEqualTo("north");
    }

    @Test
    void aColumnAbsentFromTheFixtureShouldNotOverwriteWhatIsAlreadyThere(CqlSession session) {
        // The schema script inserted this row with label='preexisting'; the YAML row for the same
        // primary key sets only tags. Absent is not the same as null: the column is left out of
        // the INSERT entirely, so nothing is written to it.
        Row row = widget(session, "00000000-0000-0000-0000-000000000009");

        assertThat(row.getString("label")).isEqualTo("preexisting");
        assertThat(row.getSet("tags", String.class)).isEqualTo(Set.of("kept"));
    }
}
