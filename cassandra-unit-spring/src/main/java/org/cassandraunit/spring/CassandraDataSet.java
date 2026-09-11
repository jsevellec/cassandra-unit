package org.cassandraunit.spring;


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
  /** Classpath locations of the .cql scripts to load. */
  String[] value() default {};

  /** Keyspace the scripts are loaded into, and which is dropped and recreated per dataset. */
  String keyspace() default "cassandra_unit_keyspace";
}
