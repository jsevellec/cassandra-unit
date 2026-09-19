package org.cassandraunit.assertion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * What the database should hold once this test has run.
 *
 * <pre>
 * &#64;Test
 * &#64;ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace",
 *                          ignoreColumns = "created")
 * void shipping_a_widget_marks_it_dispatched() {
 *     service.ship(widgetId);
 * }
 * </pre>
 *
 * Verified after the test method, and <b>only if the test passed</b> - an expectation that fired
 * after a failure would bury the real error under a second one.
 * <p>
 * One annotation serves every integration: {@code CassandraUnitExtension} and
 * {@link ExpectedCassandraDataSetExtension} on JUnit 5, {@code ExpectedCassandraDataSetRule} on
 * JUnit 4, and the Spring test listeners. Deliberately not one per module: the Spring module already
 * mirrors {@code @CassandraDataSet} for historical reasons, and three copies of seven attributes
 * would drift - the same argument {@code CQLDataSetFactory} makes for having deleted its
 * {@code type} attribute.
 * <p>
 * Every attribute mirrors a method on {@link ExpectedDataSet}, where the semantics are documented.
 *
 * @author Jeremy Sevellec
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Documented
public @interface ExpectedCassandraDataSet {

    /** Classpath locations of the expected datasets. Several are verified in order. */
    String[] value();

    /** Empty means the session's current keyspace. */
    String keyspace() default "";

    String[] ignoreColumns() default {};

    MatchMode mode() default MatchMode.STRICT;

    Scope scope() default Scope.TABLE;

    boolean checkClusteringOrder() default false;

    double numericTolerance() default 0d;
}
