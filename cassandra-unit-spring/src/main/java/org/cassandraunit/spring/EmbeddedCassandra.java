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
  // cassandra configuration file
  String configuration() default EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE;
  // the following settings is needed to load dataset, you must use the same value that can be found in configuration file
  String clusterName() default "Test Cluster";
  String host() default "127.0.0.1";
  // CQL port be default, use 9171 for Thrift
  int port() default 9142;
}
