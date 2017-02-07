package org.cassandraunit.spring.boot;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import org.assertj.core.api.Assertions;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author pfrank
 */
@RunWith(SpringRunner.class)
@EmbeddedCassandraTest
@TestExecutionListeners(listeners = {CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@EmbeddedCassandra(configuration = EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE, timeout = 60000)
@CassandraDataSet({"cql/dataset1.cql"})
public class BootApplicationTest {
  @Resource
  private Session session;

  @Test
  public void loadsCassandra(){
    Assertions.assertThat(session).isNotNull();
    ResultSet result = session.execute("select * from testCQLTable1 WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570717");
    String val = result.iterator().next().getString("value");
    assertEquals("1- Cql loaded string", val);
  }
}
