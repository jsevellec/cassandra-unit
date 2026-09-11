WELCOME to CassandraUnit
========================

What is it?
-----------
Like other \*Unit projects, CassandraUnit is a Java utility test tool.
It helps you create your Java Application with [Apache Cassandra](http://cassandra.apache.org) Database backend.
CassandraUnit is for Cassandra what DBUnit is for Relational Databases.

CassandraUnit helps you writing isolated JUnit tests in a Test Driven Development style.

Main features:

- Start an embedded Cassandra.
- Create the schema and load data from a CQL script.
- Integrations for JUnit 4 (`@Rule`), JUnit 5 (`Extension`) and Spring Test.

Requirements
------------

| | |
|---|---|
| Apache Cassandra | 5.0.8 (embedded, pulled in transitively) |
| **JDK** | **17 — nothing else** |
| Maven | 3.9+ |

That is not a recommendation, it is the whole supported set:

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

Setup
-----

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit</artifactId>
    <version>5.0.0</version>
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
@CassandraDataSet(value = "cql/dataset.cql", keyspace = "mykeyspace")
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

5.0.0 deliberately breaks compatibility. See [CHANGELOG.md](CHANGELOG.md) for the full list;
the highlights:

- **Removed: the `cassandra-unit-shaded` artifact.** It existed to hide old, vulnerable copies
  of guava/netty/jackson. Those versions are gone with the Cassandra 5.0 upgrade, so it has no
  purpose. Declare your own exclusions if you still need them.
- **Removed: the `cu-loader` / `cu-starter` command line tools.** `cu-starter` never worked.
- **Removed: `EmbeddedCassandraServerHelper.getRpcPort()`** — Thrift is gone from Cassandra 4.0+.
- **Removed: `@CassandraDataSet(type = ...)`**, and the XML/JSON/YAML dataset enum behind it.
  Only CQL datasets have been loadable for years.
- **JUnit 4 and Hamcrest are no longer compile-scope dependencies**, so they no longer land on
  your classpath through this library. Declare whichever test framework you actually use.
- **The driver is now a required dependency** (`org.apache.cassandra:java-driver-core`), not an
  optional one — that was the cause of the recurring `NoClassDefFoundError` reports.
- `tmpDir` now genuinely relocates Cassandra's data, commitlog, hints, saved caches and cdc
  directories. It previously relocated nothing but a copy of the yaml.

License
-------
This project is licensed under LGPL V3.0:
http://www.gnu.org/licenses/lgpl-3.0-standalone.html
