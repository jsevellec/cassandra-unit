# Datasets

A dataset is a **CQL script**. CassandraUnit reads it, splits it into statements, and executes
them through the DataStax driver against the embedded node.

**CQL is the only supported format.** Earlier versions advertised XML, JSON and YAML datasets;
those loaders were removed years ago, and 5.0.0 removes the last traces of them — the
`DataSetFileExtensionEnum` type and the `@CassandraDataSet(type = ...)` attribute are gone. If you
have XML/JSON/YAML datasets, convert them to CQL.

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

## Truncating instead of dropping

Dropping and recreating a keyspace per test is simple but not free. To keep the schema and empty
only the rows:

```java
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace");
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace", "reference_data");
```

The second form truncates every table *except* the ones named. See [Embedded
server](embedded-server.md).
