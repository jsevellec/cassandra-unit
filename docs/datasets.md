# Datasets

A dataset is what CassandraUnit loads into the embedded node before your test runs — usually a file,
but a row dataset can also be [built in Java](#in-java-with-no-file). There are two kinds, and most
projects end up using both:

| | **CQL script** | **Row dataset** |
|---|---|---|
| Extensions | `.cql` | `.yaml`, `.yml`, `.json`, `.xml`, `.csv` |
| Contains | any CQL statements | rows, and nothing else |
| Creates the schema | yes | no — it needs one to already exist |
| Values are written as | CQL literals you format yourself | plain values, converted using the real column types |
| Loaded by | executing each statement | binding prepared statements |

The format is chosen by **file extension**. There is no `type` attribute to keep in sync with the
filename.

> **These are not the 4.x XML/JSON/YAML formats.** Those described Thrift column families
> (`columnFamilies`, `superColumns`, `comparatorType`) and their loaders were deleted in 2016. The
> formats here describe CQL tables and share nothing with them. An old 4.x dataset will not load.

## Choosing

**Use a CQL script** for schema — always — and for anything a row dataset deliberately cannot
express: counters, `USING TTL`, `USING TIMESTAMP`, `DELETE`, or a keyspace with its own replication
settings.

**Use a row dataset** for fixture data, especially when there is a lot of it or when the values are
awkward to write as CQL literals. `uuid`, `timestamp`, `blob`, collections and UDTs all convert from
their natural form, so there is no quoting to get wrong. CSV in particular lets fixtures come
straight out of a spreadsheet or a production export.

**If in doubt, use a CQL script.** It is the simpler thing and it can do everything.

**For a handful of rows, skip the file.** [A builder](#in-java-with-no-file) produces the same row
dataset in Java, next to the test that needs it.

## A first pair

`src/test/resources/cql/schema.cql` — the schema:

```sql
CREATE TABLE widget (
    id      uuid PRIMARY KEY,
    label   text,
    tags    set<text>,
    created timestamp
);
```

Note what is *absent*: no `CREATE KEYSPACE`, no `USE`. By default CassandraUnit drops and recreates
the keyspace and switches to it before running your script, so the script only describes tables. See
[Keyspace handling](#keyspace-handling) to change that.

`src/test/resources/data/widget.yaml` — the rows:

```yaml
widget:
  - id: 1690e8da-5bf8-49e8-9583-4dff8a570737
    label: "hello"
    tags: [alpha, beta]
    created: "2026-09-19T10:00:00Z"
```

A CQL script can of course carry its rows too, as `INSERT` statements — the pair above is one way to
split the work, not a requirement.

## Loading a dataset

`CQLDataSetFactory` builds the right dataset for any location, picking the format from the
extension:

```java
CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace")     // from the classpath
CQLDataSetFactory.fromFile("/tmp/widget.yaml", "mykeyspace")        // from the filesystem
```

Classpath is what you want in almost every case. The location is resolved as a classpath resource,
so `cql/schema.cql` means `src/test/resources/cql/schema.cql`.

Both take the same four argument shapes:

| Arguments | Meaning |
|---|---|
| `(location)` | default keyspace `cassandraunitkeyspace`, dropped and created |
| `(location, keyspaceName)` | named keyspace, dropped and created |
| `(location, keyspaceCreation, keyspaceDeletion)` | control both flags |
| `(location, keyspaceCreation, keyspaceDeletion, keyspaceName)` | all four |

A missing or unreadable dataset raises `ParseException` from the factory, not later at load time, so
a typo in the path fails fast. So does an extension nothing can read.

For a CQL script you can also construct the class directly, which is what older code does and what
you need if you want the concrete type:

```java
new ClassPathCQLDataSet("cql/schema.cql", "mykeyspace")
new FileCQLDataSet("/tmp/schema.cql", "mykeyspace")
```

Both also accept `(location, keyspaceCreation)`, and `ClassPathCQLDataSet` additionally accepts
`(location, keyspaceCreation, keyspaceName)`. `CQLDataSetFactory.fromClassPath` and `fromFile`
return exactly these objects for a `.cql` location, so the two routes are interchangeable.

## Loading several

Almost every suite loads more than one file, and a row dataset always does — it needs its schema
first. `fromClassPathAll` loads them in order as a single dataset:

```java
CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml")
```

The keyspace is dropped and created **once**, for the chain, and each file then loads into it. Use
this form whenever you have more than one file.

It also matters because `CassandraCQLUnit` and `CassandraUnitExtension` take exactly **one** dataset:

```java
@RegisterExtension
static CassandraUnitExtension cassandra = new CassandraUnitExtension(
        CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml"));
```

Without it there is no way to give a row dataset its schema through the rule or the extension.

`fromClassPathAllKeepingKeyspace` is the same thing with the drop and create left off, for adding
rows to a keyspace something else already built.

### Doing it by hand

If you load through `CQLDataLoader` yourself, you own the keyspace flags:

```java
CQLDataLoader loader = new CQLDataLoader(session);   // embedded, Testcontainers, anything
loader.load(CQLDataSetFactory.fromClassPath("cql/schema.cql", true,  true,  "mykeyspace"));
loader.load(CQLDataSetFactory.fromClassPath("data/widget.yaml", false, false, "mykeyspace"));
```

The first load creates the keyspace, the second **must not drop it**. Getting this backwards is the
usual cause of "my data disappeared" — the second dataset dropped the keyspace the first had
populated. `fromClassPathAll` exists so you do not have to get it right by hand.

Note *why* the second load lands in the right keyspace: `CQLDataLoader.load` issues `USE` at the
end, so the first load leaves the session pointing at `mykeyspace` and the second inherits it. It
follows that a second **CQL script** naming a different keyspace, with `keyspaceCreation` off, would
run its statements against whatever keyspace was current — see
[issue #160](https://github.com/jsevellec/cassandra-unit/issues/160). Qualify table names as
`keyspace.table` in any CQL script that does not create its own keyspace. Row datasets are immune:
they qualify every statement with the dataset's keyspace themselves.

## Keyspace handling

Two independent flags control what happens to the keyspace before a dataset loads. They work the
same way for both kinds:

- **`keyspaceCreation`** (default `true`) — issue `CREATE KEYSPACE IF NOT EXISTS` and then `USE` it.
- **`keyspaceDeletion`** (default `true`) — issue `DROP KEYSPACE` first.

With both defaults, each load gives you an empty keyspace. **This is what isolates one test from
the next** — neither the JUnit rule nor the JUnit 5 extension wipes anything itself, so the
dataset's flags are the isolation mechanism. This matters if you change them.

The keyspace is created as:

```sql
CREATE KEYSPACE IF NOT EXISTS <name>
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
  AND durable_writes = false
```

`durable_writes = false` skips the commit log, which is a significant speed-up and is safe for a
throwaway instance. If you need different replication, set `keyspaceCreation` to `false` and write
your own `CREATE KEYSPACE` as the first statement of a CQL script.

For a chain built with `fromClassPathAll`, the flags belong to the **chain**, not to its members:
the composite drops and creates once, and its members never touch the keyspace.

### Keeping data across tests in a class

```java
CQLDataSetFactory.fromClassPath("cql/schema.cql", false, false, "mykeyspace")
```

Creation and deletion both off: the dataset runs against whatever is already there. Useful for a
schema-once, insert-per-test arrangement — but then the tests share state, and their order starts
to matter.

## Row dataset formats

A row dataset describes **data only**, and that split is what makes the values right. Because the
schema is already in the database when rows load, the loader reads each column's type from **there**
rather than making the fixture re-declare it, and the driver's own codecs do the conversion.

The practical consequence is the one that used to be a bug: a `text` column holding `"1"` stays the
string `"1"`. It does not become the number 1.

All four formats share one model — tables, rows, and values — so the rules below about null, about
collections and about type conversion apply to every one of them. Only the syntax differs. So does
[the builder](#in-java-with-no-file), which is the same model with no file at all.

### null, and the absence of a value

Two different things, and the difference is visible in the database:

| In the file | Meaning | Effect |
|---|---|---|
| the column is **absent from that row** | unset | the column is not in the generated `INSERT` at all: nothing is written, and an existing value is left alone |
| the column is present with a **null** value | explicit null | a tombstone is written, erasing any existing value |

Rows in one file may each set a different subset of columns; they do not have to agree.

### Type conversion

Values are converted against the column's real type, so you write them in their natural form:

| Column type | Write it as |
|---|---|
| `text`, `ascii` | a string. A number is accepted when its decimal form is exact — `label: 1` gives `"1"` — but `1.10` is **refused**, because it would silently become `"1.1"`. Quote it. |
| numerics | a number, or a string. An `int` in a `bigint` column widens. |
| `boolean` | `true` / `false` |
| `uuid`, `timeuuid` | the usual dashed form |
| `timestamp` | an ISO-8601 string, epoch millis, or (in YAML) an unquoted date |
| `date`, `time`, `inet` | their usual string form |
| `blob` | a `0x`-prefixed hex string — **quote it in YAML**, or YAML reads it as a number |
| `list`, `set` | a list. A single bare value counts as a list of one. |
| `map` | a map |
| UDT | a map keyed by field name — or, from a builder, a `UdtValue` of that type |
| tuple | the CQL literal, e.g. `"(1, 'a')"` — or, from a builder, a `TupleValue` |

A conversion that cannot be made fails with a message naming the dataset, the table, the column and
its type — not a bare server error.

### YAML and JSON

A map of table name to a list of rows. Several tables per file are fine.

```yaml
widget:
  - id: 1690e8da-5bf8-49e8-9583-4dff8a570737
    label: "1"                      # the text "1"
    tags: [alpha, beta]
    created: "2026-09-19T10:00:00Z"
    props: {a: 1, b: 2}
  - id: 1690e8da-5bf8-49e8-9583-4dff8a570738
    label: null                     # tombstone
    # tags absent                   -> unset
```

```json
{ "widget": [ { "id": "1690e8da-5bf8-49e8-9583-4dff8a570737", "label": "1",
                "tags": ["alpha", "beta"] } ] }
```

If the document's root is a bare list of rows rather than a map, the table name comes from the
filename — `widget.yaml` loads into `widget`.

**Quote anything whose YAML meaning differs from its CQL meaning.** `0x0a0b` unquoted is the number
2571 to YAML, so a `blob` needs `"0x0a0b"`; the same goes for a number-shaped value belonging in a
`text` column. An unquoted timestamp works either way.

A duplicate key in a row is an error, not last-one-wins.

### XML

The same shape. Every value is a string, which is fine — the column type decides what it becomes.
XML has no null of its own, hence `null="true"`; an absent element means unset.

```xml
<dataset>
  <table name="widget">
    <row>
      <id>1690e8da-5bf8-49e8-9583-4dff8a570737</id>
      <label>1</label>
      <tags><value>alpha</value><value>beta</value></tags>
      <props><entry key="a">1</entry></props>
    </row>
    <row>
      <id>1690e8da-5bf8-49e8-9583-4dff8a570738</id>
      <label null="true"/>
    </row>
  </table>
</dataset>
```

Parsed with the JDK's own parser, with DOCTYPE and external entities disabled — a dataset is data,
and should not be able to read your filesystem.

### CSV

Flat, so **one file is one table**, named after the file: `data/widget.csv` loads into `widget`. The
header row gives the column names.

```csv
id,label,tags,created
1690e8da-5bf8-49e8-9583-4dff8a570737,1,alpha|beta,2026-09-19T10:00:00Z
```

Collections are split on `|`. Quoting, embedded commas and embedded newlines follow ordinary CSV
rules.

**An empty field means unset, not null**, and CSV has no way to say "explicitly null" — use YAML or
JSON when you need a tombstone. A `NULL` sentinel was deliberately not invented: a `text` column can
legitimately contain the string `NULL`, and a sentinel would corrupt it silently.

CSV is the one format needing a dependency that is not already there:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-csv</artifactId>
  <scope>test</scope>
</dependency>
```

It is declared `optional` by `cassandra-unit`, so you only pay for it if you use it — YAML, JSON and
XML need nothing. Loading a `.csv` dataset without it fails immediately, with a message saying
exactly this.

### In Java, with no file

For a handful of rows a file is a lot of ceremony, and it puts the fixture somewhere other than the
test that depends on it. A builder produces the same row dataset in code:

```java
RowsCQLDataSet fixtures = CQLDataSetFactory.builder("mykeyspace")
        .table("widget").columns("id", "label", "quantity")
            .row(id1, "one", 42)
            .row(id2, "two", 7)
            .row(id3, null, 0)
        .table("event").columns("day", "at", "kind")
            .row("2026-09-19", Instant.parse("2026-09-19T12:00:00Z"), "start")
        .build();

new CQLDataLoader(session).load(fixtures);
```

It is **not a second way of loading rows**. `build()` returns an ordinary `RowsCQLDataSet`, so from
there on everything is the code a file goes through: the column types still come from the live
schema, the values still go through the same converter, and the keyspace handling, the isolation
modes, the extensions and the JUnit 4 rule all behave identically. A test in the suite builds
`rows/assertion-data.yaml` in code and checks each direction against the other, so the two cannot
drift.

**Hand over the object you already have.** This is the one thing a file cannot do — a `UUID`, an
`Instant`, a `Set<String>` are passed straight through when the column's codec already accepts them,
and so are a `UdtValue` and a `TupleValue` of that exact type, which no file could express at all.
The written forms still work, converted exactly as a CSV file's would be, so a fixture can mix the
two.

**Columns are declared once per table**, and the values of each `row(...)` line up with them.
Returning to a table appends rows and keeps its columns, which is what lets a loop add to a table it
opened earlier:

```java
        .table("widget")
            .row(id4, "four", 1)
```

**A row of a different shape names its own columns.** Rows in one dataset do not have to agree on
which columns they set — absent still means unset — so the escape hatch is a map:

```java
        .table("widget")
            .row(Map.of("id", id5, "tags", Set.of("alpha")))
```

Note that `Map.of` rejects null values. An explicit null — the tombstone — needs either the
positional form, as `id3` above, or a `LinkedHashMap`.

**Keyspace creation and deletion default to off**, unlike `fromClassPath`. A builder describes rows
and never schema, so dropping its keyspace would destroy the tables its own inserts need.
`builder(keyspace, true, true)` is there for the rare fixture that really should own its keyspace.

**Name it if you have several.** `describe()` prefixes every parse error and every assertion
failure, and a file shows its path there. A built dataset says `a dataset built in code` unless
`named("the shipping fixture")` gives it something better.

The same object can also state the expectation, because an expected dataset is a row dataset read
the other way round — see [Asserting with a dataset file](assertions.md):

```java
ExpectedDataSetFactory.of(fixtures, "mykeyspace").verify(session);
```

Declare the variable as `RowsCQLDataSet` rather than `CQLDataSet` if you want both uses from one
object: `ExpectedDataSetFactory.of` is typed on the concrete class.

### What a row dataset cannot do

Counters (they need `UPDATE ... SET c = c + n`), `USING TTL`, `USING TIMESTAMP`, `DELETE`, and any
schema change. Use a CQL script for those — that is what the pair is for.

## CQL statement parsing rules

These apply to `.cql` scripts only. The parser is deliberately small — a lexer that strips comments
and splits on semicolons, not a CQL grammar. The exact rules, from
[`SimpleCQLLexer`](../cassandra-unit-dataset/src/main/java/org/cassandraunit/dataset/cql/SimpleCQLLexer.java):

**Statements are separated by semicolons.** The final one may omit its semicolon — whatever is left
over at end of file is executed as a statement — but terminate every statement anyway, so that
appending to the file later cannot accidentally merge two statements into one.

**Statements may span multiple lines.** Newlines are replaced with spaces, so this is one statement:

```sql
CREATE TABLE widget (
    id    int PRIMARY KEY,
    label text
);
```

**Three comment syntaxes are stripped:**

```sql
-- a line comment
// also a line comment
/* a block comment,
   possibly spanning lines */
```

**Quoted strings are respected**, so a semicolon or a comment marker inside a literal does not end
the statement or vanish:

```sql
INSERT INTO widget (id, label) VALUES (1, 'a; semicolon and -- a dash');
```

**A doubled quote escapes a quote**, the CQL convention, so a literal can contain an apostrophe:

```sql
INSERT INTO widget (id, label) VALUES (2, 'it''s fine');
```

**Double-quoted identifiers are preserved case-sensitively**, and the parser keeps them intact:

```sql
CREATE TABLE "MixedCase" (id int PRIMARY KEY);
```

The parser also understands `""` inside a double-quoted identifier, but that is of no practical
use: Cassandra rejects a table name containing anything but letters, digits and underscores, so
`"Mixed""Case"` parses correctly and is then refused by the server with
`Table name must not be empty or not contain non-alphanumeric-underscore characters`. Quoting an
identifier buys you case sensitivity, nothing more.

**Files are read as UTF-8** as of 5.0.0 — row datasets too. Earlier versions used the platform
default encoding, so the same dataset could parse differently on different machines.

## Isolation: truncating instead of dropping

Dropping and recreating a keyspace per test is simple but not free — the schema has to be built
again every time. `CQLDataLoader.Isolation` picks what a load clears:

| | |
|---|---|
| `DATASET` | honour the dataset's own creation and deletion flags — normally drop the keyspace and create it again. **The default**, and what every release before 5.1.0 did |
| `TRUNCATE` | keep the keyspace and its schema, empty every table in it instead. The keyspace is created first if it is not there |
| `NONE` | clear nothing. The keyspace is selected if it exists, so an unqualified statement still lands where it should |

```java
new CQLDataLoader(session).load(rows, Isolation.TRUNCATE);
```

On the extensions:

```java
CqlDataSetExtension.using(...)
        .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
        .rowsPerTest(CQLDataSetFactory.fromClassPath("rows/widget.yaml", false, false, "mykeyspace"))
        .isolation(Isolation.TRUNCATE)
        .build();

new CassandraUnitExtension(dataSet).withIsolation(Isolation.TRUNCATE);
```

**With `TRUNCATE`, the per-test dataset must not re-create the schema it loads into.** Either pair
it with `schemaOnce` — or `CQLDataLoader.loadIfKeyspaceAbsent`, which is the same thing by hand and
reports whether it loaded — and keep the per-test dataset to rows, or write
`CREATE TABLE IF NOT EXISTS` in the script. The mode ignores the dataset's own creation and
deletion flags, deliberately: asking for it is asking for the keyspace to survive.

### How much faster

Measured on the embedded server, median of ten cycles after three warm-ups, with twenty rows per
table put back before each timed cycle. Both arms include the work the other avoids: `DATASET` is
`DROP KEYSPACE` + `CREATE KEYSPACE` + one `CREATE TABLE` per table, `TRUNCATE` is one `TRUNCATE`
per table. Loading the rows is common to both and excluded from both.

| tables | `DATASET` | `TRUNCATE` |
|---|---|---|
| 2 | 940 ms | 1.6 ms |
| 10 | 1022 ms | 4.1 ms |
| 50 | 1640 ms | 10.7 ms |

Reproduce it with
`mvn -pl cassandra-unit -am test -Dtest=IsolationBenchmarkTest -Dsurefire.failIfNoSpecifiedTests=false -Dcassandraunit.benchmark=true`.

The gap closes as tables are added — truncation costs one round trip per table, while schema
changes agree in something closer to a batch — but it closes far too slowly to matter: the two
would not meet until a keyspace held thousands of tables.

What keeps this from being the default is not speed. It is **not** a drop-in — a dataset that
creates its own schema breaks under it, as above — and changing the default would alter what an
existing suite's keyspace looks like between tests.

The numbers are one embedded node, where schema agreement has no peers to wait for. That is the
friendliest case `DATASET` will ever get, so treat the gap as a lower bound on what `TRUNCATE`
saves against a container or a real cluster.

> **`auto_snapshot` costs both modes, not only this one.** `TRUNCATE` snapshots each table it
> empties unless the server sets `auto_snapshot: false` — but so does `DROP KEYSPACE`, for each
> table it drops: Cassandra gates `truncateBlocking` and `onTableDropped` on the same setting. So
> leaving it on does not change which mode is faster; it makes *both* write a snapshot per table
> per test and fill the disk. The yaml files shipped with the embedded server turn it off, and the
> benchmark asserts that rather than assuming it. A stock `cassandra:5.0` image does not — see
> [With your own Cassandra](with-your-own-cassandra.md).

### By hand

The primitive is public, so you can truncate without going through a load at all:

```java
CqlOperations.truncateKeyspace(session, "mykeyspace");
CqlOperations.truncateKeyspace(session, "mykeyspace", "reference_data");
```

The second form truncates every table *except* the ones named, which is how you keep pre-seeded
reference data. With the embedded server,
`EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace", "reference_data")` is the
same thing against the shared session — see [Embedded server](embedded-server.md).
