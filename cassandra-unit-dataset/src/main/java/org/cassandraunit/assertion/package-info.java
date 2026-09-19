/**
 * Asserting what a Cassandra actually holds — the compare direction of DBUnit, which this project
 * claimed since 2010 and only gained in 5.1.0.
 *
 * <p>There are two entry points. They are halves of one feature, not alternatives to each other.
 *
 * <h2>In code</h2>
 *
 * {@link org.cassandraunit.assertion.CqlAssertions} — fluent and AssertJ-native, for a single
 * value, a row count, or a row that should not be there. Nothing to register: these are static
 * methods over a {@code CqlSession}, so they work under any test framework, or none.
 *
 * <pre>
 * import static org.cassandraunit.assertion.CqlAssertions.assertThat;
 *
 * assertThat(session).keyspace("mykeyspace")
 *         .table("widget")
 *             .hasRowCount(3)
 *             .row("id", widgetId)
 *                 .hasValue("label", "one")
 *                 .hasNull("created");
 * </pre>
 *
 * Needs {@code assertj-core} on the test classpath; it is an <em>optional</em> dependency of this
 * artifact, so it reaches nobody who does not ask for it.
 *
 * <h2>From a dataset file</h2>
 *
 * {@link org.cassandraunit.assertion.ExpectedCassandraDataSet} and
 * {@link org.cassandraunit.assertion.ExpectedDataSetFactory} — for stating <em>every</em> row a
 * table should hold. The same YAML, JSON, XML or CSV file can both load the fixture and assert the
 * result, because the load rules and the assert rules are deliberately identical.
 *
 * <pre>
 * &#64;Test
 * &#64;ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace")
 * void shipping_a_widget_marks_it_dispatched() { ... }
 * </pre>
 *
 * <h2>What both guarantee</h2>
 *
 * <ul>
 *   <li><b>Rows are matched on the primary key</b>, read from the live schema, so a wrong value is
 *       reported as one column on the right row rather than as a missing row plus an extra one.</li>
 *   <li><b>One definition of equal.</b> Both go through the same comparison, which is where
 *       Cassandra's rules live: a collection column reads back empty rather than null,
 *       {@code BigDecimal} compares by value rather than by scale, a {@code ByteBuffer} is not
 *       consumed by being read. Expected values are converted using the column's real type, so a
 *       {@code text} column holding {@code "1"} is the string, never the number.</li>
 *   <li><b>A failure and an error are different things.</b> Data that does not match raises an
 *       {@link java.lang.AssertionError} — the code under test is wrong. An assertion that cannot
 *       be carried out at all, because it names a column, table or keyspace that does not exist or
 *       gives a malformed primary key, raises
 *       {@link org.cassandraunit.dataset.ParseException} — the test is wrong. Getting that
 *       backwards sends someone hunting through production code for a typo in a fixture.</li>
 *   <li><b>Never {@code ALLOW FILTERING}</b>, in either direction.</li>
 * </ul>
 *
 * <p>The two can be mixed:
 * {@link org.cassandraunit.assertion.KeyspaceAssert#matches(org.cassandraunit.assertion.ExpectedDataSet)}
 * runs a dataset comparison from inside a fluent chain.
 *
 * <p>See {@code docs/assertions-fluent.md} and {@code docs/assertions.md} for the full treatment of
 * each.
 *
 * @author Jeremy Sevellec
 */
package org.cassandraunit.assertion;
