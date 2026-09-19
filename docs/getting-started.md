# Getting started

## Requirements

| | |
|---|---|
| **JDK** | **17 — and only 17** |
| Maven | 3.9+ |
| Apache Cassandra | embedded, pulled in transitively — [Version compatibility](../README.md#version-compatibility) has the exact version |

JDK 17 is the entire supported set, not a recommendation:

- Cassandra 5.0 removed Java 8, and supports only JDK 11 and 17.
- Of those two this project targets 17, which `spring-test` 6.2 requires anyway.
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
    <version>5.1.0</version>
    <scope>test</scope>
</dependency>
```

JUnit is **not** pulled in for you. CassandraUnit ships an integration for JUnit 4 and one for
JUnit 5 and declares both as optional, so you keep whichever you already use. Add your own
`junit-jupiter` and/or `junit` dependency as normal.

For Spring support, add `cassandra-unit-spring` as well — see [Spring
integration](spring.md).

## 2. Configure surefire (mandatory)

CassandraUnit starts a real Cassandra node inside your test JVM, so that JVM needs the same flags
a Cassandra server gets: the JPMS `--add-exports` / `--add-opens` set from Cassandra's own
`conf/jvm17-server.options`, plus the `jamm` memory-meter agent.

**Without these the JVM dies during startup** and surefire reports only
`The forked VM terminated without properly saying goodbye`, which does not mention the JVM flags,
the JDK, or Cassandra. This is the single most common setup problem.

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
`${com.github.jbellis:jamm:jar}` to the agent's real path on disk, so no version is hardcoded
into a path. The agent itself arrives transitively with `cassandra-all`; you do not declare it.

Gradle users need the equivalent on the `test` task's `jvmArgs`, and must resolve the jamm jar
path themselves.

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

### JUnit 5

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
