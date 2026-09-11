package org.cassandraunit.spring;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gaëtan Le Brun
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(value = {"classpath:/autowired-context.xml"},
    initializers = {CassandraStartAndLoadWithCQLDatasetAnnotationAndAutowiredBeanAndDirtiesContextTest.EnsureUniqueContext.class} )
@TestExecutionListeners({CassandraUnitDependencyInjectionTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@CassandraDataSet(value = {"cql/dataset1.cql"})
@EmbeddedCassandra
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class CassandraStartAndLoadWithCQLDatasetAnnotationAndAutowiredBeanAndDirtiesContextTest {
    @Autowired
    private DummyCassandraConnector dummyCassandraConnector;

    @BeforeAll
    public static void beforeClass() {
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
        assertThat(val).isEqualTo("1- Cql loaded string");
    }

    @AfterAll
    public static void afterClass(){
        assertThat(DummyCassandraConnector.getInstancesCounter()).isEqualTo(2);
    }

    public static class EnsureUniqueContext implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
        }
    }
}
