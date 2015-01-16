package org.cassandraunit.spring;

import org.springframework.test.context.TestContext;

/**
 * The goals of this listeners is:
 * - start an embedded Cassandra
 * - load dataset into Cassandra keyspace
 *
 * @author Olivier Bazoud
 */
public class CassandraUnitTestExecutionListener extends AbstractCassandraUnitTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        startServer(testContext);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        cleanServer();
    }

}
