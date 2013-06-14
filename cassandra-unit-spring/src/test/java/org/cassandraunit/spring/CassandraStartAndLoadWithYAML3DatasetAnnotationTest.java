package org.cassandraunit.spring;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.cassandraunit.dataset.DataSetFileExtensionEnum;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Olivier Bazoud
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = { "classpath:/default-context.xml" })
@TestExecutionListeners({ CassandraUnitTestExecutionListener.class })
@CassandraDataSet(value = { "cql/dataset1.yaml", "cql/dataset2.yaml", "cql/dataset3.yaml" }, type = DataSetFileExtensionEnum.yaml)
@EmbeddedCassandra(port = 9171)
public class CassandraStartAndLoadWithYAML3DatasetAnnotationTest {

  @Test
  public void should_work() throws Exception {
    test();
  }

  @Test
  public void should_work_twice() throws Exception {
    test();
  }

  private void test() throws Exception {
    Cluster cluster = HFactory.getOrCreateCluster("Test Cluster", "localhost:9171");
    List<KeyspaceDefinition> keyspaces = cluster.describeKeyspaces();
    assertThat(cluster.describeKeyspaces(), notNullValue());
    assertThat(keyspaces.size(), is(3 + 1));
    assertThat(cluster.describeKeyspace("mykeyspacename"), notNullValue());
    assertThat(cluster.describeKeyspace("mykeyspacename").getName(), is("mykeyspacename"));

    Keyspace keyspace = HFactory.createKeyspace("mykeyspacename", cluster);
    RangeSlicesQuery<byte[], byte[], byte[]> query = HFactory.createRangeSlicesQuery(
        keyspace,
        BytesArraySerializer.get(),
        BytesArraySerializer.get(),
        BytesArraySerializer.get());
    query.setColumnFamily("testcqltabley1");
    query.setRange(null, null, false, Integer.MAX_VALUE);
    QueryResult<OrderedRows<byte[], byte[], byte[]>> result = query.execute();
    List<Row<byte[], byte[], byte[]>> rows = result.get().getList();
    assertThat(rows.size(), Matchers.is(1));
    assertThat(rows.get(0).getKey(), is(decodeHex("01".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(1));

    assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("01".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("02".toCharArray())));

    query = HFactory.createRangeSlicesQuery(
        keyspace,
        BytesArraySerializer.get(),
        BytesArraySerializer.get(),
        BytesArraySerializer.get());
    query.setColumnFamily("testcqltabley2");
    query.setRange(null, null, false, Integer.MAX_VALUE);
    result = query.execute();
    rows = result.get().getList();
    assertThat(rows.size(), Matchers.is(1));
    assertThat(rows.get(0).getKey(), is(decodeHex("02".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(1));

    assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("03".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("04".toCharArray())));

    query = HFactory.createRangeSlicesQuery(
        keyspace,
        BytesArraySerializer.get(),
        BytesArraySerializer.get(),
        BytesArraySerializer.get());
    query.setColumnFamily("testcqltabley3");
    query.setRange(null, null, false, Integer.MAX_VALUE);
    result = query.execute();
    rows = result.get().getList();
    assertThat(rows.size(), Matchers.is(1));
    assertThat(rows.get(0).getKey(), is(decodeHex("03".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().size(), is(1));

    assertThat(rows.get(0).getColumnSlice().getColumns().get(0), notNullValue());
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getName(), is(decodeHex("04".toCharArray())));
    assertThat(rows.get(0).getColumnSlice().getColumns().get(0).getValue(), is(decodeHex("05".toCharArray())));
  }

}
