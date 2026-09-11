package org.cassandraunit.dataset.cql;

import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.ParseException;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Jeremy Sevellec
 */
public class ClasspathCQLDataSetTest {

    @Test
    public void shouldGetACQLDataSet() {

        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql");
        assertThat(dataSet).isNotNull();
    }

    @Test
    public void shouldNotGetACQLDataSetBecauseNull() {
        assertThatThrownBy(() -> new ClassPathCQLDataSet(null))
                .isInstanceOf(ParseException.class);
    }

    @Test
    public void shouldNotGetACQLDataSetBecauseItNotExist() {
        assertThatThrownBy(() -> new ClassPathCQLDataSet("cql/unknownDataSet.cql"))
                .isInstanceOf(ParseException.class);
    }

    @Test
    public void shouldGetCQLQueries() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql");
        assertThat(dataSet.getCQLStatements()).isNotNull();
        assertThat(dataSet.getCQLStatements()).isNotEmpty();
        assertThat(dataSet.getCQLStatements()).hasSize(4);
        assertThat(dataSet.getCQLStatements().get(0)).isEqualTo("CREATE TABLE IF NOT EXISTS testCQLTable (id uuid, value varchar, PRIMARY KEY(id));");
        assertThat(dataSet.getCQLStatements().get(1)).isEqualTo("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570737,'Cql loaded string');");
        assertThat(dataSet.getCQLStatements().get(2)).isEqualTo("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570738,'BLA2');");
        assertThat(dataSet.getCQLStatements().get(3)).isEqualTo("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570739,'BLA1');");
    }

    @Test
    public void shouldGetDefinedTestKeyspaceName() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace");
        assertThat(dataSet.getKeyspaceName()).isEqualTo("mykeyspace");
    }

    @Test
    public void shouldGetCQLQueriesFromMultiLineCQLScript() {
        CQLDataSet dataSet = new ClassPathCQLDataSet("cql/multiLineStatements.cql");
        assertThat(dataSet.getCQLStatements()).isNotNull();
        assertThat(dataSet.getCQLStatements()).isNotEmpty();
        assertThat(dataSet.getCQLStatements()).hasSize(4);
        assertThat(dataSet.getCQLStatements().get(0)).isEqualTo("CREATE TABLE testCQLTable ( id uuid, value varchar, PRIMARY KEY(id) );");
        assertThat(dataSet.getCQLStatements().get(1)).isEqualTo("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570737,'Cql loaded string');");
        assertThat(dataSet.getCQLStatements().get(2)).isEqualTo("INSERT INTO testCQLTable( id,value ) values( 1690e8da-5bf8-49e8-9583-4dff8a570738, 'BLA2' );");
        assertThat(dataSet.getCQLStatements().get(3)).isEqualTo("INSERT INTO testCQLTable(id, value) values(1690e8da-5bf8-49e8-9583-4dff8a570739,'BLA1');");
    }
}
