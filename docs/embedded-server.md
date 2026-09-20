# Embedded server

`EmbeddedCassandraServerHelper` is the low-level API. The JUnit rule, the Jupiter extension and the
Spring listeners all drive it, and you can use it directly when none of those fit.

**Dependency:** `cassandra-unit`. The server is the one feature not in `cassandra-unit-dataset`. See [What to declare](README.md#what-to-declare).

## One Cassandra per JVM

**This is a permanent design constraint, not a limitation waiting to be fixed.** Cassandra's
`DatabaseDescriptor`, `Schema` and `StorageService` hold static state that cannot be reset
in-process. Consequences:

- The first `startEmbeddedCassandra(...)` call in a JVM wins. Later calls return immediately
  without doing anything, whatever arguments you pass.
- Asking for a **different configuration file** in the same JVM throws
  `UnsupportedOperationException`.
- `stopEmbeddedCassandra()` does **not** let you start again with a different configuration.

If different test classes need different configurations, give each its own JVM:

```xml
<configuration>
    <reuseForks>false</reuseForks>
</configuration>
```

That costs one startup (~3s) per test class. It is the price of an in-process server; if it is too
high, [Testcontainers](https://java.testcontainers.org/modules/databases/cassandra/) is the
alternative.

## Starting

```java
EmbeddedCassandraServerHelper.startEmbeddedCassandra();
```

Overloads, in increasing order of control:

```java
startEmbeddedCassandra()
startEmbeddedCassandra(long timeoutMillis)
startEmbeddedCassandra(String yamlFile)
startEmbeddedCassandra(String yamlFile, long timeoutMillis)
startEmbeddedCassandra(String yamlFile, String tmpDir)
startEmbeddedCassandra(String yamlFile, String tmpDir, long timeoutMillis)
startEmbeddedCassandra(File yamlFile, long timeoutMillis)
startEmbeddedCassandra(File yamlFile, String tmpDir, long timeoutMillis)
```

`yamlFile` as a `String` is a **classpath resource**; as a `File` it is a path on disk. The default
startup timeout is `DEFAULT_STARTUP_TIMEOUT` (20 000 ms); raise it on a slow or loaded machine.

## Configuration files

Two configurations ship with the library.

| Constant | File | Ports |
|---|---|---|
| `DEFAULT_CASSANDRA_YML_FILE` | `cu-cassandra.yaml` | storage 7010, ssl storage 7011, native transport **9142** |
| `CASSANDRA_RNDPORT_YML_FILE` | `cu-cassandra-rndport.yaml` | all three chosen free at startup |

The ports are deliberately *not* Cassandra's defaults, so an embedded instance does not collide
with a Cassandra you happen to be running locally on 9042.

```java
EmbeddedCassandraServerHelper.startEmbeddedCassandra(
        EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
```

Use the random-port configuration when several builds share a machine. Ask the helper which port
was actually chosen rather than assuming:

```java
int port = EmbeddedCassandraServerHelper.getNativeTransportPort();
String host = EmbeddedCassandraServerHelper.getHost();
String cluster = EmbeddedCassandraServerHelper.getClusterName();
```

### Your own yaml

Pass a classpath resource name or a `File`. Start from the shipped `cu-cassandra.yaml` rather than
from Cassandra's own `conf/cassandra.yaml`: `YamlConfigurationLoader` **rejects unknown
properties**, and every key in the shipped file was checked against
`org.apache.cassandra.config.Config` for the pinned Cassandra version. A yaml copied from an older
Cassandra, or from an older CassandraUnit, will fail to load — pre-4.0 keys such as `start_rpc`,
`rpc_port`, `rpc_server_type`, `request_scheduler` and `index_interval` no longer exist.

## Where data is written

`tmpDir` decides where the node keeps its data, commit log, saved caches, hints and CDC files. It
defaults to `DEFAULT_TMP_DIR`, which resolves against `java.io.tmpdir`.

```java
EmbeddedCassandraServerHelper.startEmbeddedCassandra(
        EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE, "/tmp/my-test-node", 30_000L);
```

The directory is **deleted at the start of every run**, so it does not accumulate between builds
and each run begins from an empty node.

> In 4.3.1.0 and earlier this argument did essentially nothing: the yaml was adapted with a regular
> expression that only matched port lines, so every directory kept the hardcoded
> `target/embeddedCassandra/*` value from the shipped yaml. It works as documented from 5.0.0, and
> the default is no longer a Maven-specific `target/` path.

## The session

```java
CqlSession session = EmbeddedCassandraServerHelper.getSession();
```

One session per JVM, created on first call and closed by a JVM shutdown hook. Do not close it
yourself — every other test in the JVM shares it.

To change the driver request timeout, do it **before** anything creates the session:

```java
EmbeddedCassandraServerHelper.setRequestTimeout(Duration.ofSeconds(30));
```

A call after the session exists logs a warning and has no effect, because there is only one session
to configure. The default is `Duration.ZERO`, which the driver reads as *no timeout*.

### A note on driver metadata

`session.getMetadata().getKeyspaces()` will not list `system`, `system_schema` or any other system
keyspace. That is the driver's documented default — its `reference.conf` ships
`refreshed-keyspaces = [ "!system", "!/^system_.*/", ... ]` — and not a bug in CassandraUnit. Query
`system_schema.keyspaces` if you need the server's own view.

## Cleaning between tests

```java
EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
```

Drops every non-system keyspace. System keyspaces — including the virtual `system_views` and
`system_virtual_schema` introduced in Cassandra 4.0 — are preserved.

```java
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace");
EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("mykeyspace", "reference_data");
```

Truncates the tables in one keyspace, keeping the schema, optionally excluding named tables. Much
faster than dropping and recreating — measurably so, at every keyspace size.

Neither is called for you. **Cleanup is driven by the dataset**, not by the rule or extension — see
[Datasets](datasets.md#keyspace-handling). To get the truncating behaviour without calling anything
yourself, hand the extension an isolation mode:

```java
new CassandraUnitExtension(rows).withIsolation(Isolation.TRUNCATE);
```

See [Isolation](datasets.md#isolation-truncating-instead-of-dropping) for what that changes, the
numbers behind it, and the one configuration setting that reverses them.

## Stopping

```java
EmbeddedCassandraServerHelper.stopEmbeddedCassandra();
```

Deactivates the daemon. You almost never need it: the node dies with the JVM, and with
`reuseForks=false` every test class gets a fresh one anyway. It will **not** let you start another
instance with a different configuration afterwards.

Safe to call when nothing was started. In 4.3.1.0 it threw `NullPointerException` in that case, and
when something *had* been started it terminated the JVM outright — the daemon was created unmanaged
and Cassandra's `deactivate()` ends in `System.exit(0)`. Both are fixed in 5.0.0.

## Direct use, without a rule or extension

```java
class ManualTest {

    @BeforeAll
    static void startCassandra() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(60_000L);
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession())
                .load(CQLDataSetFactory.fromClassPathAll(
                        "mykeyspace", "cql/schema.cql", "data/widget.yaml"));
    }

    @Test
    void queries() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();
        // ...
    }
}
```

`CQLDataSetFactory` picks the format from each extension, and `fromClassPathAll` drops and creates
the keyspace once for the whole chain — see [Datasets](datasets.md). A single `.cql` script works
just as well: `new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace")`.

`AbstractCassandraUnit4CQLTestCase` offers the same thing through inheritance for JUnit 4 — extend
it and implement `getDataSet()`.
