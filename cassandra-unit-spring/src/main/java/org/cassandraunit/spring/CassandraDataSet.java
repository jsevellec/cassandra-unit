package org.cassandraunit.spring;

import org.cassandraunit.dataset.DataSetFileExtensionEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This class should be used as follows :</p>
 * <blockquote><pre>
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration
 * @TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, CassandraUnitTestExecutionListener.class })
 * @EmbeddedCassandra
 * @CassandraDataSet
 * public class MyClassTest {
 * @Test
 * public void xxx_xxx() throws Exception {
 * }
 * }
 * </pre></blockquote>
 *
 * or if you use convention over configuration:
 * <blockquote><pre>
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration
 * @TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, CassandraUnitTestExecutionListener.class })
 * @CassandraUnit
 * public class MyClassTest {
 * @Test
 * public void xxx_xxx() throws Exception {
 * }
 * }
 * </pre></blockquote>
 * `class`-dataset.xml
 *
 * @author Olivier Bazoud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface CassandraDataSet {
  String[] value() default {};
  // Only needed if CQL
  String keyspace() default "cassandra_unit_keyspace";
  DataSetFileExtensionEnum type() default DataSetFileExtensionEnum.cql;
}
