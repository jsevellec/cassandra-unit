package org.cassandraunit;

import com.datastax.driver.core.Session;
import org.junit.After;
import org.junit.Before;

/**
 * @author Marcin Szymaniuk
 */
public abstract class AbstractCassandraUnit4CQL3DataSetTestCase {

    private CassandraCQLUnit cassandraUnit;
    private boolean initialized = false;
    protected Session session;

    public AbstractCassandraUnit4CQL3DataSetTestCase() {
    }

	@Before
    public void before() throws Exception {
        if (!initialized) {
            cassandraUnit = new CassandraCQLUnit(getCqlFile(),getKeyspaceName());
            cassandraUnit.before();

            session = cassandraUnit.createSession();
            session.execute("USE "+getKeyspaceName());

            initialized = true;
        }
    }
    @After
    public void after(){
        if(session!=null){
            session.shutdown();
        }
    }

    public abstract String getCqlFile();
    public abstract String getKeyspaceName();




}
