package org.cassandraunit.utils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CqlOperations {

    private static final Logger log = LoggerFactory.getLogger(CqlOperations.class);

    public static Consumer<String> execute(CqlSession session) {
        return query -> {
            log.debug("executing : {}", query);
            session.execute(query);
        };
    }

    public static Consumer<String> use(CqlSession session) {
        return keyspace -> session.execute("USE " + keyspace);
    }

    public static Consumer<String> truncateTable(CqlSession session) {
        return fullyQualifiedTable -> session.execute("truncate table " + fullyQualifiedTable);
    }

    public static Consumer<String> dropKeyspace(CqlSession session) {
        return keyspace -> execute(session).accept("DROP KEYSPACE IF EXISTS " + keyspace);
    }

    public static Consumer<String> createKeyspace(CqlSession session) {
        return keyspace -> execute(session).accept("CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1} AND durable_writes = false");
    }

    /**
     * Empties every table in one keyspace, keeping the schema, optionally leaving some alone.
     * <p>
     * Faster than dropping and recreating the keyspace when the schema is expensive to build, and
     * the only option when something else holds a reference to the keyspace. It needs no embedded
     * server: the table list comes from {@code system_schema.tables}, so this works against any
     * session - a Testcontainers node, a local one, anything speaking CQL.
     * <p>
     * The table list is read from {@code system_schema} rather than {@code session.getMetadata()}
     * on purpose. The driver's default {@code refreshed-keyspaces} filter decides what metadata
     * reports, so going through it would make the result depend on driver configuration rather
     * than on the server.
     * <p>
     * Note that {@code TRUNCATE} takes a snapshot first unless the server sets
     * {@code auto_snapshot: false}. The yaml files shipped here do; a stock Cassandra image, such
     * as the one Testcontainers pulls, does not.
     *
     * @param excludedTables table names to leave untouched
     */
    public static void truncateKeyspace(CqlSession session, String keyspace, String... excludedTables) {
        Set<String> excluded = new HashSet<>(Arrays.asList(excludedTables));

        session.execute(SimpleStatement.newInstance(
                        "SELECT table_name FROM system_schema.tables WHERE keyspace_name = ?", keyspace))
                .all().stream()
                .map(row -> row.getString("table_name"))
                .filter(tableName -> !excluded.contains(tableName))
                .map(tableName -> quote(keyspace) + "." + quote(tableName))
                .forEach(truncateTable(session));
    }

    /**
     * Quotes a CQL identifier so that names which are not lower-case, or which collide with a
     * reserved word, survive being concatenated into a statement. These used to be interpolated
     * raw, so a keyspace with an upper-case letter could not be dropped (#222).
     */
    public static String quote(String identifier) {
        return CqlIdentifier.fromInternal(identifier).asCql(true);
    }
}
