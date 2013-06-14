package org.cassandraunit;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.rules.ExternalResource;

/**
 * @author Marcin Szymaniuk
 */
public abstract class BaseCassandraUnit extends ExternalResource {
    protected String configurationFileName;

    @Override
    protected void before() throws Exception {
        /* start an embedded Cassandra */
        if (configurationFileName != null) {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra(configurationFileName);
        } else {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        }

        /* create structure and load data */
        load();
    }

    protected abstract void load();
}
