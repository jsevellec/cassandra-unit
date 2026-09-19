package org.cassandraunit.dataset;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.dataset.rows.CsvRowParser;
import org.cassandraunit.dataset.rows.JsonRowParser;
import org.cassandraunit.dataset.rows.RowDataSetParser;
import org.cassandraunit.dataset.rows.RowsCQLDataSet;
import org.cassandraunit.dataset.rows.XmlRowParser;
import org.cassandraunit.dataset.rows.YamlRowParser;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/**
 * Builds the right {@link CQLDataSet} for a dataset location, choosing the format from the file
 * extension.
 * <p>
 * Extension, not a {@code type} attribute. 5.0.0 deleted {@code DataSetFileExtensionEnum} and
 * {@code @CassandraDataSet(type = ...)} because they had gone stale - the enum still advertised
 * formats nothing could load. Re-adding a knob that has to agree with the filename would recreate
 * the same drift; the filename is the single source of truth.
 * <p>
 * Supported: {@code .cql} (statements, the original format), and {@code .yaml} / {@code .yml} /
 * {@code .json} / {@code .xml} / {@code .csv} (rows, loaded against a schema that already exists -
 * see {@link RowsCQLDataSet}).
 * <p>
 * {@link #builder(String)} is the sixth format and the one with no file at all: the same rows,
 * written in Java next to the test that needs them - see {@link CQLDataSetBuilder}.
 *
 * @author Jeremy Sevellec
 */
public final class CQLDataSetFactory {

    /**
     * Extensions tried, in order, when looking for a dataset by convention rather than by name.
     * {@code cql} first so existing projects keep resolving exactly as they did.
     */
    public static final List<String> SUPPORTED_EXTENSIONS = List.of("cql", "yaml", "yml", "json", "xml", "csv");

    private CQLDataSetFactory() {
    }

    public static CQLDataSet fromClassPath(String location) {
        return fromClassPath(location, true, true, null);
    }

    public static CQLDataSet fromClassPath(String location, String keyspaceName) {
        return fromClassPath(location, true, true, keyspaceName);
    }

    public static CQLDataSet fromClassPath(String location, boolean keyspaceCreation, boolean keyspaceDeletion) {
        return fromClassPath(location, keyspaceCreation, keyspaceDeletion, null);
    }

    public static CQLDataSet fromClassPath(String location, boolean keyspaceCreation,
                                           boolean keyspaceDeletion, String keyspaceName) {
        return build(location, new ClassPathDataSetSource(location),
                () -> new ClassPathCQLDataSet(location, keyspaceCreation, keyspaceDeletion, keyspaceName),
                keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    /**
     * Several classpath datasets loaded in order as one - the shape a row dataset needs, since it
     * describes data only and the schema has to be there first:
     * <pre>{@code
     * @RegisterExtension
     * static CassandraUnitExtension cassandra = new CassandraUnitExtension(
     *         CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml"));
     * }</pre>
     * The keyspace is dropped and created once, for the chain, not once per file. Mixing formats
     * is the point: the schema stays CQL and the rows do not have to be.
     */
    public static CQLDataSet fromClassPathAll(String keyspaceName, String... locations) {
        return composite(keyspaceName, true, locations);
    }

    /**
     * As {@link #fromClassPathAll}, but leaves an existing keyspace alone - for loading more rows
     * into a keyspace another dataset already built.
     */
    public static CQLDataSet fromClassPathAllKeepingKeyspace(String keyspaceName, String... locations) {
        return composite(keyspaceName, false, locations);
    }

    private static CQLDataSet composite(String keyspaceName, boolean recreateKeyspace, String... locations) {
        if (locations == null || locations.length == 0) {
            throw new ParseException("No dataset locations given");
        }
        // The members are built with the keyspace flags off: the composite owns the keyspace, and
        // a member that dropped it would destroy what an earlier member just loaded.
        List<CQLDataSet> dataSets = Arrays.stream(locations)
                .map(location -> fromClassPath(location, false, false, keyspaceName))
                .collect(Collectors.toList());
        return new CompositeCQLDataSet(dataSets, recreateKeyspace, recreateKeyspace, keyspaceName);
    }

    /**
     * Fixture rows written in Java rather than read from a file - the sixth format, and the only
     * one that lives in the test that needs it:
     * <pre>{@code
     * RowsCQLDataSet fixtures = CQLDataSetFactory.builder("mykeyspace")
     *         .table("widget").columns("id", "label", "quantity")
     *             .row(id1, "one", 42)
     *             .row(id2, "two", 7)
     *         .build();
     * }</pre>
     * Keyspace creation and deletion default to off, as they do for
     * {@link #rowsFromClassPath(String, String)} and for the same reason: a builder describes rows
     * and never schema, so dropping the keyspace would destroy the tables its own inserts need.
     * <p>
     * The result is a {@link RowsCQLDataSet}, so it can also state the expectation - see
     * {@link CQLDataSetBuilder}.
     */
    public static CQLDataSetBuilder builder(String keyspaceName) {
        return new CQLDataSetBuilder(keyspaceName, false, false);
    }

    /** As {@link #builder(String)}, against whatever keyspace the session is already using. */
    public static CQLDataSetBuilder builder() {
        return builder(null);
    }

    /**
     * As {@link #builder(String)}, with the keyspace flags spelled out - for the rare fixture that
     * really should own its keyspace. Anything the builder inserts needs tables that already
     * exist, so {@code keyspaceCreation} here creates an empty keyspace and nothing else.
     */
    public static CQLDataSetBuilder builder(String keyspaceName, boolean keyspaceCreation,
                                            boolean keyspaceDeletion) {
        return new CQLDataSetBuilder(keyspaceName, keyspaceCreation, keyspaceDeletion);
    }

    public static CQLDataSet fromFile(String location) {
        return fromFile(location, true, true, null);
    }

    public static CQLDataSet fromFile(String location, String keyspaceName) {
        return fromFile(location, true, true, keyspaceName);
    }

    public static CQLDataSet fromFile(String location, boolean keyspaceCreation, boolean keyspaceDeletion) {
        return fromFile(location, keyspaceCreation, keyspaceDeletion, null);
    }

    public static CQLDataSet fromFile(String location, boolean keyspaceCreation,
                                      boolean keyspaceDeletion, String keyspaceName) {
        return build(location, new FileDataSetSource(location),
                () -> new FileCQLDataSet(location, keyspaceCreation, keyspaceDeletion, keyspaceName),
                keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    /**
     * A row dataset as its concrete type, for callers needing {@link RowsCQLDataSet#parse()} rather
     * than just something loadable - the assertion side, which has to read the rows back out of the
     * file to compare them.
     * <p>
     * Keyspace creation and deletion are forced off. An expected dataset must not be able to drop
     * the keyspace it is about to inspect, and making that unrepresentable is better than
     * documenting it as a caution.
     *
     * @throws ParseException if the location is a {@code .cql} script, which describes statements
     *                        rather than rows
     */
    public static RowsCQLDataSet rowsFromClassPath(String location, String keyspaceName) {
        return rows(location, new ClassPathDataSetSource(location), keyspaceName);
    }

    /** As {@link #rowsFromClassPath}, reading from the filesystem. */
    public static RowsCQLDataSet rowsFromFile(String location, String keyspaceName) {
        return rows(location, new FileDataSetSource(location), keyspaceName);
    }

    private static RowsCQLDataSet rows(String location, DataSetSource source, String keyspaceName) {
        if (location == null) {
            throw new ParseException("Dataset location is null");
        }
        String extension = extensionOf(location);
        if ("cql".equals(extension)) {
            throw new ParseException(location + " is a CQL script, not a row dataset. Rows have to be"
                    + " described so they can be compared; a script is a list of statements. Use"
                    + " .yaml, .yml, .json, .xml or .csv.");
        }
        return new RowsCQLDataSet(source, parserFor(extension, location), tableNameOf(location),
                false, false, keyspaceName);
    }

    private static CQLDataSet build(String location, DataSetSource source,
                                    Supplier<CQLDataSet> cqlDataSet,
                                    boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName) {
        if (location == null) {
            throw new ParseException("Dataset location is null");
        }
        String extension = extensionOf(location);
        if ("cql".equals(extension)) {
            return cqlDataSet.get();
        }
        RowDataSetParser parser = parserFor(extension, location);
        return new RowsCQLDataSet(source, parser, tableNameOf(location),
                keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    private static RowDataSetParser parserFor(String extension, String location) {
        switch (extension) {
            case "yaml":
            case "yml":
                return new YamlRowParser();
            case "json":
                return new JsonRowParser();
            case "xml":
                return new XmlRowParser();
            case "csv":
                requireCsvSupport();
                return new CsvRowParser();
            default:
                throw new ParseException("Unsupported dataset extension '" + extension + "' for "
                        + location + ". Supported: " + String.join(", ", SUPPORTED_EXTENSIONS) + '.');
        }
    }

    /**
     * jackson-dataformat-csv is the one dependency this feature adds that is not already on the
     * classpath, and it is declared optional so that projects not using CSV do not pull it. Probed
     * here rather than at load time so a missing jar fails where a missing file does - when the
     * dataset is constructed.
     */
    private static void requireCsvSupport() {
        try {
            Class.forName("com.fasterxml.jackson.dataformat.csv.CsvMapper");
        } catch (ClassNotFoundException e) {
            throw new ParseException("CSV datasets need jackson-dataformat-csv on the test "
                    + "classpath. It is an optional dependency of cassandra-unit: add "
                    + "com.fasterxml.jackson.dataformat:jackson-dataformat-csv (no version needed "
                    + "if you import com.fasterxml.jackson:jackson-bom).", e);
        }
    }

    private static String extensionOf(String location) {
        String name = fileName(location);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new ParseException("Cannot tell the format of " + location + ": it has no file "
                    + "extension. Supported: " + String.join(", ", SUPPORTED_EXTENSIONS) + '.');
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * The filename stem, used as the table name by formats that cannot name their own table - CSV
     * always, and a YAML/JSON document whose root is a bare list of rows.
     */
    private static String tableNameOf(String location) {
        String name = fileName(location);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String fileName(String location) {
        int slash = Math.max(location.lastIndexOf('/'), location.lastIndexOf('\\'));
        return slash < 0 ? location : location.substring(slash + 1);
    }
}
