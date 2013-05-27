package org.cassandraunit;

import com.datastax.driver.core.ResultSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 
 * @author Marcin Szymaniuk
 *
 */
public class CQLLoadTest extends AbstractCassandraUnit4CQL3DataSetTestCase {


    private final String KEYSPACE_NAME = "testkeyspace";

    @Override
	public String getCqlFile() {
		return "/cql/data.cql";
	}

    @Override
    public String getKeyspaceName() {
        return KEYSPACE_NAME;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Test
	public void testCQLDataAreInPlace() throws Exception {
        test();

	}

    @Test
    public void sameTestToMakeSureMultipleTestsAreFine() throws Exception {
        test();

    }

    private void test() {
        ResultSet result = session.execute("select * from testCQLTable WHERE id='1690e8da-5bf8-49e8-9583-4dff8a570737'");

        String val = result.iterator().next().getString("value");
        assertEquals("Cql loaded string",val);
    }


}
