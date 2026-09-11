package org.cassandraunit.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Bazoud
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(value = { "classpath:/default-context.xml" })
@TestExecutionListeners({ CassandraUnitTestExecutionListener.class })
@CassandraDataSet(value = { "cql/dataset1.cql" })
@EmbeddedCassandra
public class CassandraStartAndLoadWithCQLDatasetAnnotationTest {

  @Test
  public void should_work() {
    test();
  }

  @Test
  public void should_work_twice() {
    test();
  }

  private void test() {
    CqlSession session = EmbeddedCassandraServerHelper.getSession();
    ResultSet result = session.execute("select * from testCQLTable1 WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570717");
    String val = result.iterator().next().getString("value");
    assertThat(val).isEqualTo("1- Cql loaded string");
  }

}
