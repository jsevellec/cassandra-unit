# Datasets

A dataset is a file CassandraUnit loads into the embedded node before your test runs. There are two
kinds:

- a **CQL script** (`.cql`) — read, split into statements, and executed through the driver. This is
  the original format and the one that creates your schema.
- a **row dataset** (`.yaml`, `.yml`, `.json`, `.xml`, `.csv`) — a declarative list of rows, loaded
  against a schema that already exists. See [Row datasets](#row-datasets).

The format comes from the file extension; there is no `type` attribute to keep in sync.

> **These are not the 4.x XML/JSON/YAML formats.** Those described Thrift column families
> (`columnFamilies`, `superColumns`, `comparatorType`) and their loaders were deleted in 2016. The
> formats below describe CQL tables and share nothing with them. An old 4.x dataset will not load.

## A minimal dataset

`src/test/resources/cql/simple.cql`:

```sql
CREATE TABLE widget (id int PRIMARY KEY, label text);
INSERT INTO widget (id, label) VALUES (1, 'hello');
```

Note what is *absent*: no `CREATE KEYSPACE`, no `USE`. By default CassandraUnit drops and recreates
the keyspace and switches to it before running your script, so the script only describes tables and
rows. See [Keyspace handling](#keyspace-handling) to change that.

## Loading a dataset

```java
new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace")   // from the classpath
new FileCQLDataSet("/tmp/simple.cql", "mykeyspace")       // from the filesystem
```

`ClassPathCQLDataSet` is what you want in almost every case. The location is resolved as a
classpath resource, so `cql/simple.cql` means `src/test/resources/cql/simple.cql`.

Both classes accept the same argument shapes:

| Constructor | Meaning |
|---|---|
| `(String location)` | default keyspace `cassandraunitkeyspace`, dropped and created |
| `(String location, String keyspaceName)` | named keyspace, dropped and created |
| `(String location, boolean keyspaceCreation)` | control creation only |
| `(String location, boolean keyspaceCreation, boolean keyspaceDeletion)` | control both |
| `(String location, boolean keyspaceCreation, boolean keyspaceDeletion, String keyspaceName)` | all four |

A missing or unreadable dataset raises `ParseException` from the constructor, not later at load
time, so a typo in the path fails fast.

## Statement parsing rules

The parser is deliberately small — it is a lexer that strips comments and splits on semicolons, not
a CQL grammar. The exact rules, from
[`SimpleCQLLexer`](../cassandra-unit/src/main/java/org/cassandraunit/dataset/cql/SimpleCQLLexer.java):

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

**Files are read as UTF-8** as of 5.0.0. Earlier versions used the platform default encoding, so
the same dataset could parse differently on different machines.

## Keyspace handling

Two independent flags control what happens to the keyspace before your script runs:

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
your own `CREATE KEYSPACE` as the first statement of the script.

### Keeping data across tests in a class

```java
new ClassPathCQLDataSet("cql/simple.cql", false, false, "mykeyspace")
```

Creation and deletion both off: the script runs against whatever is already there. Useful for a
schema-once, insert-per-test arrangement — but then the tests share state, and their order starts
to matter.

### Loading several datasets

```java
CQLDataLoader loader = new CQLDataLoader(EmbeddedCassandraServerHelper.getSession());
loader.load(new ClassPathCQLDataSet("cql/schema.cql", true, true, "mykeyspace"));
loader.load(new ClassPathCQLDataSet("cql/data.cql", false, false, "mykeyspace"));
```

The first load creates the keyspace, the second must not drop it. Getting this backwards is the
usual cause of "my data disappeared" — the second dataset dropped the keyspace the first had
populated.

Note *why* the second load lands in the right keyspace: `CQLDataLoader.load` issues `USE` at the
end, so the first load leaves the session pointing at `mykeyspace` and the second inherits it. It
follows that a second dataset naming a **different** keyspace, with `keyspaceCreation` off, would
run its statements against whatever keyspace was current — see
[issue #160](https://github.com/jsevellec/cassandra-unit/issues/160). Qualify table names as
`keyspace.table` in any dataset that does not create its own keyspace.

## Row datasets

A row dataset describes **data only**. The schema stays in a `.cql` script.

That split is not arbitrary. Writing rows as CQL `INSERT` statements means hand-formatting every
value into CQL literal syntax, and a mistake fails at load time with a server-side error. Writing
them declaratively means the loader has to know each column's type — and because the schema is
already in the database by the time rows load, it reads the types from **there** rather than making
you re-declare them. `uuid`, `timestamp`, `blob`, collections and UDTs are all converted by the
driver's own codecs.

The practical consequence is the one that used to be a bug: a `text` column holding `"1"` stays the
string `"1"`. It does not become the number 1.

### Loading one

A row dataset always needs its schema first, so it comes in a pair:

```java
new CassandraUnitExtension(
    CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml"));
```

`fromClassPathAll` drops and creates the keyspace **once** for the whole chain, then loads each file
in order. Use it whenever you load more than one file, and always for a row dataset — the JUnit rule
and the JUnit 5 extension take exactly one dataset, so without it a row dataset has no way to get
its schema loaded first.

The loader form, if you are not using the rule or the extension:

```java
CQLDataLoader loader = new CQLDataLoader(EmbeddedCassandraServerHelper.getSession());
loader.load(new ClassPathCQLDataSet("cql/schema.cql", true, true, "mykeyspace"));
loader.load(CQLDataSetFactory.fromClassPath("data/widget.yaml", false, false, "mykeyspace"));
```

Note the `false, false` on the second: **a second dataset that drops the keyspace destroys what the
first one just created.** This is the usual cause of "my data disappeared". `fromClassPathAll` exists
so you do not have to get this right by hand.

`CQLDataSetFactory.fromFile(...)` and `fromClassPath(...)` take the same four argument shapes as the
`ClassPathCQLDataSet` constructors above, and return a plain `ClassPathCQLDataSet` when handed a
`.cql` location — so the factory is safe to use everywhere.

### null, and the absence of a value

Two different things, and the difference is visible in the database:

| In the file | Meaning | Effect |
|---|---|---|
| the column is **absent from that row** | unset | the column is not in the generated `INSERT` at all: nothing is written, and an existing value is left alone |
| the column is present with a **null** value | explicit null | a tombstone is written, erasing any existing value |

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

If the document's root is a bare list of rows rather than a map, the table name is taken from the
filename — `widget.yaml` loads into `widget`.

Two YAML quoting notes, both of which bite:

- **Quote anything whose YAML meaning differs from its CQL meaning.** `0x0a0b` unquoted is the
  *number* 2571 to YAML, so a `blob` needs `"0x0a0b"`. Same for a value that looks like a number
  but belongs in a `text` column.
- An **unquoted timestamp** (`2026-09-19T10:00:00Z`) is read as a date by YAML and works fine; so
  does the quoted form, and so does epoch-millis as a number.

A number written to a `text` column is stringified when that is lossless — `label: 1` gives `"1"` —
but a **decimal** is refused, because `1.10` would silently become `"1.1"`. Quote it.

Duplicate keys in a row are an error, not last-one-wins.

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

Parsed with the JDK's own parser, with DOCTYPE and external entities disabled.

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
legitimately contain the string `NULL`.

CSV is the one format needing a dependency that is not already there. Add it to your test scope:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-csv</artifactId>
  <scope>test</scope>
</dependency>
```

It is declared `optional` by `cassandra-unit`, so you only pay for it if you use it. Loading a
`.csv` dataset without it fails immediately with a message saying exactly this.

### What a row dataset cannot do

Counters (they need `UPDATE ... SET c = c + n`), `USING TTL`, `USING TIMESTAMP`, and schema changes.
Use a `.cql` script for those.

## Truncating instead of dropping

Dropping and recreating a keyspace per test is simple but not free. To keep the schema and empty
only the rows:

```java
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace");
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace", "reference_data");
```

The second form truncates every table *except* the ones named. See [Embedded
server](embedded-server.md).
