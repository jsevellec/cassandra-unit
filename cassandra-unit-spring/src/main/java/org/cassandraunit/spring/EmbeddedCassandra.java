package org.cassandraunit.spring;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to start an embedded Cassandra
 *
 * @author Olivier Bazoud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface EmbeddedCassandra {

  /** Classpath name of the Cassandra yaml to start with. */
  String configuration() default EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE;

  /**
   * Directory for the node's data, commitlog, hints, saved caches and cdc files.
   * <p>
   * Empty means {@link EmbeddedCassandraServerHelper#DEFAULT_TMP_DIR}. It cannot be the default
   * value here: annotation defaults must be compile-time constants, and DEFAULT_TMP_DIR is
   * resolved at runtime against {@code java.io.tmpdir} rather than being a literal.
   */
  String tmpDir() default "";

  long timeout() default EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT;

  /**
   * Whether to publish the node's address into the test's {@code Environment} as
   * {@code spring.cassandra.contact-points}, {@code spring.cassandra.port} and
   * {@code spring.cassandra.local-datacenter}.
   * <p>
   * On by default, because without it a Spring-managed {@code CqlSession} - Spring Boot's
   * auto-configured one, say - goes to port 9042 while the embedded node listens on 9142, and with
   * {@code cu-cassandra-rndport.yaml} the port is not knowable in advance at all.
   * <p>
   * Turn it off if you set {@code spring.cassandra.*} yourself and want your values to win: the
   * property source is added first, so otherwise it takes precedence over your configuration file.
   *
   * @see EmbeddedCassandraContextCustomizer
   */
  boolean exposeProperties() default true;
}
