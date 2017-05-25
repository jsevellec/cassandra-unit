package org.cassandraunit.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * This {@link TestExecutionListener} differs from {@link CassandraUnitDependencyInjectionTestExecutionListener} 
 * because the database is started and loaded only once, before Spring dependency injection (not after each test method).
 *
 * @author Romain Sertelon
 */
public class CassandraUnitDependencyInjectionIntegrationTestExecutionListener extends AbstractCassandraUnitTestExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUnitDependencyInjectionTestExecutionListener.class);

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    startServer(testContext);
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (Boolean.TRUE.equals(testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
      LOGGER.debug("Cleaning and reloading server for test context [{}]", testContext);
      cleanServer();
      startServer(testContext);
    }
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    cleanServer();
  }
}
