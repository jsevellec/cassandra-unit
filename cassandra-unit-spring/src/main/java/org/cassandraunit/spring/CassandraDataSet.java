package org.cassandraunit.spring;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the CQL dataset(s) to load before each test.
 * <p>
 * Annotations inside {@code {@literal @}code} below are escaped on purpose: an unescaped
 * {@code @RunWith} at the start of a javadoc line is parsed as a javadoc tag, which is what
 * used to break the javadoc build.
 *
 * <pre>{@code
 * @ExtendWith(SpringExtension.class)
 * @ContextConfiguration
 * @TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
 *                           CassandraUnitTestExecutionListener.class })
 * @EmbeddedCassandra
 * @CassandraDataSet("cql/dataset.cql")
 * class MyClassTest {
 *     @Test
 *     void xxx_xxx() { }
 * }
 * }</pre>
 *
 * Or, relying on convention over configuration, where the dataset is found at
 * {@code <TestClassName>-dataset.cql} on the classpath:
 *
 * <pre>{@code
 * @ExtendWith(SpringExtension.class)
 * @ContextConfiguration
 * @TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
 *                           CassandraUnitTestExecutionListener.class })
 * @CassandraUnit
 * class MyClassTest {
 *     @Test
 *     void xxx_xxx() { }
 * }
 * }</pre>
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
