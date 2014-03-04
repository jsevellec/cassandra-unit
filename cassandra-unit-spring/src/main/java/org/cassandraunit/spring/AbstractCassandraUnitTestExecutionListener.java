package org.cassandraunit.spring;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.DataLoader;
import org.cassandraunit.dataset.ClassPathDataSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * The goal of this abstract listener is to provide utility methods for its subclasses to be able to :
 * - start an embedded Cassandra
 * - load dataset into Cassandra keyspace 
 * 
 * @author Gaëtan Le Brun
 */
public abstract class AbstractCassandraUnitTestExecutionListener extends AbstractTestExecutionListener {
  private static final org.slf4j.Logger LOGGER      = LoggerFactory.getLogger(CassandraUnitTestExecutionListener.class);
  private static       boolean          initialized = false;

  protected void startServer(TestContext testContext) throws Exception {
    EmbeddedCassandra embeddedCassandra = Preconditions.checkNotNull(
            AnnotationUtils.findAnnotation(testContext.getTestInstance().getClass(), EmbeddedCassandra.class),
            "CassandraUnitTestExecutionListener must be used with @EmbeddedCassandra on " + testContext.getTestClass());
    if (!initialized) {
      EmbeddedCassandraServerHelper.startEmbeddedCassandra(Optional.fromNullable(embeddedCassandra.configuration()).get());
      initialized = true;
    }

    String clusterName = Preconditions.checkNotNull(embeddedCassandra.clusterName(), "@EmbeddedCassandra host must not be null");
    String host = Preconditions.checkNotNull(embeddedCassandra.host(), "@EmbeddedCassandra clusterName must not be null");
    int port = Preconditions.checkNotNull(embeddedCassandra.port(), "@EmbeddedCassandra port must not be null");
    Preconditions.checkArgument(port > 0, "@EmbeddedCassandra port must not be > 0");

    CassandraDataSet cassandraDataSet = AnnotationUtils.findAnnotation(testContext.getTestInstance().getClass(), CassandraDataSet.class);
    if (cassandraDataSet != null) {
      List<String> dataset = null;
      ListIterator<String> datasetIterator = null;
      String keyspace = cassandraDataSet.keyspace();
      // TODO : find a way to hide them and avoid switch, need some refactoring cassandra-unit
      switch (cassandraDataSet.type()) {
        case cql:
          dataset = dataSetLocations(testContext, cassandraDataSet);
          datasetIterator = dataset.listIterator();

          Cluster cluster = new Cluster.Builder().addContactPoints(host).withPort(port).build();
          Session session = cluster.connect();
          
          CQLDataLoader cqlDataLoader = new CQLDataLoader(session);
          while (datasetIterator.hasNext()) {
            String next = datasetIterator.next();
            boolean dropAndCreateKeyspace = datasetIterator.previousIndex() == 0;
            cqlDataLoader.load(new ClassPathCQLDataSet(next, dropAndCreateKeyspace, dropAndCreateKeyspace, keyspace));
          }
          break;
        default:
          dataset = dataSetLocations(testContext, cassandraDataSet);
          datasetIterator = dataset.listIterator();
          DataLoader dataLoader = new DataLoader(clusterName, host + ":" + port);
          while (datasetIterator.hasNext()) {
            String next = datasetIterator.next();
            boolean dropAndCreateKeyspace = datasetIterator.previousIndex() == 0;
            dataLoader.load(new ClassPathDataSet(next), dropAndCreateKeyspace);
          }
      }
    }

  }

  private List<String> dataSetLocations(TestContext testContext, CassandraDataSet cassandraDataSet) {
    String[] dataset = cassandraDataSet.value();
    if (dataset.length == 0) {
      String alternativePath = alternativePath(testContext.getTestInstance().getClass(), true, cassandraDataSet.type().name());
      if (testContext.getApplicationContext().getResource(alternativePath).exists()) {
        dataset = new String[]{alternativePath.replace(ResourceUtils.CLASSPATH_URL_PREFIX + "/", "")};
      } else {
        alternativePath = alternativePath(testContext.getTestInstance().getClass(), false, cassandraDataSet.type().name());
        if (testContext.getApplicationContext().getResource(alternativePath).exists()) {
          dataset = new String[]{alternativePath.replace(ResourceUtils.CLASSPATH_URL_PREFIX + "/", "")};
        } else {
          LOGGER.info("No dataset will be loaded");
        }
      }
    }
    return Arrays.asList(dataset);
  }

  protected void cleanServer() {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
  }

  protected String alternativePath(Class<?> clazz, boolean includedPackageName, String extension) {
    if (includedPackageName) {
      return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + ClassUtils.convertClassNameToResourcePath(clazz.getName()) + "-dataset" + "." + extension;
    } else {
      return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + clazz.getSimpleName() + "-dataset" + "." + extension;
    }
  }

}
