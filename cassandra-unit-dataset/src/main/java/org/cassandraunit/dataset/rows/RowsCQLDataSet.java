package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.dataset.DataSetSource;
import org.cassandraunit.dataset.ParseException;
import org.cassandraunit.dataset.SessionAwareDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A dataset of rows in a declarative format - YAML, JSON, XML or CSV - loaded against a schema that
 * already exists.
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
 *
 * @author Jeremy Sevellec
 */
public class RowsCQLDataSet implements SessionAwareDataSet {

    private final DataSetSource source;
    private final RowDataSetParser parser;
    private final String defaultTableName;
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
        this.source = source;
        this.parser = parser;
        this.defaultTableName = defaultTableName;
        this.keyspaceCreation = keyspaceCreation;
        this.keyspaceDeletion = keyspaceDeletion;
        // Lowercased for the same reason AbstractCQLDataSet does it: USE would otherwise break.
        this.keyspaceName = keyspaceName == null ? null : keyspaceName.toLowerCase();
    }

    @Override
    public void load(CqlSession session) {
        new RowBinder(session, keyspaceName, source.describe()).insertAll(parse());
    }

    /**
     * The parsed rows, before any type conversion. Useful in tests and for anyone wanting to see
     * what a dataset says without a running node.
     */
    public List<TableRows> parse() {
        try (InputStream in = source.open()) {
            if (in == null) {
                throw new ParseException("Dataset not found: " + source.describe());
            }
            return parser.parse(in, defaultTableName, source.describe());
        } catch (IOException e) {
            throw new ParseException(e);
        }
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
}
