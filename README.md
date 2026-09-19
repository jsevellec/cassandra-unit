WELCOME to CassandraUnit
========================

What is it?
-----------
Like other \*Unit projects, CassandraUnit is a Java utility test tool.
It helps you create your Java Application with [Apache Cassandra](http://cassandra.apache.org) Database backend.
CassandraUnit is for Cassandra what DBUnit is for Relational Databases.

CassandraUnit helps you writing isolated JUnit tests in a Test Driven Development style.

CassandraUnit is two things, and you can take either.

**The fixture loader** (`cassandra-unit-dataset`) turns a YAML, JSON, XML, CSV or CQL file into rows
in a real keyspace, converting every value with the column's actual type read from the live schema —
so a `text` column holding `"1"` stays the string `"1"`, and `uuid`, `timestamp`, `blob`,
collections and UDTs need no hand-formatted CQL literals. It loads through **any `CqlSession` you
hand it**: a Testcontainers container, a local node, ScyllaDB, Astra. No embedded server, no JVM
flags, no JDK ceiling.

**The embedded server** (`cassandra-unit`) starts a real Cassandra node inside your test JVM, for
when you want one and would rather not run Docker. It includes the fixture loader.

| I already have a Cassandra | I want one started for me |
|---|---|
| [Using your own Cassandra](docs/with-your-own-cassandra.md) | [Getting started](docs/getting-started.md) |

```java
@Testcontainers
class WidgetIT {

    @Container
    static final CassandraContainer cassandra =
            new CassandraContainer("cassandra:5.0").withReuse(true);

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(() -> CqlSession.builder()
                    .addContactPoint(cassandra.getContactPoint())
                    .withLocalDatacenter(cassandra.getLocalDatacenter())
                    .build())
            .closingSession()
            .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
            .rowsPerTest(CQLDataSetFactory.fromClassPath(
                    "data/widget.yaml", false, false, "mykeyspace"))
            .build();

    @Test
    void readsTheFixture(CqlSession session) {    // resolved by the extension
        ...
    }
}
```

Testcontainers gives you the node; `withInitScript` is the whole of its data API, one CQL file.
The snippet above is the rest.

**And the other direction.** A dataset can also state what a table should hold *after* a test —
`@ExpectedCassandraDataSet` — which is the half of the DBUnit comparison this project has been
missing since 2010, and which no other Cassandra test library has at all:

```java
@Test
@ExpectedCassandraDataSet(value = "rows/expected-widget.yaml", keyspace = "mykeyspace")
void shipping_a_widget_marks_it_dispatched() {
    service.ship(widgetId);
}
```

Rows are matched on the primary key, order across partitions is never compared, and the failure
report names the row and column that differ. See
[Asserting with a dataset file](docs/assertions.md).

**Or in code, when a file is overkill.** The same comparison, fluent and AssertJ-native, with
nothing to register — it is static methods over a session, so it works under any framework or none:

```java
assertThat(session).keyspace("mykeyspace")
        .table("widget")
            .hasRowCount(3)
            .row("id", widgetId)
                .hasValue("label", "one")
                .hasNull("created");
```

See [Asserting in code](docs/assertions-fluent.md).

Other features:

- Create the schema from a CQL script.
- Integrations for JUnit 4 (`@Rule`), JUnit 5 (`Extension`) and Spring Test.
- `truncateKeyspace` to empty tables between tests without dropping the schema.

Documentation
-------------

Full documentation is in **[docs/](docs/)**, versioned alongside the code:

- [Using your own Cassandra](docs/with-your-own-cassandra.md) — `cassandra-unit-dataset` against a session you supply
- [Getting started](docs/getting-started.md) — the embedded server: dependency, the mandatory surefire setup, a first test
- [Datasets](docs/datasets.md) — `.cql` scripts and YAML/JSON/XML/CSV row datasets, keyspace create/drop control
- [Asserting with a dataset file](docs/assertions.md) — `@ExpectedCassandraDataSet`, and the comparison rules Cassandra forces
- [Asserting in code](docs/assertions-fluent.md) — the fluent `CqlAssertions` API, for a single value or row count
- [Embedded server](docs/embedded-server.md) — the `EmbeddedCassandraServerHelper` API
- [Spring integration](docs/spring.md) — the annotations and listeners
- [Troubleshooting](docs/troubleshooting.md) — failure modes whose messages hide the cause
- [Migrating from 4.x](docs/migrating-from-4.md) — everything removed or changed in 5.0.0

The old [project wiki](https://github.com/jsevellec/cassandra-unit/wiki) is **retired**. It had
drifted to the point of documenting classes and annotation attributes that never existed; its pages
now point here.

Requirements
------------

The two artifacts have different requirements, and the difference is the main reason to prefer one.

| | `cassandra-unit-dataset` | `cassandra-unit` |
|---|---|---|
| Apache Cassandra | **none** — you supply the session | embedded, pulled in transitively |
| **JDK** | **17 or later**, no upper bound | **17 — nothing else** |
| Surefire `argLine` | not needed | **mandatory**, see [Setup](#setup) |
| Maven | 3.9+ | 3.9+ |

`cassandra-unit`'s JDK row is not a recommendation, it is the whole supported set:

- Cassandra 5.0 removed Java 8, and Cassandra 5.0 supports only JDK 11 and 17.
- Of those two, this project targets 17: it compiles with `--release 17`, and `spring-test` 6.2
  requires 17 regardless, so supporting 11 would mean different bytecode levels per module for
  no practical gain.
- No released Cassandra line supports JDK 18–23.
- **JDK 24+ will never work.** Cassandra's `ThreadAwareSecurityManager` calls
  `System::setSecurityManager`, which is terminally deprecated and throws on 24 and later.

The build enforces this, so a wrong JDK fails with a clear message rather than a confusing
crash. If the message surprises you, check `mvn -v` rather than `java -version` — tools like
`jenv` install a shim that overrides `JAVA_HOME` for `mvn` only.

`cassandra-unit-dataset` carries none of that. It starts no daemon, so it needs no JPMS flags, and
the 24+ ceiling does not apply — it compiles to 17 and runs on anything later.

Version compatibility
---------------------

**From 5.0.0, the version number leads with the embedded Apache Cassandra major.** The minor and
patch are cassandra-unit's own, by ordinary semver. The driver version never appears in the
number — it is a compatibility fact, listed below. The full policy is in
[CONTRIBUTING.md](CONTRIBUTING.md#versioning).

The artifacts, as of 5.1.0:

| artifact | Embedded Cassandra | CQL driver | JDK |
|---|---|---|---|
| `cassandra-unit-dataset` | none — you supply the session | `org.apache.cassandra:java-driver-core` 4.19.3 | 17+ |
| `cassandra-unit` | 5.0.8 | same | 17 only |
| `cassandra-unit-spring` | via `cassandra-unit` | same | 17 only |

`cassandra-unit-dataset` also has one **optional** dependency, `assertj-core` **3.x**, needed only by
the fluent `CqlAssertions` API — see [Asserting in code](docs/assertions-fluent.md). Optional dependencies are not
transitive, so it reaches you only if you declare it yourself.

And the history, which is all `cassandra-unit`:

| cassandra-unit | Embedded Cassandra | CQL driver | JDK |
|---|---|---|---|
| `5.1.x` | 5.0.8 | `org.apache.cassandra:java-driver-core` 4.19.3 | 17 |
| `5.0.x` | 5.0.8 | `org.apache.cassandra:java-driver-core` 4.19.3 | 17 |
| `4.3.1.0` | 3.11.5 | `com.datastax.oss:java-driver-core` 4.3.1 *(optional)* | 8 |
| `3.7.1.0` | 3.11.4 | `com.datastax.cassandra:cassandra-driver-core` 3.7.1 *(optional)* | 8 |
| `3.11.2.0` | 3.11.4 | same — identical code to 3.7.1.0, see below | 8 |
| `3.5.0.1` | 3.11.2 | `com.datastax.cassandra:cassandra-driver-core` 3.5.0 *(optional)* | 8 |
| `3.3.0.2` | 3.11.0 | `com.datastax.cassandra:cassandra-driver-core` 3.3.0 *(optional)* | 8 |
| `3.1.3.2` | 3.9 | `com.datastax.cassandra:cassandra-driver-core` 3.1.3 *(optional)* | 7 |
| `2.2.2.1` | 2.2.2 | `com.datastax.cassandra:cassandra-driver-core` 2.1.9 *(optional)* | 7 |

Every release before 5.0.0 also put a **second** CQL driver on your classpath: `cassandra-all`
carries a shaded `cassandra-driver-core` exposing the older `com.datastax.driver.core.*` API, and
nothing excluded it. 5.0.0 does, so you now get exactly one driver.

Two rows need explaining, because they are the reason this section exists:

- **`4.3.1.0` tracked the driver, not Cassandra.** It embeds Cassandra **3.11.5**. Everyone who
  read it as "Cassandra 4" read it the obvious way and was wrong — between 3.0.0.1 and 4.3.1.0
  the number followed the DataStax driver, while `cassandra-all` moved independently.
- **`3.11.2.0` and `3.7.1.0` are the same code**, released seventeen minutes apart on 2019-05-09
  while the scheme was being reverted to driver-tracking. `3.7.1.0` is the intended one, even
  though it sorts *lower* than the release it replaced.

### Using a different driver version

The driver is deliberately not managed in this project's `dependencyManagement`, so you can pick
your own 4.x:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.cassandra</groupId>
            <artifactId>java-driver-core</artifactId>
            <version>4.18.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Setting a `cu.cassandra.driver.version` property in your own pom does **not** work, and this
trips people up. The published pom carries the literal `${cu.cassandra.driver.version}`, which
Maven resolves against *cassandra-unit's* parent, never yours. Use `dependencyManagement` above,
or declare `java-driver-core` directly.

Setup
-----

**If you already have a Cassandra**, this is the whole setup — no surefire block, nothing else:

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit-dataset</artifactId>
    <version>5.1.0</version>
    <scope>test</scope>
</dependency>
```

See [Using your own Cassandra](docs/with-your-own-cassandra.md) and stop here.

**If you want the embedded server**, take `cassandra-unit` instead — it includes everything above:

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit</artifactId>
    <version>5.1.0</version>
    <scope>test</scope>
</dependency>
```

### You must also configure surefire

This is not optional, and it is the single biggest difference from older versions. CassandraUnit
starts a **real Cassandra node inside your test JVM**, so your test JVM needs the same flags a
Cassandra server gets: the JPMS `--add-exports`/`--add-opens` set from Cassandra's own
`conf/jvm17-server.options`, plus the `jamm` memory-meter agent. Without them the JVM dies
during startup and surefire reports only
`The forked VM terminated without properly saying goodbye`.

```xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>properties</goal></goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            -javaagent:${com.github.jbellis:jamm:jar}
            -Djdk.attach.allowAttachSelf=true
            -Dio.netty.tryReflectionSetAccessible=true
            --add-exports java.base/jdk.internal.misc=ALL-UNNAMED
            --add-exports java.management.rmi/com.sun.jmx.remote.internal.rmi=ALL-UNNAMED
            --add-exports java.management/com.sun.jmx.remote.security=ALL-UNNAMED
            --add-exports java.rmi/sun.rmi.registry=ALL-UNNAMED
            --add-exports java.rmi/sun.rmi.server=ALL-UNNAMED
            --add-exports java.sql/java.sql=ALL-UNNAMED
            --add-exports java.base/java.lang.ref=ALL-UNNAMED
            --add-exports jdk.unsupported/sun.misc=ALL-UNNAMED
            --add-opens java.base/java.lang.module=ALL-UNNAMED
            --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
            --add-opens java.base/jdk.internal.ref=ALL-UNNAMED
            --add-opens java.base/jdk.internal.reflect=ALL-UNNAMED
            --add-opens java.base/jdk.internal.math=ALL-UNNAMED
            --add-opens java.base/jdk.internal.module=ALL-UNNAMED
            --add-opens java.base/jdk.internal.util.jar=ALL-UNNAMED
            --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED
            --add-opens java.base/sun.nio.ch=ALL-UNNAMED
            --add-opens java.base/java.io=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
            --add-opens java.base/java.nio=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

The `maven-dependency-plugin` `properties` goal is what resolves
`${com.github.jbellis:jamm:jar}` to the agent's real path, so no version is hardcoded.

Usage
-----

A dataset is a `.cql` script, or a `.yaml` / `.yml` / `.json` / `.xml` / `.csv` file of rows loaded
against a schema a `.cql` script created. The format comes from the extension:

```java
new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace")                  // one CQL script

CQLDataSetFactory.fromClassPathAll("mykeyspace",                         // schema, then rows
        "cql/schema.cql", "data/widget.yaml")
```

Either can go anywhere a dataset is accepted below. See [Datasets](docs/datasets.md).

### JUnit 5

```java
class MyTest {

    @RegisterExtension
    static CassandraUnitExtension cassandra =
            new CassandraUnitExtension(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));

    @Test
    void queries(CqlSession session) {   // injected by the extension
        ResultSet rs = session.execute("select * from my_table");
        ...
    }
}
```

### JUnit 4

```java
public class MyTest {

    @Rule
    public CassandraCQLUnit cassandra =
            new CassandraCQLUnit(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));

    @Test
    public void queries() {
        ResultSet rs = cassandra.session.execute("select * from my_table");
        ...
    }
}
```

### Spring Test

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(...)
@TestExecutionListeners(CassandraUnitTestExecutionListener.class)
@CassandraDataSet(value = {"cql/schema.cql", "data/widget.yaml"}, keyspace = "mykeyspace")
@EmbeddedCassandra
class MySpringTest { ... }
```

Spring is a `provided` dependency: your application decides the Spring version. The module is
compiled against Spring 6.2 and also runs on Spring 7.

One embedded Cassandra per JVM
------------------------------

**This is a permanent design constraint, not a bug.** Cassandra's `DatabaseDescriptor`,
`Schema` and `StorageService` hold static state that cannot be reset in-process, so:

- The first call to `startEmbeddedCassandra` in a JVM wins. Later calls return immediately.
- Asking for a *different* configuration file in the same JVM throws
  `UnsupportedOperationException`.
- `stopEmbeddedCassandra()` does not let you restart with a different configuration.

If different test classes need different configurations, give each one its own JVM:

```xml
<configuration>
    <reuseForks>false</reuseForks>
</configuration>
```

That costs one Cassandra startup (~3s) per test class, which is the honest price of an
in-process server. If you would rather not pay it, consider
[Testcontainers' Cassandra module](https://java.testcontainers.org/modules/databases/cassandra/)
instead — it runs a real node in Docker, with no JVM or JDK coupling to your test process.

Cleanup between tests is **driven by the dataset**, not by the rule or extension: a
`CQLDataSet` declares whether its keyspace is dropped and recreated on load, and that is what
isolates one test from the next. For a full wipe, call
`EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()` yourself.

Migrating from 4.3.1.0
----------------------

5.0.0 deliberately breaks compatibility. [docs/migrating-from-4.md](docs/migrating-from-4.md) is
the full guide and [CHANGELOG.md](CHANGELOG.md) the release-by-release detail. The highlights:

- **Removed: the `cassandra-unit-shaded` artifact.** It existed to hide old, vulnerable copies
  of guava/netty/jackson. Those versions are gone with the Cassandra 5.0 upgrade, so it has no
  purpose. Declare your own exclusions if you still need them.
- **Removed: the `cu-loader` / `cu-starter` command line tools.** `cu-starter` never worked.
- **Removed: `EmbeddedCassandraServerHelper.getRpcPort()`** — Thrift is gone from Cassandra 4.0+.
- **Removed: `@CassandraDataSet(type = ...)`**, and the XML/JSON/YAML dataset enum behind it.
  Only CQL datasets were loadable in 5.0.0. 5.1.0 adds YAML/JSON/XML/CSV *row* datasets, chosen by
  file extension rather than by an attribute — a new design, not the 4.x one.
- **JUnit 4 and Hamcrest are no longer compile-scope dependencies**, so they no longer land on
  your classpath through this library. Declare whichever test framework you actually use.
- **The driver is now a required dependency** (`org.apache.cassandra:java-driver-core`), not an
  optional one — that was the cause of the recurring `NoClassDefFoundError` reports.
- `tmpDir` now genuinely relocates Cassandra's data, commitlog, hints, saved caches and cdc
  directories. It previously relocated nothing but a copy of the yaml.
- **New: a JUnit 5 extension**, `CassandraUnitExtension`. See
  [docs/getting-started.md](docs/getting-started.md#junit-5).

License
-------
This project is licensed under the MIT License - see [LICENSE.txt](LICENSE.txt) for details.
