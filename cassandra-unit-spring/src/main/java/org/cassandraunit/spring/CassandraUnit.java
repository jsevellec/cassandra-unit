package org.cassandraunit.spring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A composition of @CassandraDataSet and @EmbeddedCassandra with default settings.
 *
 * @author Olivier Bazoud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@CassandraDataSet
@EmbeddedCassandra
public @interface CassandraUnit {
}
