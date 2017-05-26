package org.cassandraunit.utils;

import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class CqlOperations {

    private static final Logger log = LoggerFactory.getLogger(CqlOperations.class);

    public static Consumer<String> execute(Session session) {
        return query -> {
            log.debug("executing : {}", query);
            session.execute(query);
        };
    }

    public static Consumer<String> use(Session session) {
        return keyspace -> session.execute("USE " + keyspace);
    }

    public static Consumer<String> truncateTable(Session session) {
        return fullyQualifiedTable -> session.execute("truncate table " + fullyQualifiedTable);
    }

    public static Consumer<String> dropKeyspace(Session session) {
        return keyspace -> execute(session).accept("DROP KEYSPACE IF EXISTS " + keyspace);
    }

    public static Consumer<String> createKeyspace(Session session) {
        return keyspace -> execute(session).accept("CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1} AND durable_writes = false");
    }
}
