# Migrating from 4.x

5.0.0 deliberately breaks compatibility. This page lists what changed and what to do about it;
[CHANGELOG.md](https://github.com/jsevellec/cassandra-unit/blob/main/CHANGELOG.md) has the same ground organised by release.

## The version number changed meaning

From 5.0.0 the version tracks the **embedded Cassandra major**, not the driver version. 5.0.0
embeds Cassandra 5.0.

This is why `4.3.1.0 → 5.0.0` is not the jump it looks like. `4.3.1.0` was named after the
DataStax **driver** 4.3.1; the Cassandra it embedded was **3.11.5**. So you are not moving from
Cassandra 4 to Cassandra 5 — you are moving from 3.11.5 to 5.0.8, which is why this page is as
long as it is.

The [compatibility matrix](https://github.com/jsevellec/cassandra-unit/blob/main/README.md#version-compatibility) lists what every past release
actually embedded.

## Things that will stop your build

### JDK 17 is required

Cassandra 3.11.5 was Java 8 only, which is why 4.3.1.0 could not run on a modern JDK — the single
cause behind most of the JDK, Apple Silicon and "won't start" reports. 5.0.0 embeds Cassandra 5.0.8
and requires **JDK 17**, exactly: no released Cassandra supports 18–23, and JDK 24+ never will,
because Cassandra calls the terminally-deprecated `System::setSecurityManager`.

### You must configure surefire

New, and unavoidable. An embedded Cassandra 5.0 needs the JPMS `--add-exports` / `--add-opens`
flags in the test JVM; skipping them fails the test with an `IllegalAccessException` against a
`sun.*` or `jdk.internal.*` member. The block to copy is in
[the surefire section](getting-started.md#2-configure-surefire). Releases up to 5.3.0 also
required the `jamm` agent and a `maven-dependency-plugin` execution to resolve its path; 5.4.0
dropped both.

This is the biggest practical cost of the upgrade, and it is inherent to running a modern Cassandra
in-process.

### JUnit and Hamcrest no longer arrive with the library

4.3.1.0 declared JUnit 4 and Hamcrest at compile scope, so they landed on every consumer's
classpath. They are now optional. Declare whichever test framework you actually use.

Spring likewise moved from `4.0.2.RELEASE` at compile scope to **6.2.19 `provided`** — your
application picks the Spring version.

## Removed

| Removed | Why | Instead |
|---|---|---|
| `cassandra-unit-shaded` | existed to hide old vulnerable guava/netty/jackson; the Cassandra 5.0 upgrade removes those versions | depend on `cassandra-unit` and declare your own exclusions if you still need them |
| `cu-loader` / `cu-starter` CLI | `cu-starter` never worked — it read a yaml the assembly did not ship, swallowed the error and exited 0 | drive `EmbeddedCassandraServerHelper` from code |
| `EmbeddedCassandraServerHelper.getRpcPort()` | Thrift was removed in Cassandra 4.0, and with it `DatabaseDescriptor.getRpcPort()` | `getNativeTransportPort()` |
| The 4.x XML / JSON / YAML datasets, `DataSetFileExtensionEnum`, `@CassandraDataSet(type = ...)` | the loaders were removed years ago; only the dead enum remained | convert them to CQL. 5.1.0 adds YAML/JSON/XML/CSV **row** datasets, but they are a new design describing CQL tables — a 4.x file describing Thrift column families will not load. See [Datasets](datasets.md) |
| `GenericType`, `GenericTypeEnum`, `CassandraUnitException` | referenced nowhere | — |
| bundled log4j configuration | log4j was excluded from the build and Cassandra uses logback, so it never took effect | configure logback |

## Changed

### Driver coordinates

```xml
<!-- was -->
<groupId>com.datastax.oss</groupId>
<artifactId>java-driver-core</artifactId>

<!-- now -->
<groupId>org.apache.cassandra</groupId>
<artifactId>java-driver-core</artifactId>
```

The DataStax coordinates are frozen at 4.17.0; the driver was donated to Apache. **Java packages
are unchanged** (`com.datastax.oss.driver.*`), so no imports change — only the pom.

The driver is also now a **required** dependency rather than `<optional>true</optional>`, which is
what caused the recurring `NoClassDefFoundError: ...CqlSession` reports.

### Your custom `cassandra.yaml` will not load

Cassandra 4.0+ rejects unknown properties, and the 3.11-era keys are gone: `start_rpc`, `rpc_port`,
`rpc_server_type`, `thrift_*`, `request_scheduler`, `index_interval`. Cassandra 4.1 also renamed
the `*_in_ms` / `*_in_mb` family to typed durations and data sizes (`10s`, `5MiB`).

Start from the shipped `cu-cassandra.yaml`, which is minimal and validated, rather than patching
your old one.

### `tmpDir` now works

It previously relocated nothing but a copy of the yaml — every storage directory kept the hardcoded
`target/embeddedCassandra/*` path. It now moves data, commitlog, saved caches, hints and CDC as
documented. The default also no longer assumes a Maven layout; it resolves against
`java.io.tmpdir`.

If you relied on finding data under `target/embeddedCassandra`, pass an explicit `tmpDir`.

### `readTimeoutMillis` now has an effect

The `CassandraCQLUnit` constructors that take it stored it and never used it — the session was
always built with a request timeout of zero, meaning no timeout. It is now applied. **If you passed
a value expecting it to be ignored, queries can now time out.** There is one session per JVM, so
the first caller wins; a later call warns rather than silently doing nothing.

### `stopEmbeddedCassandra()` no longer kills the JVM

It used to throw `NullPointerException` if nothing was started, and to terminate the JVM outright if
something was — the daemon was created unmanaged and Cassandra's `deactivate()` ends in
`System.exit(0)`. Both fixed. It still does not let you start a different configuration afterwards.

### Cleanup preserves virtual keyspaces

`system_views` and `system_virtual_schema` (Cassandra 4.0) are recognised as system keyspaces, so
`cleanEmbeddedCassandra()` no longer tries to drop them.

### Datasets are read as UTF-8

Previously the platform default encoding, so the same file could parse differently on different
machines.

## New

- **`CassandraUnitExtension`**, a JUnit 5 extension alongside the JUnit 4 `@Rule`. Register it with
  `@RegisterExtension`; a `CqlSession` test parameter is injected. This is the entry point
  Jupiter-based suites, Spring Boot 3+ included, previously did not have.
- **`EmbeddedCassandraServerHelper.setRequestTimeout(Duration)`**.
- **Row datasets in YAML, JSON, XML and CSV** (5.1.0), plus `CQLDataSetFactory` for building a
  dataset from any location and loading several as one, and `CQLDataSetFactory.builder(...)` for
  the same rows written in Java with no file at all (5.2.0). If you came here to convert 4.x XML/JSON/YAML
  datasets, read this first — the formats share a name with the old ones but nothing else, so it is
  a rewrite rather than a port. See [Datasets](datasets.md).

## Suggested order

1. Move to JDK 17 and get the build green without CassandraUnit.
2. Bump the version and add the surefire `argLine`. Expect the first run to fail if you skip it.
3. Fix compile errors: driver coordinates, `getRpcPort()`, `@CassandraDataSet(type = ...)`.
4. Convert any non-CQL datasets to CQL. If they were mostly rows, the 5.1.0 row datasets may be a
   better destination than `INSERT` statements — but they are a different format, not the 4.x one.
5. Replace a custom yaml with the shipped one, or port its keys.
6. Only then adopt the JUnit 5 extension, if you want it — the `@Rule` still works.
