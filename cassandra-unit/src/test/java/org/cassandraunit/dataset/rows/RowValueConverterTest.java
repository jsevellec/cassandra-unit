package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.cassandraunit.dataset.ParseException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The conversion ladder, exercised with no Cassandra running.
 * <p>
 * The load path takes its {@code DataType} from a prepared statement and a read-back path would
 * take it from a result set, but the conversion itself only ever needed the type and a
 * {@link CodecRegistry}. Testing it directly means the rungs are covered without paying three
 * seconds of node startup, and it pins the behaviour the parallel assertion work will depend on.
 * <p>
 * UDT conversion is not here: {@code UserDefinedType.newValue()} needs an attachment point, so it
 * genuinely requires a session. {@code RowsDataSetLoadTest} covers it against a real node.
 */
class RowValueConverterTest {

    private static final String ORIGIN = "classpath:rows/widget.yaml";

    private final RowValueConverter converter = new RowValueConverter(CodecRegistry.DEFAULT, ORIGIN);

    private Object convert(com.datastax.oss.driver.api.core.type.DataType type, Object value) {
        return converter.convert(type, value, "widget", "col");
    }

    @Test
    void nullShouldStayNull() {
        assertThat(convert(DataTypes.TEXT, null)).isNull();
    }

    // Rung 1 - the value already has the type the column wants.

    @Test
    void aValueTheCodecAlreadyAcceptsShouldPassStraightThrough() {
        assertThat(convert(DataTypes.TEXT, "hello")).isEqualTo("hello");
        assertThat(convert(DataTypes.BIGINT, 42L)).isEqualTo(42L);
        assertThat(convert(DataTypes.BOOLEAN, true)).isEqualTo(true);
    }

    // Rung 2 - a string, which is everything CSV and XML produce.

    @Test
    void aStringShouldBeParsedForAnUnquotedLiteralType() {
        UUID id = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570702");

        assertThat(convert(DataTypes.UUID, id.toString())).isEqualTo(id);
    }

    @Test
    void aStringShouldBeQuotedBeforeParsingForAQuotedLiteralType() {
        assertThat(convert(DataTypes.TIMESTAMP, "2026-09-19T10:00:00Z"))
                .isEqualTo(Instant.parse("2026-09-19T10:00:00Z"));
    }

    /**
     * The defining bug of the 4.x dataset formats: a text column holding "1" became VALUES (1).
     */
    @Test
    void aNumericStringInATextColumnShouldStayAString() {
        assertThat(convert(DataTypes.TEXT, "1")).isEqualTo("1");
    }

    @Test
    void aStringWithAQuoteShouldSurviveTheQuotingRoundTrip() {
        assertThat(convert(DataTypes.TEXT, "it's")).isEqualTo("it's");
    }

    // Rung 3 - a non-string landing on a textual column.

    @Test
    void anIntegerInATextColumnShouldBecomeItsDecimalForm() {
        assertThat(convert(DataTypes.TEXT, 1)).isEqualTo("1");
        assertThat(convert(DataTypes.TEXT, true)).isEqualTo("true");
    }

    /**
     * YAML {@code label: 1.10} parses to the double 1.1, so writing it to a text column would
     * quietly store something the fixture never said. There is no way to recover the intent.
     */
    @Test
    void aDecimalInATextColumnShouldBeRefusedRatherThanRounded() {
        assertThatThrownBy(() -> convert(DataTypes.TEXT, 1.10d))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("exact form")
                .hasMessageContaining("Quote it in the dataset file");

        assertThatThrownBy(() -> convert(DataTypes.TEXT, new BigDecimal("1.10")))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("exact form");
    }

    // Rung 4 - widening, and unquoted literal forms.

    @Test
    void anIntegerShouldWidenToABigintColumn() {
        assertThat(convert(DataTypes.BIGINT, 42)).isEqualTo(42L);
    }

    /**
     * snakeyaml resolves an unquoted ISO timestamp to a java.util.Date, which no codec accepts.
     */
    @Test
    void aUtilDateShouldBeNormalisedToAnInstant() {
        Instant when = Instant.parse("2026-09-19T10:00:00Z");

        assertThat(convert(DataTypes.TIMESTAMP, Date.from(when))).isEqualTo(when);
    }

    // Collections.

    @Test
    void collectionElementsShouldBeConvertedIndividually() {
        UUID a = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570701");
        UUID b = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570702");

        assertThat(convert(DataTypes.listOf(DataTypes.UUID), List.of(a.toString(), b.toString())))
                .isEqualTo(List.of(a, b));
        assertThat(convert(DataTypes.setOf(DataTypes.INT), Set.of("1")))
                .isEqualTo(Set.of(1));
    }

    @Test
    void mapKeysAndValuesShouldBothBeConverted() {
        assertThat(convert(DataTypes.mapOf(DataTypes.TEXT, DataTypes.INT), Map.of("a", "1")))
                .isEqualTo(Map.of("a", 1));
    }

    @Test
    void aListShouldKeepItsOrder() {
        assertThat(convert(DataTypes.listOf(DataTypes.INT), List.of("3", "1", "2")))
                .isEqualTo(List.of(3, 1, 2));
    }

    /**
     * A CSV field holding one element has no separator to split on, so it arrives as a bare value.
     */
    @Test
    void aSingleValueShouldBeTreatedAsACollectionOfOne() {
        assertThat(convert(DataTypes.setOf(DataTypes.TEXT), "alpha")).isEqualTo(Set.of("alpha"));
    }

    @Test
    void aNullElementInsideACollectionShouldStayNull() {
        Object converted = convert(DataTypes.listOf(DataTypes.TEXT),
                java.util.Arrays.asList("a", null));

        assertThat(converted).isEqualTo(java.util.Arrays.asList("a", null));
    }

    // Error reporting.

    @Test
    void aValueTheColumnCannotTakeShouldNameTheFileTheColumnAndTheType() {
        assertThatThrownBy(() -> convert(DataTypes.INT, "not a number"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining(ORIGIN)
                .hasMessageContaining("widget.col")
                .hasMessageContaining("int");
    }

    @Test
    void somethingThatIsNotAMapWhereAMapIsExpectedShouldSaySo() {
        assertThatThrownBy(() -> convert(DataTypes.mapOf(DataTypes.TEXT, DataTypes.TEXT), "nope"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("expected a map");
    }
}
