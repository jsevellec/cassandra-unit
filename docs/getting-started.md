# Your first test — embedded server

A real Apache Cassandra node inside your test JVM, with a dataset loaded, in five steps. No Docker.

> Already have a Cassandra — a container, a node on localhost, ScyllaDB, Astra? You want
> [Your first test — your own Cassandra](with-your-own-cassandra.md) instead. It needs no JVM flags
> and has no JDK ceiling.

## Requirements

| | |
|---|---|
| **JDK** | **17 — and only 17** |
| Maven | 3.9+ |
| Apache Cassandra | embedded, pulled in transitively — [Version compatibility](https://github.com/jsevellec/cassandra-unit/blob/main/README.md#version-compatibility) has the exact version |

JDK 17 is the entire supported set, not a recommendation:

- Cassandra 5.0 removed Java 8, and supports only JDK 11 and 17.
- Of those two this project targets 17, which `spring-test` requires anyway.
- No released Cassandra line supports JDK 18–23.
- **JDK 24+ can never work.** Cassandra's `ThreadAwareSecurityManager` calls
  `System::setSecurityManager`, which is terminally deprecated and throws on 24 and later.

The build enforces this, so a wrong JDK fails with a readable message. If that message surprises
you, run `mvn -v` rather than `java -version` — see [Troubleshooting](troubleshooting.md).

## 1. Add the dependency

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit</artifactId>
    <version>5.2.0</version>
    <scope>test</scope>
</dependency>
```

JUnit is **not** pulled in for you. CassandraUnit ships an integration for JUnit 4 and one for
JUnit Jupiter, and declares both as optional, so you keep whichever you already use. Add your own
`junit-jupiter` and/or `junit` dependency as normal.

The Jupiter integration is built against **Jupiter 6**, which is what Spring 7 requires. The
extensions themselves only implement callback interfaces that are the same in Jupiter 5, so they
generally work there too — but 6 is the version this project compiles and tests against.

For Spring support, add `cassandra-unit-spring` as well — see [Spring
integration](spring.md).

<a id="2-configure-surefire-mandatory"></a>

## 2. Configure surefire

This page starts a Cassandra node inside your test JVM, so that JVM needs the same flags a Cassandra
server gets: the JPMS `--add-exports` / `--add-opens` set from Cassandra's own
`conf/jvm17-server.options`.

### The configuration

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
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

That is the whole of it — flags on one plugin, nothing to resolve, no agent.

On 5.3.0 and earlier this block was bigger — see the
[changelog](https://github.com/jsevellec/cassandra-unit/blob/main/CHANGELOG.md).

### Gradle

The same flags on the `test` task. Pin the toolchain too: Gradle runs tests on its own JDK unless
told otherwise, and on anything but 17 the node never finishes starting.

```kotlin
java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.cassandraunit:cassandra-unit:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    jvmArgs(
        "-Dio.netty.tryReflectionSetAccessible=true",
        "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.management.rmi/com.sun.jmx.remote.internal.rmi=ALL-UNNAMED",
        "--add-exports", "java.management/com.sun.jmx.remote.security=ALL-UNNAMED",
        "--add-exports", "java.rmi/sun.rmi.registry=ALL-UNNAMED",
        "--add-exports", "java.rmi/sun.rmi.server=ALL-UNNAMED",
        "--add-exports", "java.sql/java.sql=ALL-UNNAMED",
        "--add-exports", "java.base/java.lang.ref=ALL-UNNAMED",
        "--add-exports", "jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.module=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.ref=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.math=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.module=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.util.jar=ALL-UNNAMED",
        "--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
    )
}
```

## 3. Write the dataset

`src/test/resources/cql/simple.cql`:

```sql
CREATE TABLE widget (id int PRIMARY KEY, label text);
INSERT INTO widget (id, label) VALUES (1, 'hello');
```

No `CREATE KEYSPACE` and no `USE` — CassandraUnit creates the keyspace and switches to it before
running the script.

That one file is enough to get started. When the fixture data grows, you can split it: keep the
schema in the `.cql` script and move the rows to a **row dataset** — in YAML, JSON, XML or CSV, or
[built in Java](datasets.md#in-java-with-no-file) if there are only a few —

`src/test/resources/data/widget.yaml`:

```yaml
widget:
  - id: 1
    label: hello
```

— loading the pair together:

```java
CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/simple.cql", "data/widget.yaml")
```

Values are then converted using the real column types instead of CQL literals you format by hand,
which is what makes `uuid`, `timestamp`, `blob` and collections painless. Either style works
anywhere a dataset is accepted; see [Datasets](datasets.md) for both.

## 4. Write the test

### JUnit Jupiter

```java
class WidgetTest {

    @RegisterExtension
    static CassandraUnitExtension cassandra =
            new CassandraUnitExtension(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));

    @Test
    void readsTheDataset(CqlSession session) {   // injected by the extension
        Row row = session.execute("select label from widget where id = 1").one();
        assertThat(row.getString("label")).isEqualTo("hello");
    }
}
```

A `CqlSession` parameter is resolved automatically. `cassandra.getSession()` works too, if you
prefer a field.

### JUnit 4

```java
public class WidgetTest {

    @Rule
    public CassandraCQLUnit cassandra =
            new CassandraCQLUnit(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"));

    @Test
    public void readsTheDataset() {
        Row row = cassandra.session.execute("select label from widget where id = 1").one();
        assertThat(row.getString("label")).isEqualTo("hello");
    }
}
```

## 5. Run it

```
mvn test
```

Expect roughly three seconds of startup for the embedded node, once per JVM. If it fails, go to
[Troubleshooting](troubleshooting.md) — most first-run failures are step 2.

## Next

- [Datasets](datasets.md) — the CQL and row dataset formats, keyspace create/drop control, and
  loading several files together.
- [Asserting in code](assertions-fluent.md) — the test above ends with a hand-written `SELECT`.
  `assertThat(session).keyspace(...).table(...)` replaces it, and needs nothing registered.
- [Asserting with a dataset file](assertions.md) — state every row a table should hold *after* the
  test, in the same format you loaded it with.
- [Embedded server](embedded-server.md) — ports, directories, random ports, cleaning between tests.
