package org.cassandraunit.spring;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.Assert.assertEquals;

/**
 * @author Olivier Bazoud
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({ CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class, DependencyInjectionTestExecutionListener.class })
@CassandraDataSet
@EmbeddedCassandra
public class CassandraStartAndLoadWithSpringDependencyInjectionTest {

  @Autowired
  private ValueContainer valueContainer;
    
  @Test
  public void should_work() {
    Cluster cluster = Cluster.builder()
            .addContactPoints("127.0.0.1")
            .withPort(9042)
            .build();
    Session session = cluster.connect("cassandra_unit_keyspace");
    ResultSet result = session.execute("select * from testCQLTable WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570737");
    String val = result.iterator().next().getString("value");
    assertEquals("Cql loaded string", val);
    assertEquals("Hello", valueContainer.value);
  }

}

@Component
class ValueContainer {
    @Value("${value}")
    public String value;
}