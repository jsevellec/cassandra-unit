# Changelog

## 5.1.0 (unreleased)

### Added

- **Row datasets in YAML, JSON, XML and CSV.** A dataset may now be a declarative list of rows
  instead of a CQL script. The format is chosen by file extension:

  ```java
  new CassandraUnitExtension(
      CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml"));
  ```

  A row dataset describes **data only** — the schema stays in a `.cql` script. That split is what
  makes the conversion correct: the column types are already in the database when rows load, so
  they are read from there rather than re-declared in the fixture, and the driver's own codecs do
  the conversion. `uuid`, `timestamp`, `blob`, collections and UDTs work without this project
  owning a type system, and a `text` column holding `"1"` stays the string `"1"` instead of
  becoming `VALUES (1)` — the defining bug of the 4.x formats.

  **These are not the 4.x XML/JSON/YAML formats.** Those described Thrift column families and their
  loaders were deleted in 2016; an old 4.x dataset will not load. See [Datasets](docs/datasets.md).

- **`SessionAwareDataSet`**, a sub-interface of `CQLDataSet` for a dataset that needs the live
  session to load itself. `CQLDataLoader` dispatches on it. Purely additive — every existing
  `CQLDataSet` implementation, including third-party ones, is unaffected.
- **`CQLDataSetFactory`**, which builds the right dataset for a location, and
  `CQLDataSetFactory.fromClassPathAll(keyspace, locations...)`, which loads several datasets as one
  with the keyspace dropped and created once for the chain. The latter is what makes a row dataset
  usable from the JUnit rule and the JUnit 5 extension, since both take exactly one dataset.
- **`DataSetSource`**, separating where a dataset's bytes come from (classpath, file) from what
  format they are in. Without it, every new format would have needed a class per source.
- `@CassandraDataSet` accepts the new formats with no new attribute, and its
  convention-over-configuration lookup now tries `<TestClassName>-dataset.<ext>` for each supported
  extension, `cql` first.

### Changed

- Generated row inserts are **fully qualified with the keyspace**. A row dataset normally loads
  with `keyspaceCreation=false`, which means no `USE` is issued before it and the current keyspace
  would otherwise be whatever the previous load left behind (#160). A CQL script can work around
  that by writing `keyspace.table` itself; a row dataset cannot.
- `jackson-databind` is now declared explicitly in `cassandra-unit`. It was already a hard,
  non-optional dependency of `java-driver-core`, so nothing new reaches your classpath; the pom now
  admits what the code compiles against.

### Dependencies

Three of the four new formats add **nothing**: YAML uses the `snakeyaml` already present for
`cassandra-all`, JSON the `jackson-databind` already present for the driver, XML the JDK's own
parser. Only **CSV** needs a new artifact, `jackson-dataformat-csv`, and it is declared
`<optional>true</optional>` — its own dependencies are `jackson-databind` and
`jackson-annotations`, both already there, so it is one jar with no new transitive tree. Loading a
`.csv` dataset without it fails at dataset construction with a message naming the coordinates.

This restraint is deliberate. `cassandra-unit-shaded` was deleted in 5.0.0 because of recurring
dependency-clash reports (#307, #314, #336, #248, #202); a feature that quietly added four parser
libraries to every consumer's classpath would have reopened exactly that. In particular
`jackson-dataformat-yaml` is **not** used: it requires a newer `snakeyaml` than Cassandra tolerates
(see CASSANDRA-20848), and cassandra-unit runs Cassandra's own `YamlConfigurationLoader` in the
test JVM — bumping it would break the embedded server this library exists to start.

## 5.0.0 (unreleased)

First release since 4.3.1.0 (January 2020). The version now tracks the embedded Apache
Cassandra major rather than the driver version.

This release deliberately breaks compatibility with 4.3.1.0.

### License

- **The license changed from LGPL-3.0 to MIT.** Previously published 4.x artifacts keep their
  LGPL-3.0 metadata and are unaffected; the change applies from 5.0.0 onward.

### Requirements

- Embedded Cassandra is now **5.0.8** (was 3.11.5).
- **JDK 17 is required, and is the complete supported set.** Cassandra 5.0 removed Java 8 and
  supports only 11 and 17; of those this project targets 17, which `spring-test` 6.2 requires
  anyway. No released Cassandra supports JDK 18-23, and JDK 24+ can never work because
  Cassandra's `ThreadAwareSecurityManager` calls `System::setSecurityManager`, which throws
  there. The build enforces `[17,18)`.
- **Consumers must add JVM flags to surefire** — the JPMS `--add-exports`/`--add-opens` set from
  Cassandra's `conf/jvm17-server.options`, plus the `jamm` agent. An embedded Cassandra cannot
  start without them. See the README for a copy-pasteable block. This is the largest
  practical change for existing users.

### Removed

- **The `cassandra-unit-shaded` artifact.** It existed to shade around old, vulnerable copies of
  guava, netty and jackson; the Cassandra 5.0 upgrade removes those versions, so it has no
  remaining purpose. It was also the source of the recurring dependency-clash reports (#307,
  #314, #336, #248, #202), and it shipped CLI classes while excluding `commons-cli`, so those
  classes could not load. If you depended on it, depend on `cassandra-unit` and declare your own
  exclusions.
- **The `cu-loader` and `cu-starter` command line tools**, their scripts and the assembly.
  `cu-starter` never worked: it read a `samples/cassandra.yaml` the assembly did not ship,
  swallowed the resulting exception and exited 0. `cu-starter.bat` named a class that does not
  exist. Neither had tests.
- `EmbeddedCassandraServerHelper.getRpcPort()` — `DatabaseDescriptor.getRpcPort()` was removed
  in Cassandra 4.0 along with Thrift.
- `@CassandraDataSet(type = ...)` and `DataSetFileExtensionEnum`. The enum still advertised
  `xml`, `json` and `yaml` although only CQL datasets have been loadable for years.
- `GenericType`, `GenericTypeEnum` and `CassandraUnitException`, which were referenced nowhere.
- The `libthrift` and `jna` dependency pins. Thrift was removed from Cassandra in 4.0 and
  `cassandra-thrift` is not published past the 3.11 line.
- The bundled log4j configuration and its wiring. log4j was excluded from the build and
  Cassandra uses logback, so it could never take effect (#319).

### Added

- **`CassandraUnitExtension`, a JUnit 5 extension**, alongside the existing JUnit 4 `@Rule`.
  Register it with `@RegisterExtension`; a `CqlSession` test parameter is injected. This is the
  entry point Jupiter-based suites, Spring Boot 3+ included, previously did not have (#293).
- GitHub Actions CI on JDK 17. There was no CI of any kind before.
- Dependabot configuration.

### Changed

- Driver coordinates are now **`org.apache.cassandra:java-driver-core` 4.19.3**. The DataStax
  coordinates (`com.datastax.oss`) are frozen at 4.17.0. Java packages are unchanged
  (`com.datastax.oss.driver.*`), so no imports change.
- **The driver is a required dependency instead of `<optional>true</optional>`.** `CqlSession` was
  always imported unconditionally, so declaring it optional is what produced the recurring
  `NoClassDefFoundError` reports (#276, #304).
- **Only one CQL driver reaches your classpath now.** `cassandra-all` 5.0.8 pulls in the legacy
  shaded `cassandra-driver-core` 3.12.1, which exposes the old `com.datastax.driver.core.*` API.
  Its coordinates differ from `java-driver-core`, so Maven never mediated between the two and
  both landed on the consumer's compile classpath — two driver APIs, no warning. It is now
  excluded.
- **JUnit 4 and Hamcrest are no longer compile-scope**, so this library no longer forces JUnit 4
  onto every consumer's classpath. Spring moved from 4.0.2.RELEASE compile-scope to 6.2.19
  `provided`. Declare whichever test framework and Spring version you use.
- `cu-cassandra.yaml` is rewritten from 589 lines to about 65, derived from Cassandra's own
  in-JVM test configuration. The old file carried Thrift-era keys (`start_rpc`, `rpc_port`,
  `rpc_server_type`, `request_scheduler`, `index_interval`) that a Cassandra 4.x+
  `YamlConfigurationLoader` rejects outright.
- Publishing moves to the Central Publisher Portal. The previous configuration pointed at
  `oss.sonatype.org`, which no longer exists — the project had no working release path.
- **Version scheme is now `<cassandra-major>.<cassandra-unit-minor>.<cassandra-unit-patch>`**, and
  the build enforces it. The major is the embedded Cassandra major; minor and patch are
  cassandra-unit's own, by semver; the driver version never appears in the number. This reverses
  the 2016-2020 scheme, under which `4.3.1.0` meant *driver* 4.3.1 on Cassandra 3.11.5 — and
  under which `3.11.2.0` and `3.7.1.0` shipped identical code seventeen minutes apart, the second
  one numbered lower. The README now carries a [compatibility
  matrix](README.md#version-compatibility) for every release back to 2.2.2.1, and CONTRIBUTING.md
  states the bump rules.
- The compile/runtime dependency set carries **no known OSV advisories**, down from roughly 60
  across 15 artifacts in 4.3.1.0. Most of that came free with the Cassandra 5.0 upgrade (guava
  18 to 32.0.1-jre, snakeyaml 1.11 to 2.1, commons-lang3 3.1 to 3.18.0, and the removal of
  libthrift, jna 4.1.0, ant, httpclient, hibernate-validator and the codehaus jackson entirely).
  Three artifacts needed pinning ahead of what Cassandra itself resolves - netty to
  4.1.137.Final, jackson to 2.22.1 and lz4-java to 1.11.2 - each verified against the full test
  suite. See SECURITY.md; they should be dropped when Cassandra catches up.

### Fixed

- **`tmpDir` now actually relocates Cassandra's storage.** The yaml was previously adapted by a
  regex matching only `^([a-z_]+)_port:` lines, so the data, commitlog, saved caches, hints and
  cdc directories kept the hardcoded `target/embeddedCassandra/*` values and `tmpDir` moved
  nothing but the copy of the yaml. Configuration is now applied by mutating Cassandra's `Config`
  object via `daemonInitialization(Supplier<Config>)` (#265, #316).
- **`readTimeoutMillis` now reaches the session.** Three `CassandraCQLUnit` constructors accepted
  it, stored it, and never used it — the session was always built with a request timeout of zero.
- Keyspaces whose names are not lower-case can now be dropped. Identifiers are quoted via
  `CqlIdentifier` instead of being concatenated into CQL raw (#222).
- `system_views` and `system_virtual_schema` are recognised as system keyspaces (#321, #329).
- `cleanEmbeddedCassandra()` and `cleanDataEmbeddedCassandra()` read the keyspace and table lists
  from `system_schema` instead of from driver metadata, which by default excludes system
  keyspaces, and no longer call `Optional.get()` on a keyspace that may legitimately be absent.
- `ssl_storage_port` is randomised by the random-port configuration. It was left hardcoded at
  7011, so that configuration could still collide.
- `cleanup()` no longer throws when a storage directory is absent, which could fail the very
  startup a fresh `tmpDir` was meant to enable (#316).
- The JVM-global daemon and session fields are `volatile` and both `startEmbeddedCassandra`
  entry points are synchronized; previously only session creation was.
- Dataset files are read as UTF-8 rather than in the platform default encoding (#144).

### Testing

- **Nine test classes were never executed by any build.** Surefire's default includes are
  `Test*`, `*Test`, `*Tests` and `*TestCase`; every `CQLDataLoadTestWith*` class ends in
  something else, so the entire dataset-loading surface — the purpose of this library — was
  silently skipped. They now run.
- All `@Ignore`s are gone. The three disabled classes were blocked only by surefire fork reuse,
  not by any code defect; `reuseForks=false` was the whole fix.
- The keyspace-drop path has real coverage for the first time. Its previous test started no
  server and asserted nothing.

## 4.3.1.0 and earlier

See the git history and the
[GitHub releases](https://github.com/jsevellec/cassandra-unit/releases).
