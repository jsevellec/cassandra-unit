package org.cassandraunit.spring;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Olivier Bazoud
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = { "classpath:/default-context.xml" })
@TestExecutionListeners({ CassandraUnitTestExecutionListener.class })
@CassandraDataSet(keyspace = "myownkeyspace")
@EmbeddedCassandra
public class CassandraStartAndLoadWithGivenKeyspaceTest {

  @Test
  public void should_work() {
    test();
  }

  @Test
  public void should_work_twice() {
    test();
  }

  private void test() {
    Session session = EmbeddedCassandraServerHelper.getSession();
    ResultSet result = session.execute("select * from testCQLTableKS WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570787");
    String val = result.iterator().next().getString("value");
    assertEquals("KS- Cql loaded string", val);
  }

}
