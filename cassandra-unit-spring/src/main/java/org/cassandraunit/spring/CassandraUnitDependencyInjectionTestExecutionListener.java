package org.cassandraunit.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * The goals of this listeners is:
 * - start an embedded Cassandra before Spring injection
 * - load dataset into Cassandra keyspace
 *
 * @author Gaëtan Le Brun
 */
public class CassandraUnitDependencyInjectionTestExecutionListener extends AbstractCassandraUnitTestExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUnitDependencyInjectionTestExecutionListener.class);

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    startServer(testContext);
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (Boolean.TRUE.equals(testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Cleaning and reloading server for test context [" + testContext + "].");
      }
      cleanServer();
      startServer(testContext);
    }
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    cleanServer();
  }

}
