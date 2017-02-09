package org.cassandraunit.spring.boot;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import org.assertj.core.api.Assertions;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.spring.boot.model.CqlTable1;
import org.cassandraunit.spring.boot.repository.CqlTable1Repository;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.UUID;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author pfrank
 */
@RunWith(SpringRunner.class)
//@SpringBootTest
@EmbeddedCassandraTest
@TestExecutionListeners(listeners = {
    CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@EmbeddedCassandra(configuration = EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE, timeout = 60000)
@CassandraDataSet({"cql/dataset1.cql"})
public class BootApplicationTest {
  private static final Logger LOG = LoggerFactory.getLogger(BootApplicationTest.class);
  @Resource
  private Session session;
  @Resource
  private CqlTable1Repository repository;
  @Resource
  private CassandraTemplate cassandraTemplate;
  @Resource
  private CassandraMappingContext cassandraMappingContext;

  @Test
  public void loadsCassandra(){
    Assertions.assertThat(session).isNotNull();
    ResultSet result = session.execute("select * from testCQLTable1 WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570717");
    String val = result.iterator().next().getString("value");
    assertEquals("1- Cql loaded string", val);
  }

  @Test
  public void cassandraTemplateSelectOne(){
    //GIVEN WHEN
    final CqlTable1 cqlTable1 = cassandraTemplate.selectOne("select * from testCQLTable1 WHERE id=1690e8da-5bf8-49e8-9583-4dff8a570717", CqlTable1.class);

    //THEN
    Assertions.assertThat(cqlTable1).isNotNull();
    Assertions.assertThat(cqlTable1.getValue()).isEqualTo("1- Cql loaded string");
    Collection<CassandraPersistentEntity<?>> cassandraPersistentEntities = cassandraMappingContext.getTableEntities();
    for(CassandraPersistentEntity<?> cassandraPersistentEntity : cassandraPersistentEntities){
      LOG.info("########## - {}", cassandraPersistentEntity.getTableName().getUnquoted());
      LOG.info("########## - {}", cassandraPersistentEntity.getIdProperty().getName());
    }
  }

  @Test
  public void springDataCassandra(){
    //GIVEN
    final UUID uuid = UUID.fromString("1690e8da-5bf8-49e8-9583-4dff8a570717");

    //WHEN
    final CqlTable1 found = repository.findOne(uuid);

    //THEN
    Assertions.assertThat(found).isNotNull();
    Assertions.assertThat(found.getId()).isEqualTo(uuid);
    Assertions.assertThat(found.getValue()).isEqualTo("1- Cql loaded string");
  }
}
