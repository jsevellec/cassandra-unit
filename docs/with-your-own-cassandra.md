# Your first test — your own Cassandra

A Testcontainers container, a node on localhost, ScyllaDB, Astra — anything speaking CQL. You hand
`cassandra-unit-dataset` a `CqlSession`, it loads the fixtures through that, in four steps.

> Want a node started for you instead, in-process and without Docker? That is
> [Your first test — embedded server](getting-started.md). It costs JVM flags and pins you to
> JDK 17.

## 1. Add the dependency

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit-dataset</artifactId>
    <version>5.2.0</version>
    <scope>test</scope>
</dependency>
```

**No surefire configuration.** No `--add-opens`, no `--add-exports`, no JVM flags at all. Those exist
because the embedded server runs a real Cassandra daemon inside your test JVM, and they are needed
by [any JVM that starts one](getting-started.md#which-jvms-need-it); there is no daemon here.
**No JDK ceiling** either: `cassandra-unit` is pinned to JDK 17 and can never run on 24+, because
Cassandra's `ThreadAwareSecurityManager` calls `System::setSecurityManager`. This artifact needs 17
or later and has no upper bound.

Its whole dependency tree is `java-driver-core`, `jackson-databind`, `snakeyaml` and `slf4j-api`,
plus optional extras you only resolve by asking for them: `jackson-dataformat-csv` for CSV datasets,
and `spring-test` / `spring-context` for `SpringSessions`. The build enforces that: a
`bannedDependencies` rule fails if `cassandra-all` ever appears.

There is no JDK row to satisfy either: 17 or later, no upper bound, and Maven 3.9+.

## 2. Write the dataset

`src/test/resources/cql/simple.cql`:

```sql
CREATE TABLE widget (id int PRIMARY KEY, label text);
INSERT INTO widget (id, label) VALUES (1, 'hello');
```

No `CREATE KEYSPACE` and no `USE` — the dataset creates the keyspace and switches to it before
running the script, and drops it again afterwards unless you say otherwise. On a cluster where you
cannot create keyspaces, load into one that exists and turn both off:
`CQLDataSetFactory.fromClassPath("cql/simple.cql", false, false, "mykeyspace")`.

Rows can live in a separate YAML, JSON, XML or CSV file instead of in the script, which is what
makes `uuid`, `timestamp`, `blob` and collections painless — see [Datasets](datasets.md).

## 3. Write the test

```java
class WidgetTest {

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(() -> CqlSession.builder()
                    .addContactPoint(new InetSocketAddress("127.0.0.1", 9042))
                    .withLocalDatacenter("datacenter1")
                    .build())
            .closingSession()
            .load(new ClassPathCQLDataSet("cql/simple.cql", "mykeyspace"))
            .build();

    @Test
    void readsTheDataset(CqlSession session) {   // resolved by the extension
        Row row = session.execute("select label from widget where id = 1").one();
        assertThat(row.getString("label")).isEqualTo("hello");
    }
}
```

`using(...)` takes a **supplier**, not a session, and it is called lazily — that is what lets the
session depend on something else the test sets up, a container most of all. `closingSession()` says
this extension created the session and may close it; leave it off for a session you own elsewhere.
`load(...)` is the simple case: one dataset, loaded before every test.

No JVM flags, no `argLine`, nothing else to configure — see [Which JVMs need
it](getting-started.md#which-jvms-need-it) for why that is a property of the embedded daemon and not
of this library.

## 4. Run it

```
mvn test
```

The node has to be reachable before the first test runs. If you do not have one standing by, the
next section starts one per build.

## With Testcontainers

The same test with the node supplied by a container rather than by you, and the fixtures split in
two: the schema once per class, the rows before every test.

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
    void readsTheFixture(CqlSession session) {          // resolved by the extension
        ResultSet rs = session.execute("select * from mykeyspace.widget");
        ...
    }
}
```

Testcontainers' own Cassandra module gives you the node. `withInitScript` is the whole of its data
API — one CQL file. This is the rest: row datasets in YAML, JSON, XML and CSV, or
[built in Java](datasets.md#in-java-with-no-file), bound to the real column types read from the live
schema.

### Build the session inside the supplier

This is the one thing worth getting right, because the failure is confusing.

Jupiter runs `beforeAll` callbacks registered **declaratively** — which is how `@Testcontainers`
registers — before callbacks registered with `@RegisterExtension`, and runs `@BeforeAll` *methods*
after all callbacks. So this does **not** work:

```java
static CqlSession session;

@BeforeAll                                  // runs too late
static void connect() {
    session = CqlSession.builder()...build();
}

@RegisterExtension
static final CqlDataSetExtension fixtures =
        CqlDataSetExtension.using(() -> session)      // still null when the extension starts
                .load(...)
                .build();
```

Build the session in the supplier, as the first example does. The supplier is called once, lazily,
when the extension actually starts — by which time the container is up.

### Isolation, and `auto_snapshot`

`.isolation(Isolation.TRUNCATE)` keeps the keyspace between tests and empties its tables instead of
rebuilding the schema. On the embedded server that is [dramatically
faster](datasets.md#isolation-truncating-instead-of-dropping) at every keyspace size, and a
container — where schema changes have a real cluster to agree with — should favour it more, not
less.

**Turn off `auto_snapshot` whichever mode you pick.** Unless the server sets it to `false`,
Cassandra snapshots each table a `TRUNCATE` empties — and each table a `DROP KEYSPACE` drops, which
is the same setting gating both. The configurations shipped with the embedded server turn it off;
the stock `cassandra:5.0` image does **not**, so an un-overridden container writes a snapshot per
table per test on `DATASET` and on `TRUNCATE` alike, and fills its disk over a long suite. It is
not a reason to prefer one mode over the other; it is a reason to override the file.

There is no system property or environment variable for it — `auto_snapshot` is read from
`cassandra.yaml` and nowhere else, and no `nodetool` command or JMX operation changes it at
runtime. So overriding it means supplying the file:

```java
@Container
static final CassandraContainer cassandra = new CassandraContainer("cassandra:5.0")
        .withReuse(true)
        .withConfigurationOverride("cassandra-test-config");   // a classpath directory
```

`withConfigurationOverride` copies that classpath directory over `/etc/cassandra` in the container,
so it must hold a **complete** `cassandra.yaml` — the image's own is replaced, not merged. Start
from the one in the image and set `auto_snapshot: false`.

`Isolation.DATASET` remains the default, and stays a safe choice: it needs no thought about which
statements in your dataset are schema.

### Session ownership

The extension **never closes a session it did not create**. `closingSession()` opts in to closing it
in `afterAll`. Leave it off if something else owns the session's lifecycle — a Spring context, say.

## Without JUnit Jupiter

`CQLDataLoader` is the whole API, and it has always taken a session:

```java
new CQLDataLoader(session).load(
        CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml"));
```

That works against anything: a local node, a CI cluster, Astra, ScyllaDB, a Spring Boot
`@ServiceConnection` bean, a session another extension built. See [Datasets](datasets.md) for the
formats and the conversion rules — that page applies unchanged here.

Two more things on `CQLDataLoader`:

- `loadIfKeyspaceAbsent(dataSet)` loads only if the keyspace is not already there, and returns
  whether it did. This is what `schemaOnce` uses. It asks the server rather than remembering in a
  field, so it still does the right thing when several test classes share a JVM and a session.
- `CqlOperations.truncateKeyspace(session, keyspace, excludedTables...)` empties a keyspace's tables
  without dropping the schema.

### A warning about `TRUNCATE` on a stock image

`TRUNCATE` takes a snapshot first unless the server has `auto_snapshot: false`. The yaml files
shipped with the embedded server set it. **A stock `cassandra:5.0` image does not** — so on a
Testcontainers node, truncating between tests will write a snapshot every time, which is slow and
fills the container's disk. Override it if you go that route:

```java
new CassandraContainer("cassandra:5.0")
        .withConfigurationOverride("cassandra-test-config")   // a dir holding cassandra.yaml
```

## Asserting what ended up there

Both halves of the assertion feature live in this artifact, and neither needs the embedded server —
they take the same `CqlSession` you already built:

```java
import static org.cassandraunit.assertion.CqlAssertions.assertThat;

assertThat(session).keyspace("mykeyspace")
        .table("widget")
            .hasRowCount(3)
            .row("id", widgetId)
                .hasValue("label", "one");
```

That is [Asserting in code](assertions-fluent.md) — static methods, so it works with Testcontainers,
a local node, ScyllaDB or Astra, under any test framework or none.

For the other direction, a file stating every row a table should hold after the test, see
[Asserting with a dataset file](assertions.md). Outside the embedded server there is an extension
for it:

```java
@RegisterExtension
final ExpectedCassandraDataSetExtension expectations =
        new ExpectedCassandraDataSetExtension(fixtures::getSession);
```

The fluent API is the one that needs no registration at all.

## What this artifact does not give you

- **No server lifecycle.** Nothing starts or stops Cassandra; that is yours to arrange.
- **No `EmbeddedCassandraServerHelper`**, and no `CassandraCQLUnit` / `CassandraUnitExtension` —
  those live in `cassandra-unit` because they start the embedded daemon.
- **No server lifecycle for Spring either.** `cassandra-unit-spring`'s annotations start the
  embedded node, so they are not the way in here. Spring is still supported — see below.

If you want a node started for you and would rather not run Docker, use
[`cassandra-unit`](getting-started.md) instead — but read its surefire section first.

## With Spring

`SpringSessions` hands `CqlDataSetExtension` the `CqlSession` bean out of your test's
`ApplicationContext`, so fixtures load into the session Spring already owns — a Boot-auto-configured
one, or any bean you declare:

```java
@RegisterExtension
static final CqlDataSetExtension fixtures = CqlDataSetExtension
        .using(SpringSessions.fromApplicationContext())
        .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
        .rowsPerTest(CQLDataSetFactory.fromClassPath(
                "data/widget.yaml", false, false, "mykeyspace"))
        .build();
```

`spring-test` and `spring-context` are optional dependencies here, so they reach you only because
you are already using Spring. Do **not** add `closingSession()` — the bean belongs to the context.
The [Spring page](spring.md#boot-with-your-own-cassandra) has the full `@SpringBootTest` example and
the lifecycle gotchas.

Depending on `cassandra-unit` already gives you everything on this page: it depends on
`cassandra-unit-dataset` at compile scope, and the class and package names are identical. You never
need both.
