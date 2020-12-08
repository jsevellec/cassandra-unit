package org.cassandraunit.spring;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.Assert.assertEquals;

/**
 * @author Gaëtan Le Brun
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = {"classpath:/autowired-context.xml"},
  initializers = {CassandraStartAndLoadWithCQLDatasetAnnotationAndAutowiredBeanTest.EnsureUniqueContext.class} )
@TestExecutionListeners({CassandraUnitDependencyInjectionTestExecutionListener.class, DependencyInjectionTestExecutionListener.class})
@CassandraDataSet(value = { "cql/dataset1.cql" })
@EmbeddedCassandra
public class CassandraStartAndLoadWithCQLDatasetAnnotationAndAutowiredBeanTest {
  @Autowired
  private DummyCassandraConnector dummyCassandraConnector;

  @BeforeClass
  public static void beforeClass(){
    DummyCassandraConnector.resetInstancesCounter();
  }

  @Test
  public void should_work() {
    test();
  }

  @Test
  public void should_work_twice() {
    test();
  }

  private void test() {
    ResultSet result = dummyCassandraConnector.getSession().execute("select * from testCQLTable1 WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570717");
    String val = result.iterator().next().getString("value");
    assertEquals("1- Cql loaded string", val);
  }

  @AfterClass
  public static void afterClass(){
    assertEquals(1, DummyCassandraConnector.getInstancesCounter());
  }

  public static class EnsureUniqueContext implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
    }
  }
}
