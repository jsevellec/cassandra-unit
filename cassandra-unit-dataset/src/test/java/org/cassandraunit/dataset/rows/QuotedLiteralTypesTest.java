package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks {@link RowValueConverter}'s idea of which types have a quoted CQL literal against the driver's own.
 * <p>
 * {@code RowValueConverter} has to quote a raw string from CSV or XML before {@link TypeCodec#parse} will
 * take it - but only for the types whose literal form is quoted, and it carries a hardcoded set.
 * A hardcoded set written from memory is a guess. {@link TypeCodec#format} is the exact inverse of
 * {@code parse}, so asking the driver to format a sample value says authoritatively whether that
 * type's literal is quoted. This test fails loudly if a driver upgrade ever moves a type from one
 * group to the other, which is a failure that would otherwise show up as one puzzling type not
 * loading.
 *
 * @author Jeremy Sevellec
 */
class QuotedLiteralTypesTest {

    private static final CodecRegistry REGISTRY = CodecRegistry.DEFAULT;

    static List<Object[]> samples() throws UnknownHostException {
        return List.of(
                new Object[]{DataTypes.TEXT, "hello"},
                new Object[]{DataTypes.ASCII, "hello"},
                new Object[]{DataTypes.TIMESTAMP, Instant.ofEpochMilli(1758276000000L)},
                new Object[]{DataTypes.DATE, LocalDate.of(2026, 9, 19)},
                new Object[]{DataTypes.TIME, LocalTime.of(10, 0)},
                new Object[]{DataTypes.INET, InetAddress.getByName("127.0.0.1")},
                new Object[]{DataTypes.INT, 1},
                new Object[]{DataTypes.BIGINT, 1L},
                new Object[]{DataTypes.DOUBLE, 1.5d},
                new Object[]{DataTypes.FLOAT, 1.5f},
                new Object[]{DataTypes.DECIMAL, new BigDecimal("1.5")},
                new Object[]{DataTypes.VARINT, BigInteger.ONE},
                new Object[]{DataTypes.BOOLEAN, Boolean.TRUE},
                new Object[]{DataTypes.UUID, UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570737")},
                new Object[]{DataTypes.BLOB, ByteBuffer.wrap(new byte[]{1, 2, 3})});
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    void theQuotedSetShouldMatchWhatTheDriverActuallyFormats(DataType type, Object sample) {
        TypeCodec<Object> codec = REGISTRY.codecFor(type);
        boolean driverQuotesIt = codec.format(sample).startsWith("'");

        assertThat(RowValueConverter.needsQuoting(type))
                .as("%s formats as %s", type.asCql(true, true), codec.format(sample))
                .isEqualTo(driverQuotesIt);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    void quotingTheWayTheConverterDoesShouldSurviveARoundTrip(DataType type, Object sample) {
        TypeCodec<Object> codec = REGISTRY.codecFor(type);
        // What a CSV field or an XML element looks like: the value with no CQL syntax around it.
        String raw = unquoted(codec.format(sample));

        Object parsed = codec.parse(RowValueConverter.needsQuoting(type) ? RowValueConverter.quote(raw) : raw);

        assertThat(parsed).isEqualTo(sample);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("samples")
    void everySampleShouldHaveAQuotingAnswerThatIsNotAnException(DataType type, Object sample) {
        // Guards against a type being silently absent from the sample list: if a type reached here
        // without a codec, codecFor would throw rather than this assertion failing.
        assertThat(REGISTRY.codecFor(type)).isNotNull();
        assertThat(sample).isNotNull();
    }

    private static String unquoted(String literal) {
        return literal.startsWith("'") && literal.endsWith("'")
                ? literal.substring(1, literal.length() - 1).replace("''", "'")
                : literal;
    }
}
