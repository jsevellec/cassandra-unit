# Asserting in code

Fluent, AssertJ-native assertions on what a Cassandra actually holds.

A dataset file is the right tool for *these are all the rows this table should hold* — that is
[Asserting with a dataset file](assertions.md). For one value, one row count, or one row that should
not be there, writing a file is out of proportion. This is that half.

```java
import static org.cassandraunit.assertion.CqlAssertions.assertThat;

assertThat(session).keyspace("mykeyspace")
        .table("widget")
            .hasRowCount(3)
            .row("id", widgetId)
                .hasValue("label", "one")
                .hasNull("created");
```

It is the **same comparison** underneath. Both halves share one value-equality implementation and
one value converter, so they cannot drift — everything in
[Values](assertions.md#values) on the dataset page holds here too, and a test in the suite asserts
the two pass and fail together on the same data.

## A worked example

Nothing to register, nothing to annotate. This is a complete test:

```java
import com.datastax.oss.driver.api.core.CqlSession;
import org.cassandraunit.CassandraUnitExtension;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.cassandraunit.assertion.CqlAssertions.assertThat;

class ShippingTest {

    @RegisterExtension
    static final CassandraUnitExtension cassandra =
            new CassandraUnitExtension(new ClassPathCQLDataSet("cql/schema.cql", "mykeyspace"));

    @Test
    void shipping_a_widget_marks_it_dispatched(CqlSession session) {
        UUID id = UUID.randomUUID();
        session.execute("INSERT INTO mykeyspace.widget (id, label, status) "
                + "VALUES (?, 'crate', 'pending')", id);

        new ShippingService(session).ship(id);

        assertThat(session).keyspace("mykeyspace")
                .table("widget")
                    .hasRowCount(1)
                    .row("id", id)
                        .hasValue("status", "dispatched")
                        .hasNonNull("shipped_at");
    }
}
```

The extension here only starts the server and loads the schema. The assertion needs neither — see
[Where it runs](#where-it-runs).

## Getting there

Four levels, each returning the next:

```
assertThat(session)                 CqlSessionAssert
    .keyspace("mykeyspace")         KeyspaceAssert
        .table("widget")            TableAssert
            .row("id", widgetId)    RowAssert
```

Two shortcuts skip the navigation entirely, for when you already hold the data:

```java
assertThat(row).hasValue("label", "one");
assertThat(resultSet).hasSize(3);
```

`assertThat` is overloaded on `CqlSession`, `Row` and `ResultSet`, so it can be statically imported
next to `org.assertj.core.api.Assertions.*` without ambiguity — the same arrangement `assertj-guava`
uses.

**Navigating somewhere that does not exist is an error, not a failure.** `keyspace("nope")` and
`table("nope")` throw `ParseException`: an assertion about data in a keyspace that was never created
cannot have been what you meant, so the test is wrong rather than the code under test. Use
`hasKeyspace` / `hasTable` when absence is the thing you are asserting. This is the same split the
dataset page describes under
[Failures and errors are different](assertions.md#failures-and-errors-are-different).

## Method reference

### `assertThat(CqlSession)` → `CqlSessionAssert`

| | |
|---|---|
| `hasKeyspace(name)` | fails unless the keyspace exists |
| `doesNotHaveKeyspace(name)` | fails if it exists |
| `keyspace(name)` | narrows to it; **`ParseException`** if absent |

### `.keyspace(name)` → `KeyspaceAssert`

| | |
|---|---|
| `hasTable(name)` | fails unless the table exists |
| `doesNotHaveTable(name)` | fails if it exists |
| `matches(expectedDataSet)` | compares the whole keyspace against a dataset file — see [Mixing the two](#mixing-the-two) |
| `table(name)` | narrows to it; **`ParseException`** if absent |

### `.table(name)` → `TableAssert`

| | |
|---|---|
| `hasRowCount(n)` | exact count |
| `isEmpty()` / `isNotEmpty()` | count is / is not zero |
| `row(column, value)` | narrows to one row, single-column primary keys only |
| `row(Map)` | narrows to one row by a compound primary key |
| `hasNoRow(column, value)`, `hasNoRow(Map)` | fails if such a row exists |
| `rows()` | every row, as a `CqlRowsAssert` |

`row(...)` **fails** when no such row exists — that is a claim about data. Giving it the wrong
*shape* of key is a **`ParseException`**: see [Addressing a row](#addressing-a-row).

### `.row(...)` → `RowAssert`

| | |
|---|---|
| `hasValue(column, expected)` | compares one column; expected value is coerced, see [Values](#values) |
| `hasValues(Map)` | several at once; see the ordering caveat under [Limits](#limits) |
| `hasNull(column)` | column reads back null — or empty, for a collection |
| `hasNonNull(column)` | column holds something |

A column the row does not have is a **`ParseException`**, listing the columns it does have.

### `.rows()` and `assertThat(ResultSet)` → `CqlRowsAssert`

| | |
|---|---|
| `hasSize(n)`, `isEmpty()`, `isNotEmpty()` | on the row count |
| `first()` | the first row, in whatever order the server returned |
| `singleRow()` | the only row; fails unless there is exactly one |
| `extracting(column)` | hands that column's values to AssertJ as a `ListAssert` |

`assertThat(resultSet)` consumes the result set — it is a one-pass cursor, so do not iterate it
yourself afterwards.

## Values

Expected values are converted to the column's type before comparison, using the same converter a row
dataset uses. So they can be written the way you would write them in a fixture, rather than the way
the driver happens to represent them:

```java
.hasValue("quantity", 42)                                  // int literal vs a bigint column
.hasValue("id", "1690e8da-5bf8-49e8-9583-4dff8a570701")    // string vs a uuid column
.hasValue("created", "2026-09-19T10:00:00Z")               // string vs a timestamp column
```

A value the column's codec already accepts is passed through untouched, so handing over a real
`UUID`, `Instant` or `Set<String>` costs nothing and cannot be mangled.

Comparison itself is [the same five normalizations](assertions.md#values) the dataset page
documents. The one worth repeating, because it surprises people: **a collection column never reads
back as null**, so `hasNull("tags")` and `hasValue("tags", Set.of())` are the same assertion.

Counters are assertable even though a row dataset cannot write one — they read back as a `bigint`.

### Addressing a row

`row("id", value)` is shorthand for a single-column primary key. On a table with a compound key it
throws `ParseException` naming the real key, rather than quietly matching whichever row a partial
key returned first:

```java
.row(Map.of("day", "2026-09-19", "at", "2026-09-19T12:00:00Z"))
```

A key column set to `null` is refused the same way. Cassandra permits no null in a primary-key
column, so such a key could never match anything.

## Beyond a single assertion

Every assert type extends AssertJ's `AbstractAssert`, which is the reason to build on AssertJ rather
than invent a fluent API — all of this comes for free:

```java
assertThat(session).keyspace("mykeyspace").table("widget")
        .as("after shipping %s", widgetId)      // names the assertion in the failure
        .hasRowCount(1);

// collect every failure instead of stopping at the first
SoftAssertions softly = new SoftAssertions();
softly.check(() -> assertThat(session).keyspace("mykeyspace")
        .table("widget").row("id", widgetId).hasValue("status", "dispatched"));
softly.check(() -> assertThat(session).keyspace("mykeyspace")
        .table("audit").hasRowCount(1));
softly.assertAll();
```

`extracting` hands off to AssertJ's own collection vocabulary:

```java
assertThat(session).keyspace("mykeyspace").table("widget").rows()
        .extracting("label")
        .containsExactlyInAnyOrder("one", "two", "three");
```

## Where it runs

**Anywhere you have a `CqlSession`.** There is no extension, rule or annotation to register — these
are static methods. That makes them usable in places the dataset annotation is not:

| | |
|---|---|
| JUnit 5 | works; `CassandraUnitExtension` or `CqlDataSetExtension` can supply the session as a test parameter |
| JUnit 4 | works, with no rule beyond whatever starts your Cassandra |
| Spring | works in any test; no listener needed |
| no framework | works |
| your own Cassandra | works — Testcontainers, a local node, ScyllaDB, Astra. Nothing here needs the embedded server |

## Failures and errors

Values are rendered as CQL literals, so they paste into `cqlsh`:

```
Expected column label of mykeyspace.widget to be
  'one'
but was
  '1'
```

```
Expected mykeyspace.widget to hold
  5 rows
but it holds
  3
```

```
Expected mykeyspace.widget to hold a row with
  id=1690e8da-5bf8-49e8-9583-4dff8a5707ff
but it holds no such row
```

The split is the same one the dataset page sets out in
[Failures and errors are different](assertions.md#failures-and-errors-are-different):

| | |
|---|---|
| `AssertionError` | the data does not match — **the code under test is wrong** |
| `ParseException` | the assertion is unusable: an unknown column, table or keyspace, a malformed or null primary key — **the test is wrong** |

## Limits

- **`row(...)` and `rows()` issue `SELECT *`**, unlike a dataset comparison, because there is no
  expected-column set to narrow to. Both restrict by primary key or not at all, so neither can emit
  `ALLOW FILTERING`.
- **`rows()` refuses more than 10,000 rows**, the same ceiling a dataset comparison uses. Assert on
  one row, or on the count, instead.
- **`hasRowCount` issues `SELECT count(*)`**, which makes the server log *"Aggregation query used
  without partition key"*. Expected, and harmless at test-fixture sizes.
- **`hasValues(Map)` stops at the first mismatch**, and a `Map.of(...)` has no defined iteration
  order, so with two wrong columns the one reported can vary between runs. Pass a `LinkedHashMap`,
  or chain separate `hasValue` calls, when that matters.
- **No assertions on schema, TTL or writetime.** Same boundaries as the dataset comparison.

## Mixing the two

They are halves of one feature, and `matches` is the seam:

```java
assertThat(session).keyspace("mykeyspace")
        .hasTable("widget")
        .matches(ExpectedDataSetFactory.fromClassPath("rows/expected.yaml", "mykeyspace"))
        .table("audit")
            .hasRowCount(1);
```

It sits on the keyspace rather than the table because that is the scope a dataset really covers — a
dataset file can name several tables, and putting `matches` on a table would misrepresent what was
compared.

## The dependency

Needs `assertj-core` on the test classpath. It is declared **optional** in
`cassandra-unit-dataset`, so it reaches nobody who does not ask for it and nothing else in the
library touches it — which means you must declare it yourself, as you almost certainly already do:

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

Supported baseline is **assertj-core 3.x**. Without it on the classpath, touching `CqlAssertions`
raises `NoClassDefFoundError: org/assertj/core/api/AbstractAssert`.
