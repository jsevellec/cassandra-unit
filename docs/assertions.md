# Asserting with a dataset file

A dataset can state what a table *should* contain after a test, not only what it contained before.
This is the half of the DBUnit comparison CassandraUnit never had.

> **Checking one value, or one row count?** Use the fluent API instead — see
> [Asserting in code](assertions-fluent.md). It is the same comparison, without a file. This page is
> for the case a file is good at: stating every row a table should hold.

```java
@Test
@ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace")
void shipping_a_widget_marks_it_dispatched() {
    service.ship(widgetId);
}
```

Verified after the test method, and only if the test passed.

## The same file, both directions

The load rules and the assert rules are deliberately identical, so one file can state the setup and
the expectation:

| In the file | Loading | Asserting |
|---|---|---|
| column **absent** from a row | not written | **not asserted** |
| column present with **`null`** | writes a tombstone | asserts it reads back as null |

That symmetry is the point. It also fixes the limit: on read-back a tombstone and a never-written
cell are indistinguishable, so an expected dataset can assert "this reads as null" but never "this
was tombstoned".

CSV cannot express an explicit null — an empty field means unset — so a CSV expectation can never
assert a null column.

## What gets compared

**Rows are matched on the primary key.** Every expected row must give the full key, and no part of
it may be `null` — Cassandra allows neither, so such a row could never match anything. Both are
errors, not failures. Matching by key is what makes a wrong value report as *one column on the right
row*, rather than as a missing row plus an unexpected one.

**Only the columns the file mentions**, plus the primary key. Nothing else is selected, so an
unrelated column of a type the driver cannot decode cannot break your assertion.

**Never `SELECT *`, never `ALLOW FILTERING`.** The statement is echoed in the failure message so you
can see exactly what was compared.

### Ordering

Across partitions, order is **never** compared, and this is not configurable. An unrestricted
`SELECT` returns partition-token order — stable, but meaningless to whoever wrote the fixture.

Within a partition, order is meaningful and can be asserted with `checkingClusteringOrder()`, off by
default. Database Rider's `orderBy = {...}` has no equivalent here: Cassandra can only order by
clustering columns inside one partition, so the option is a yes/no, not a column list.

### Values

Both sides go through the same codec, so they arrive as the same Java type and compare with
`equals` — which gives collections the right semantics for free: `list` ordered, `set` and `map` not.

Five exceptions, each for a real Cassandra behaviour:

| | |
|---|---|
| **collection `null` ≡ empty** | the driver's codecs decode an absent collection to an empty one, never to null, so these must compare equal |
| **`BigDecimal` by `compareTo`** | `1.5` and `1.50` are the same decimal; `equals` is scale-sensitive |
| **`float` / `double` exact** | a fixture value round-trips exactly. `withNumericTolerance(eps)` is for values the code under test *computed* |
| **`ByteBuffer` duplicated** | reading one moves its position; a consumed buffer must not look different |
| **UDT and tuple** | compared by type plus fields, which works because both sides carry the same type instance |

**Counters are assertable** even though a row dataset cannot write one — they read back as a
`bigint`. **Static columns** need nothing special, but their value repeats on every row of a
partition, so every expected row in that partition must agree on it.

## Strict by default

A table the dataset names must hold **exactly** the rows listed. A table it does not name is not
asserted at all.

This is stricter than DBUnit's usual default, deliberately. Cassandra is upsert-only and has no
unique constraints, so the bug an integration test most needs to catch is a write landing in the
wrong partition or under the wrong clustering key. That produces an *extra* row — invisible to a
contains-style assertion, and with no constraint violation to catch it for you.

`containing()` relaxes it, for suites that pre-seed reference data the fixture does not describe.

`widget: []` therefore means "this table is empty", which falls out for free.

## Options

| | |
|---|---|
| `ignoringColumns("created")` | not compared, not even selected. The usual case is a `now()` timestamp. A primary-key column cannot be ignored — it is how rows are matched |
| `containing()` | allow rows the dataset does not list |
| `withinMentionedPartitions()` | assert only the partitions the dataset names, leaving the rest of the table alone. Still strict inside each named partition |
| `checkingClusteringOrder()` | also assert row order within each partition |
| `withNumericTolerance(0.001)` | absolute tolerance for `float` and `double` |

## The failure message

Values are rendered as CQL literals, so they paste into `cqlsh`:

```
Expected dataset does not match keyspace mykeyspace
  expected : classpath:rows/assertion-data.yaml
  mode     : strict - every row in the asserted scope must be listed

mykeyspace.widget - 3 expected, 3 actual: 1 missing, 1 unexpected, 1 different
  SELECT id, label, tags, created, quantity, ratio, props FROM mykeyspace.widget

  missing (expected, not found in the database)
    id=1690e8da-5bf8-49e8-9583-4dff8a570702
      label    = 'two'
      tags     = {}
      quantity = 7

  unexpected (in the database, not in the expected dataset)
    id=1690e8da-5bf8-49e8-9583-4dff8a5707ff
      label    = 'stray'
      tags     = {}
      created  = NULL

  different
    id=1690e8da-5bf8-49e8-9583-4dff8a570701
      label     expected '1'  but was 'one'
      quantity  expected 42   but was 41
```

The three sections are separate because they have different causes: **missing** means a write did
not happen, **unexpected** means one happened that should not have, **different** means one wrote
the wrong value.

## Failures and errors are different

| | |
|---|---|
| `DataSetMismatchError` (an `AssertionError`) | the data does not match — **the code under test is wrong** |
| `ParseException` | the expectation is unusable: an unknown column, a row missing part of its key or setting part of it to `null`, a `.cql` file used as an expectation — **the test is wrong** |

Test engines report the first as a failure and the second as an error. Getting that backwards sends
someone hunting through production code for a typo in a fixture.

`DataSetMismatchError.getDifferences()` gives the differences as data if you want to react to them
rather than read them.

## Wiring it up

**JUnit 5, embedded server** — nothing to add. `CassandraUnitExtension` picks the annotation up:

```java
@RegisterExtension
static CassandraUnitExtension cassandra = new CassandraUnitExtension(...);

@Test
@ExpectedCassandraDataSet(value = "rows/expected.yaml", keyspace = "mykeyspace")
void myTest() { ... }
```

**JUnit 5, your own Cassandra** — register the extension with your session:

```java
@RegisterExtension
final ExpectedCassandraDataSetExtension expectations =
        new ExpectedCassandraDataSetExtension(fixtures::getSession);
```

**JUnit 4** — a sibling rule, chained. It cannot be folded into `CassandraCQLUnit`, because
`ExternalResource` is handed no `Description` and so cannot see a method annotation:

```java
@Rule
public RuleChain rules = RuleChain.outerRule(cassandra)
        .around(new ExpectedCassandraDataSetRule(cassandra::getSession));
```

**Spring** — nothing to add; the existing listeners check it, before they clean the keyspace.

**Without any framework:**

```java
ExpectedDataSetFactory.fromClassPath("rows/expected-widget.yaml", "mykeyspace")
        .ignoringColumns("created")
        .verify(session);
```

## Not supported

- **TTL and writetime.** Not expressible in the row format, and adding them would mean inventing
  per-column metadata syntax in four file formats. Use a `.cql` script and `SELECT WRITETIME(...)`.
- **Arbitrary `WHERE`.** The two scopes cover the real cases without ever emitting `ALLOW FILTERING`.
- **Placeholder matchers** (`[any]`, `regex:`). They would make the file a third dialect on top of
  YAML and CQL; `ignoringColumns` covers most of what they are reached for.
- **Schema assertions.** A different feature for a different audience.
- Tables are capped at 10,000 rows per comparison. An expectation pointed at a real table should
  fail fast and say so.
