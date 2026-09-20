# Changelog

## Unreleased

## 5.4.0 (2026-09-20)

### Changed

- **The `jamm` memory-meter agent is no longer needed.** The surefire setup consumers copy loses
  three of its lines: `-javaagent:${com.github.jbellis:jamm:jar}`,
  `-Djdk.attach.allowAttachSelf=true`, and the whole `maven-dependency-plugin` `properties`
  execution that existed only to resolve that path. What remains is one plugin and the JPMS
  `--add-exports` / `--add-opens` set.

  Cassandra never required the agent unconditionally: `ObjectSizes` builds its meter as
  `MemoryMeter.builder().withGuessing(INSTRUMENTATION_AND_SPECIFICATION, UNSAFE)`, a fallback chain,
  so with no agent loaded it measures through `Unsafe` instead. Object sizes are then derived from
  field offsets rather than from instrumentation, which shifts memtable accounting slightly in a
  node holding a test fixture. The full suite passes either way.

  This removes the most error-prone part of the setup. An unresolved `${...}` agent path killed the
  fork before surefire could report anything, surfacing only as `The forked VM terminated without
  properly saying goodbye` — the single most common first-run failure.

  **Projects on 5.3.0 and earlier must keep all three.** Their instructions are unchanged.

## 5.3.0 (2026-09-20)

### Added

- **Spring Boot tests work with no wiring.** Annotate a `@SpringBootTest` class with
  `@EmbeddedCassandra` and the node's address is published into the test's `Environment` as
  `spring.cassandra.contact-points`, `spring.cassandra.port` and
  `spring.cassandra.local-datacenter` before the context refreshes, so Boot's auto-configured
  `CqlSession` connects to the embedded node instead of to the driver default of 9042.

  This is what issue #217 asked for in 2017. It matters most with
  `@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")`, where the port is chosen at
  startup and so cannot be written into a properties file at all.

  The mechanism is a `ContextCustomizerFactory` registered in `META-INF/spring.factories`. It
  returns `null` for any class without the annotation — contributing nothing, not even a context
  cache key entry — and names no type from `cassandra-unit`, so a project using this module for one
  test class does not load the embedded server for the rest.

- **`@EmbeddedCassandra(exposeProperties = false)`** turns that off, for tests that set
  `spring.cassandra.*` themselves and want their own values to win.

- **`org.cassandraunit.SpringSessions`**, in `cassandra-unit-dataset`, loads fixtures through a
  `CqlSession` bean from the test's `ApplicationContext`:

  ```java
  CqlDataSetExtension.using(SpringSessions.fromApplicationContext())
  ```

  This is the path for Spring Boot against a Cassandra that is not the embedded one — a
  Testcontainers container, a shared cluster, Astra. It lives in the driver-only artifact, so it has
  no JDK ceiling and needs no surefire configuration. `spring-test` and `spring-context` are
  **optional** dependencies, so they reach you only if you already have Spring.

  The `CqlDataSetExtension.using(Function<ExtensionContext, CqlSession>)` overload it builds on
  existed in 5.2.0 but was documented nowhere.

### Changed

- **JUnit 6, Spring 7 and Spring Boot 4.** JUnit Jupiter moves to **6.1.3**, Spring to **7.0.9** and
  the Boot test coverage to **4.1.1**. These are one change, not three: Spring 7's `SpringExtension`
  calls `ExtensionContext.Store.computeIfAbsent(...)`, which JUnit 5 spells
  `getOrComputeIfAbsent(...)`, so Spring 7 cannot run on JUnit 5 at all.

  **This is breaking for consumers.** `CqlDataSetExtension`, `CassandraUnitExtension` and
  `ExpectedCassandraDataSetExtension` are JUnit Jupiter extensions, and `junit-jupiter-api` is an
  optional compile dependency of the artifacts that publish them — so a project using any of them
  must move to JUnit 6 as well. The JUnit 4 `@Rule` integration is unaffected and still runs through
  the vintage engine, now 6.1.3.

  Boot 4 also split auto-configuration into one module per technology: a Boot 4 application needs
  `spring-boot-cassandra` for `CassandraAutoConfiguration`, where Boot 3 had it in
  `spring-boot-autoconfigure`.

- The published `spring.cassandra.*` property source is added **first**, so it takes precedence over
  a `spring.cassandra.port` set in a configuration file or `@TestPropertySource`. If you previously
  bridged this gap by hand, your literal is now overridden by the port the node is really listening
  on. Use `exposeProperties = false` to keep your own values.

### Documentation

- `docs/spring.md` gains a real Spring Boot section — the previous one was three sentences saying
  there was no Boot support — covering both paths, the published properties, the precedence rule,
  why `spring.cassandra.keyspace-name` must not be set, and the lifecycle gotchas
  (`closingSession()`, `@DirtiesContext`, competing `ParameterResolver`s).
- `docs/with-your-own-cassandra.md` said "**No Spring integration**" about the artifact that now
  carries `SpringSessions`. Corrected, with an example.

## 5.2.0 (2026-09-19)

### Added

- **`CqlAssertions` - fluent, AssertJ-native assertions on what the database actually holds.** The
  companion to `@ExpectedCassandraDataSet`, which is deliberately file-first: until now there was no
  way to assert "this table holds three rows" or "this row's label is `one`" without writing a
  fixture file for it, which is out of proportion for a single value.

  ```java
  import static org.cassandraunit.assertion.CqlAssertions.assertThat;

  assertThat(session).keyspace("mykeyspace")
          .table("widget")
              .hasRowCount(3)
              .row("id", widgetId)
                  .hasValue("label", "one")
                  .hasNull("created");

  // or against something you already fetched
  assertThat(row).hasValue("label", "one");
  assertThat(resultSet).hasSize(3);
  ```

  Every assert type extends AssertJ's `AbstractAssert`, so `as()`, `describedAs()`, `satisfies()`
  and `SoftAssertions` work. The entry points take driver types, so this can be statically imported
  alongside `org.assertj.core.api.Assertions.*` without ambiguity.

  **It agrees with the dataset comparison about what equal means**, because both go through the same
  `ValueEquality`: a `set` column reading back empty rather than null, `1.50` against `1.5`, a
  `ByteBuffer` that must not be consumed by being read. Expected values are coerced through the same
  converter a row dataset uses, so `hasValue("quantity", 42)` works against a `bigint` and
  `hasValue("id", "1690e8da-…")` against a `uuid`. A test asserts that the two paths pass and fail
  together on the same data.

  Failure and error keep the 5.1.0 rule: a value that does not match is an `AssertionError`, while a
  column, table or keyspace that does not *exist* is a `ParseException`, because that means the test
  is wrong rather than the code under test. Addressing a compound-key table by one column is
  refused rather than silently matching a partial key.

  See [Asserting in code](docs/assertions-fluent.md).

- **A Java builder, as a sixth dataset format.** A dataset had to be a file - YAML, JSON, XML, CSV
  or CQL. For a handful of rows that is a file's worth of ceremony, and it puts the fixture
  somewhere other than the test that depends on it.

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

  **It is not a second way of loading rows.** `build()` returns an ordinary `RowsCQLDataSet`, so
  the column types still come from the live schema, the values still go through the same converter,
  and the keyspace handling, isolation modes, extensions and JUnit 4 rule are untouched. A test
  builds `rows/assertion-data.yaml` in code and checks each direction against the other, so the
  builder and the parsers cannot drift.

  Because it returns a row dataset, the same object also states the expectation -
  `ExpectedDataSetFactory.of(fixtures, "mykeyspace")` - with no new assertion API.

  Values are real Java objects: a `UUID`, an `Instant`, a `Set<String>` are passed through when the
  column's codec accepts them, and their written forms still convert. A `UdtValue` or `TupleValue`
  of the column's exact type is passed through too - no file can express one, so the converter
  previously had no reason to accept it. `null` is an explicit null, as
  in a file. `row(Map)` covers a row whose columns differ from the rest, and returning to a table
  appends rows while keeping its columns. Keyspace creation and deletion default to **off**, unlike
  `fromClassPath`: a builder describes rows and never schema.

  See [Datasets](docs/datasets.md#in-java-with-no-file).

### Dependencies

- `assertj-core` is now a dependency of `cassandra-unit-dataset`, declared
  `<optional>true</optional>` - the same treatment `junit`, `junit-jupiter-api` and
  `jackson-dataformat-csv` already get. **An optional dependency is never transitive, so it reaches
  no consumer who does not ask for it**, and the resolved dependency set of `cassandra-unit` is
  unchanged. Only `CqlAssertions` and the assert types around it touch AssertJ; a consumer who never
  imports them is unaffected, and one who does but has no `assertj-core` gets a
  `NoClassDefFoundError` on `AbstractAssert`.

  This is a compile-surface commitment where before it was a test-only one: **assertj-core 3.x is
  the supported baseline**, and a consumer on a different major would get a linkage error.

## 5.1.0 (2026-09-19)

### Added

- **`@ExpectedCassandraDataSet` — asserting what the database holds.** Closes
  [#45](https://github.com/jsevellec/cassandra-unit/issues/45), opened in 2012 and abandoned on the
  `issue45_assertions` branch the same year.

  The project has described itself as "DBUnit for Cassandra" since 2010 while only ever
  implementing the load direction. A dataset can now state what a table should hold *after* a test,
  and it can be literally the same file that set it up — the two directions follow identical rules:
  a column absent from a row is not asserted, a column present with `null` asserts the column reads
  back as null.

  ```java
  @Test
  @ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace")
  void shipping_a_widget_marks_it_dispatched() {
      service.ship(widgetId);
  }
  ```

  Nothing else in the Java/Cassandra ecosystem does this: Testcontainers' Cassandra module has one
  `withInitScript`, and embedded-cassandra has `CqlScript`.

  The comparison rules are Cassandra's, not SQL's, and are documented in
  [docs/assertions.md](docs/assertions.md):

  - **Rows are matched on the primary key**, so a wrong value reports as one column on the right row
    rather than a missing row plus an unexpected one.
  - **Order across partitions is never compared** — an unrestricted `SELECT` returns partition-token
    order, which is stable but arbitrary. Order *within* a partition can be asserted with
    `checkingClusteringOrder()`.
  - **Strict by default.** A named table must hold exactly the rows listed. Cassandra is upsert-only
    with no unique constraints, so a write landing under the wrong key produces an extra row that a
    contains-style assertion would never catch.
  - **Never `SELECT *`, never `ALLOW FILTERING`.** Only the primary key and the columns the file
    mentions are selected, and the statement is echoed in the failure message.
  - Collection `null` and empty are the same value (the driver's codecs never decode a collection to
    null); `BigDecimal` compares with `compareTo`, since `1.5` and `1.50` are the same decimal.
  - Counters are assertable although not loadable — they read back as a `bigint`.

  A mismatch throws `DataSetMismatchError`, an `AssertionError`, reported as a test **failure**. A
  broken expectation — unknown column, a row missing part of its key, a `.cql` file — throws
  `ParseException`, reported as an **error**. One means the code is wrong, the other means the test
  is.

  Integrations, all reading the one annotation rather than a copy per module: `CassandraUnitExtension`
  picks it up with no extra wiring, `ExpectedCassandraDataSetExtension` does the same against a
  session you supply, `ExpectedCassandraDataSetRule` covers JUnit 4 (a sibling rule, because
  `ExternalResource` cannot see a method annotation), and the Spring listeners check it *before*
  `cleanServer()` drops the keyspace.

- **`CQLDataSetFactory.rowsFromClassPath` / `rowsFromFile`**, returning the concrete
  `RowsCQLDataSet` for callers needing `parse()`. Keyspace creation and deletion are forced off: an
  expected dataset must not be able to drop the keyspace it is about to inspect.

- **`RowsCQLDataSet.describe()`**, the dataset's origin, for error messages.

- **`cassandra-unit-dataset`, the fixture layer without the embedded server.** Closes
  [#243](https://github.com/jsevellec/cassandra-unit/issues/243), open since 2017.

  The dataset code never needed the embedded server: nothing under `org.cassandraunit.dataset`
  references `org.apache.cassandra.*`, and `CQLDataLoader` has always taken a `CqlSession` from its
  caller. But it shipped inside an artifact depending on `cassandra-all`, so using the loader meant
  pulling in a Cassandra distribution, configuring the 25-line JPMS `argLine` and the jamm
  javaagent, and accepting JDK 17 forever.

  The new artifact carries the loader, the dataset types and the parsers, and depends on
  `java-driver-core`, `jackson-databind`, `snakeyaml` and `slf4j-api`. It works against any
  `CqlSession` - a Testcontainers container, a local node, ScyllaDB, Astra - needs no surefire
  configuration, and has no JDK ceiling.

  ```xml
  <dependency>
      <groupId>org.cassandraunit</groupId>
      <artifactId>cassandra-unit-dataset</artifactId>
      <version>5.1.0</version>
      <scope>test</scope>
  </dependency>
  ```

  **Nothing changes for existing users.** Package and class names are unchanged, and
  `cassandra-unit` depends on the new module at compile scope, so every import resolves as before.
  The set of artifacts a `cassandra-unit` consumer resolves is what 5.0.0 resolves today, plus the
  row-dataset additions listed above and `cassandra-unit-dataset` itself. `jackson-dataformat-csv`
  moves to the new pom, where the CSV parser now lives; it was optional before and is optional now,
  so it reaches no consumer either way.

- **`CqlDataSetExtension`**, a JUnit 5 extension that loads datasets through a session you supply
  rather than starting one. See [Using your own Cassandra](docs/with-your-own-cassandra.md).

  The session comes from a lazily-called supplier rather than being passed in, and that is
  deliberate: Jupiter runs declaratively-registered `beforeAll` callbacks - which is how
  `@Testcontainers` registers - before `@RegisterExtension` ones, and `@BeforeAll` *methods* after
  all callbacks. A `static CqlSession` populated in a `@BeforeAll` method is still null when the
  extension starts. The extension never closes a session it did not create; `closingSession()`
  opts in.

- **`CQLDataLoader.Isolation`** and **`load(dataSet, isolation)`** - how a load clears what the
  last test left behind. `DATASET` (the default) honours the dataset's own creation and deletion
  flags, which is what every earlier release did; `TRUNCATE` keeps the keyspace and its schema and
  empties every table instead; `NONE` clears nothing. Exposed as
  `CqlDataSetExtension.Builder.isolation(...)` and `CassandraUnitExtension.withIsolation(...)`.
  Closes [#306](https://github.com/jsevellec/cassandra-unit/issues/306) and
  [#216](https://github.com/jsevellec/cassandra-unit/issues/216).

  The mode is on the loader rather than on the dataset because a `.cql` script interleaves DDL and
  DML: nothing reading a dataset can tell which of its statements are schema, so a decorator would
  have to guess.

  **`TRUNCATE` is much faster, and it was worth measuring rather than assuming.** On the embedded
  server, median of ten cycles with the schema replayed in the `DATASET` arm - which is the work it
  actually does - and twenty rows per table put back before each timed cycle:

  | tables | `DATASET` | `TRUNCATE` |
  |---|---|---|
  | 2 | 940 ms | 1.6 ms |
  | 10 | 1022 ms | 4.1 ms |
  | 50 | 1640 ms | 10.7 ms |

  It stays opt-in anyway, because it is not a drop-in: a dataset that creates its own schema
  breaks under it. Note that `auto_snapshot` is not a reason to choose between the modes - it gates
  the snapshot taken when a table is truncated *and* the one taken when a table is dropped, so on a
  stock `cassandra:5.0` image both modes write a snapshot per table per test. There is no property
  or `nodetool` command for it, so overriding it means supplying a `cassandra.yaml`. See
  [Isolation](docs/datasets.md#isolation-truncating-instead-of-dropping).

- **`CQLDataLoader.loadIfKeyspaceAbsent(dataSet)`** - loads only if the keyspace is not already
  there, and reports whether it did. Backs the extension's `schemaOnce`. It asks
  `system_schema.keyspaces` rather than remembering in a field, because several test classes
  routinely share one JVM and one session.

- **`CqlOperations.truncateKeyspace(session, keyspace, excludedTables...)`** and
  **`CqlOperations.quote(identifier)`**, promoted from private methods on
  `EmbeddedCassandraServerHelper`. Both are pure driver code that was reachable only by starting an
  embedded node. `cleanDataEmbeddedCassandra` now delegates to the first; its behaviour is
  unchanged.

  Note that `TRUNCATE` snapshots first unless the server sets `auto_snapshot: false`. The yaml
  files shipped here do; a stock `cassandra:5.0` image does not.

- **`RowValueConverter`** and **`TableNames`**, extracted from the package-private `RowBinder`. The
  conversion ladder was already a pure function of `(DataType, value, CodecRegistry)` but was
  reachable only from the write path. Extracting it makes it testable with no node running, and
  gives a future read-back path the same ladder to use.

- `Automatic-Module-Name` manifest entries on both jars.

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

- The JDK requirement is now per-artifact. `cassandra-unit` is still **JDK 17 and nothing else** -
  Cassandra's `ThreadAwareSecurityManager` calls `System::setSecurityManager`, so 24+ can never
  work. `cassandra-unit-dataset` needs **17 or later with no upper bound**.
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

## 5.0.0 (2026-09-19)

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
