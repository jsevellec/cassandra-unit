package org.cassandraunit.dataset.rows;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import org.cassandraunit.dataset.ParseException;

/**
 * Resolves the table name written in a dataset file to a keyspace and a table.
 * <p>
 * Shared rather than inlined because both directions have to agree: a dataset that loads into one
 * table must read back from the same one. A read-back path additionally needs the two halves
 * separately, to look the table up in {@code system_schema} or in the driver's schema metadata -
 * which is why this returns a {@link QualifiedTable} instead of a string.
 *
 * @author Jeremy Sevellec
 */
public final class TableNames {

    private TableNames() {
    }

    /**
     * A table name split into its two halves, each still in internal (unquoted, case-sensitive)
     * form - the form {@link CqlIdentifier#fromInternal} expects.
     */
    public record QualifiedTable(String keyspace, String table) {

        /** The {@code keyspace.table} form, each half quoted only where CQL requires it. */
        public String asCql() {
            return identifier(keyspace) + '.' + identifier(table);
        }

        public CqlIdentifier keyspaceId() {
            return CqlIdentifier.fromInternal(keyspace);
        }

        public CqlIdentifier tableId() {
            return CqlIdentifier.fromInternal(table);
        }
    }

    /**
     * Work out which keyspace a table belongs to.
     * <p>
     * An unqualified name resolves against whatever keyspace the session happens to have current,
     * and a row dataset normally loads with {@code keyspaceCreation=false} - which means
     * {@code CQLDataLoader} issues no USE before it, so the current keyspace is whatever the
     * previous load left behind. That is issue #160, and unlike a CQL script a row dataset cannot
     * work around it by writing {@code keyspace.table} in every statement itself. Resolving here
     * is the fix.
     * <p>
     * Precedence: a keyspace written into the table name wins, so one dataset can deliberately
     * span keyspaces; then the dataset's own keyspace; then the session's current one.
     *
     * @param datasetKeyspace the dataset's keyspace, or {@code null} if it names none
     * @param sessionKeyspace the session's current keyspace, or {@code null} if it has none
     * @throws ParseException if none of the three yields a keyspace
     */
    public static QualifiedTable resolve(String rawTable, String datasetKeyspace,
                                         CqlIdentifier sessionKeyspace, String origin) {
        int dot = rawTable.indexOf('.');
        if (dot > 0) {
            return new QualifiedTable(rawTable.substring(0, dot), rawTable.substring(dot + 1));
        }
        if (datasetKeyspace != null) {
            return new QualifiedTable(datasetKeyspace, rawTable);
        }
        if (sessionKeyspace == null) {
            throw new ParseException(origin + ": no keyspace. The dataset names none and the session "
                    + "has none selected. Give the dataset a keyspace name, or write the table as "
                    + "keyspace.table.");
        }
        return new QualifiedTable(sessionKeyspace.asInternal(), rawTable);
    }

    /**
     * Render a table or column name, quoting only when CQL requires it - so an ordinary lowercase
     * name stays unquoted and a mixed-case one still resolves.
     */
    public static String identifier(String name) {
        return CqlIdentifier.fromInternal(name).asCql(true);
    }
}
