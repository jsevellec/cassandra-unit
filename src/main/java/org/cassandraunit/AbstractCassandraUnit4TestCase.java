package org.cassandraunit;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import org.cassandraunit.dataset.DataSet;
import org.junit.Before;

/**
 * @author Jeremy Sevellec
 */
public abstract class AbstractCassandraUnit4TestCase {

    private CassandraUnit cassandraUnit;
    private Keyspace keyspace = null;
    private boolean initialized = false;
    private Cluster cluster;

    @Before
    public void before() throws Exception {
        if (!initialized) {
            cassandraUnit = new CassandraUnit(getDataSet());
            cassandraUnit.before();

            cluster = cassandraUnit.cluster;
            keyspace = cassandraUnit.keyspace;
            initialized = true;
        }

    }

    public abstract DataSet getDataSet();

    public void setKeyspace(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }

}
