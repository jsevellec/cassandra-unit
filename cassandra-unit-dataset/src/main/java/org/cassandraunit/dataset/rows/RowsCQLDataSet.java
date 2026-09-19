package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.DataSetSource;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.SessionAwareDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A dataset of rows - from a declarative file (YAML, JSON, XML or CSV) or built in code - loaded
 * against a schema that already exists.
 * <p>
 * It describes data only. The schema stays in a {@code .cql} script, which is both the simpler
 * split and the thing that makes the types right: by the time rows load, the column types are in
 * the database and can be read from there instead of being re-declared in the fixture.
 * <p>
 * That means two datasets, and the second must not drop what the first created:
 * <pre>{@code
 * CQLDataLoader loader = new CQLDataLoader(session);
 * loader.load(new ClassPathCQLDataSet("cql/schema.cql", true, true, "mykeyspace"));
 * loader.load(CQLDataSetFactory.fromClassPath("data/widget.yaml", false, false, "mykeyspace"));
 * }</pre>
 * <p>
 * Where the rows come from is the only thing that varies: a file and a parser, or
 * {@link #of(List, String, boolean, boolean, String)} with the rows already in hand - which is how
 * {@link org.cassandraunit.dataset.CQLDataSetBuilder} makes a dataset written in Java behave
 * exactly like one read from a file.
 *
 * @author Jeremy Sevellec
 */
public class RowsCQLDataSet implements SessionAwareDataSet {

    private final Supplier<List<TableRows>> rows;
    private final String describe;
    private final boolean keyspaceCreation;
    private final boolean keyspaceDeletion;
    private final String keyspaceName;

    public RowsCQLDataSet(DataSetSource source, RowDataSetParser parser, String defaultTableName,
                          boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName) {
        // Opened only to check the dataset exists, so a typo in the path fails here rather than
        // later at load time - the same contract AbstractCQLDataSet has always had.
        try (InputStream probe = source.open()) {
            if (probe == null) {
                throw new ParseException("Dataset not found: " + source.describe());
            }
        } catch (IOException e) {
            throw new ParseException(e);
        }
        // Re-read and re-parsed on every parse() call, which is what it has always done. Nothing
        // here memoizes: a file edited while the JVM is up is picked up by the next verification.
        this.rows = () -> read(source, parser, defaultTableName);
        this.describe = source.describe();
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
        // Lowercased for the same reason AbstractCQLDataSet does it: USE would otherwise break.
        this.keyspaceName = keyspaceName == null ? null : keyspaceName.toLowerCase();
    }

    private RowsCQLDataSet(Supplier<List<TableRows>> rows, String describe,
                           boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName) {
        this.rows = rows;
        this.describe = describe;
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
        this.keyspaceName = keyspaceName == null ? null : keyspaceName.toLowerCase();
    }

    /**
     * A dataset over rows that are already in hand rather than in a file - the seam
     * {@link org.cassandraunit.dataset.CQLDataSetBuilder} is built on, and usable directly by
     * anything else producing {@link TableRows} of its own.
     * <p>
     * The rows are copied, deeply enough that neither the caller nor a later load can change what
     * this dataset says. That matters more here than for a file: a parsed dataset is fresh on every
     * {@code parse()}, while this one hands out the same rows every time.
     *
     * @param tables rows per table, in the order they should be inserted
     * @param origin what to call this dataset in error and failure messages
     */
    public static RowsCQLDataSet of(List<TableRows> tables, String origin, boolean keyspaceCreation,
                                    boolean keyspaceDeletion, String keyspaceName) {
        if (tables == null) {
            throw new IllegalArgumentException("tables must not be null");
        }
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("origin must not be blank");
        }
        List<TableRows> copy = copyOf(tables);
        return new RowsCQLDataSet(() -> copy, origin, keyspaceCreation, keyspaceDeletion, keyspaceName);
    }

    @Override
    public void load(CqlSession session) {
        new RowBinder(session, keyspaceName, describe).insertAll(parse());
    }

    /**
     * The parsed rows, before any type conversion. Useful in tests and for anyone wanting to see
     * what a dataset says without a running node.
     */
    public List<TableRows> parse() {
        return rows.get();
    }

    /**
     * Where this dataset came from, for error and failure messages - e.g.
     * {@code classpath:rows/widget.yaml}.
     */
    public String describe() {
        return describe;
    }

    @Override
    public String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    public boolean isKeyspaceCreation() {
        return keyspaceCreation;
    }

    @Override
    public boolean isKeyspaceDeletion() {
        return keyspaceDeletion;
    }

    private static List<TableRows> read(DataSetSource source, RowDataSetParser parser, String defaultTableName) {
        try (InputStream in = source.open()) {
            if (in == null) {
                throw new ParseException("Dataset not found: " + source.describe());
            }
            return parser.parse(in, defaultTableName, source.describe());
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    private static List<TableRows> copyOf(List<TableRows> tables) {
        List<TableRows> copies = new ArrayList<>(tables.size());
        for (TableRows table : tables) {
            List<Map<String, Object>> rows = new ArrayList<>(table.rows().size());
            for (Map<String, Object> row : table.rows()) {
                // LinkedHashMap rather than Map.copyOf, for two reasons: column order is worth
                // keeping for readable failure messages, and Map.copyOf rejects a null value -
                // which here is not a mistake but an explicit null, the thing that writes a
                // tombstone.
                rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(row)));
            }
            copies.add(new TableRows(table.table(), Collections.unmodifiableList(rows)));
        }
        return Collections.unmodifiableList(copies);
    }
}
