package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Jeremy Sevellec
 */
public class ClasspathCQLDataSetTest {

    @Test
    public void shouldGetACQLDataSet() {

        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql");
        assertThat(dataSet, notNullValue());
    }

    @Test
    public void shouldNotGetACQLDataSetBecauseNull() {
        try {
            CQLDataSet dataSet = new ClassPathCQLDataSet(null);
            fail();
        } catch (ParseException e) {
            /* nothing to do, it what we want */
        }
    }

    @Test
    public void shouldNotGetACQLDataSetBecauseItNotExist() {
        try {
            CQLDataSet dataSet = new ClassPathCQLDataSet("cql/unknownDataSet.cql");
            fail();
        } catch (ParseException e) {
            /* nothing to do, it what we want */
        }
    }

    @Test
    public void shouldGetCQLQueries() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql");
        assertThat(dataSet.getCQLStatements(), notNullValue());
        assertThat(dataSet.getCQLStatements().isEmpty(), is(false));
        assertThat(dataSet.getCQLStatements().size(),is(4));
        assertThat(dataSet.getCQLStatements().get(0), is("CREATE TABLE testCQLTable IF NOT EXISTS (id uuid, value varchar, PRIMARY KEY(id));"));
        assertThat(dataSet.getCQLStatements().get(1),is("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570737,'Cql loaded string');"));
        assertThat(dataSet.getCQLStatements().get(2),is("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570738,'BLA2');"));
        assertThat(dataSet.getCQLStatements().get(3),is("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570739,'BLA1');"));
    }


    @Test
    public void shouldGetDefinedTestKeyspaceName() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace");
        assertThat(dataSet.getKeyspaceName(),is("mykeyspace"));
    }

    @Test
    public void shouldGetCQLQueriesFromMultiLineCQLScript() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/multiLineStatements.cql");
        assertThat(dataSet.getCQLStatements(), notNullValue());
        assertThat(dataSet.getCQLStatements().isEmpty(), is(false));
        assertThat(dataSet.getCQLStatements().size(),is(4));
        assertThat(dataSet.getCQLStatements().get(0),is("CREATE TABLE testCQLTable ( id uuid, value varchar, PRIMARY KEY(id) );"));
        assertThat(dataSet.getCQLStatements().get(1),is("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570737,'Cql loaded string');"));
        assertThat(dataSet.getCQLStatements().get(2),is("INSERT INTO testCQLTable( id,value ) values( 1690e8da-5bf8-49e8-9583-4dff8a570738, 'BLA2' );"));
        assertThat(dataSet.getCQLStatements().get(3),is("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570739,'BLA1');"));
    }
}
