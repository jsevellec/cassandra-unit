# Spring integration

`cassandra-unit-spring` plugs CassandraUnit into Spring's TestContext framework, so the embedded
node starts and the dataset loads as part of the Spring test lifecycle.

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit-spring</artifactId>
    <version>5.2.0</version>
    <scope>test</scope>
</dependency>
```

Spring itself is a **`provided`** dependency: your application decides the Spring version. The
module is compiled against Spring 7, and its Spring Boot coverage runs on Boot 4. Note that Spring 7
requires **JUnit Jupiter 6** — Spring's own `SpringExtension` calls JUnit 6 APIs — so a project on
JUnit 5 needs Spring 6.2 and Boot 3 instead.

`@EmbeddedCassandra` starts the node in the test JVM, so [the surefire
configuration](getting-started.md#2-configure-surefire) applies here for the same reason
it applies anywhere: the flags are needed by a JVM that starts the node. The one path that skips
them is [Boot with your own Cassandra](#boot-with-your-own-cassandra), where nothing starts a
daemon.

## Annotations

| Annotation | Required | Purpose |
|---|---|---|
| `@EmbeddedCassandra` | yes | starts the embedded node |
| `@CassandraDataSet` | no | loads one or more CQL datasets |
| `@CassandraUnit` | — | meta-annotation combining the two with their defaults |

### `@EmbeddedCassandra`

| Attribute | Default | Meaning |
|---|---|---|
| `configuration` | `cu-cassandra.yaml` | classpath name of the Cassandra yaml |
| `tmpDir` | `""` | storage directory; empty means `EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR` |
| `timeout` | `20000` | startup timeout in milliseconds |
| `exposeProperties` | `true` | publish the node's address as `spring.cassandra.*` — see [Spring Boot](#spring-boot) |

`tmpDir` cannot default to the helper's constant directly, because annotation defaults must be
compile-time constants and that value is computed at runtime from `java.io.tmpdir`. Empty therefore
means "use the library default".

> There are no `clusterName`, `host` or `port` attributes. Older documentation showed them; they do
> not exist. Host and port come from the yaml — ask
> `EmbeddedCassandraServerHelper.getNativeTransportPort()` at runtime, especially with the
> random-port configuration. In a Spring context you rarely need to: `exposeProperties` publishes
> the address for you, which is what lets [Spring Boot](#spring-boot) auto-configure against it.

### `@CassandraDataSet`

| Attribute | Default | Meaning |
|---|---|---|
| `value` | `{}` | classpath locations of the datasets |
| `keyspace` | `cassandra_unit_keyspace` | keyspace to create and load into |

Locations may be `.cql` scripts or `.yaml` / `.yml` / `.json` / `.xml` / `.csv` row datasets; the
format comes from the extension. Mixing them is the normal case, because a row dataset needs its
schema loaded first:

```java
@CassandraDataSet({"cql/schema.cql", "data/widget.yaml"})
```

That works because of a rule the listener already had: **only the first location drops and creates
the keyspace**, every later one loads into what is already there. Order matters, and schema goes
first.

**Only locations.** A dataset [built in Java](datasets.md#in-java-with-no-file) cannot go in the
annotation — the listener resolves classpath strings
(`AbstractCassandraUnitTestExecutionListener` calls `CQLDataSetFactory.fromClassPath`), and there is
nowhere to hand it an object. Name the schema script here and load the built rows yourself:

```java
@CassandraDataSet(value = "cql/schema.cql", keyspace = "mykeyspace")
class MySpringTest {

    @BeforeEach
    void rows() {
        new CQLDataLoader(EmbeddedCassandraServerHelper.getSession())
                .load(CQLDataSetFactory.builder("mykeyspace")
                        .table("widget").columns("id", "label").row(1, "hello")
                        .build());
    }
}
```

The annotation still drops and creates the keyspace, and the builder defaults to leaving it alone,
so the order is the usual one: schema from the script, rows after.

With `value` empty, the listener looks for a dataset by convention at
`<FullyQualifiedTestClassName>-dataset.<ext>`, then `<SimpleClassName>-dataset.<ext>`, trying
extensions in the order `cql`, `yaml`, `yml`, `json`, `xml`, `csv` within each layout. First hit
wins — so a project that already has a `-dataset.cql` resolves to exactly the file it always did.
It logs "No dataset will be loaded" if none exists.

> The `type` attribute is gone as of 5.0.0, along with the `DataSetFileExtensionEnum` it
> referenced, and is not coming back: the extension is the single source of truth. See
> [Datasets](datasets.md).

## Listeners

Pick one:

- **`CassandraUnitTestExecutionListener`** — starts the node and loads the dataset. The usual
  choice.
- **`CassandraUnitDependencyInjectionTestExecutionListener`** — the same, plus Spring dependency
  injection, for tests that autowire a bean which talks to Cassandra.
- **`CassandraUnitDependencyInjectionIntegrationTestExecutionListener`** — as above, and reinjects
  dependencies after the dataset load, for use with `@DirtiesContext`.

## Example

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/my-context.xml")
@TestExecutionListeners(CassandraUnitTestExecutionListener.class)
@EmbeddedCassandra
@CassandraDataSet(value = "cql/dataset.cql", keyspace = "mykeyspace")
class MySpringTest {

    @Test
    void queries() {
        CqlSession session = EmbeddedCassandraServerHelper.getSession();
        Row row = session.execute("select label from mykeyspace.widget where id = 1").one();
        assertThat(row.getString("label")).isEqualTo("hello");
    }
}
```

Note `@ExtendWith(SpringExtension.class)`, not `@RunWith(SpringJUnit4ClassRunner.class)`. The old
runner still ships in `spring-test` but is deprecated, and Spring Boot 3+ is Jupiter-based.

`@TestExecutionListeners` **replaces** Spring's defaults unless you ask otherwise. If you need
Spring's standard listeners as well:

```java
@TestExecutionListeners(
        listeners = CassandraUnitTestExecutionListener.class,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
```

### Autowiring a bean

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/my-context.xml")
@TestExecutionListeners(CassandraUnitDependencyInjectionTestExecutionListener.class)
@CassandraUnit
class MyAutowiredTest {

    @Autowired
    private MyCassandraRepository repository;

    @Test
    void usesTheRepository() {
        assertThat(repository.findLabel(1)).isEqualTo("hello");
    }
}
```

## Asserting the result

Both ways of asserting work here. `@ExpectedCassandraDataSet` is read by the listeners, which check
it before the keyspace is cleaned — see
[Asserting with a dataset file](assertions.md#wiring-it-up). The fluent API needs no listener at
all, because it is static methods over the session:

```java
assertThat(EmbeddedCassandraServerHelper.getSession())
        .keyspace("mykeyspace").table("widget").hasRowCount(1);
```

See [Asserting in code](assertions-fluent.md).

## Qualify your table names

The dataset load issues `USE <keyspace>` on the shared session, so the "current keyspace" is
JVM-global mutable state. A test that queries `widget` instead of `mykeyspace.widget` will pass or
fail depending on what ran before it. Always qualify — in your test code, and in any `.cql` script
that does not create its own keyspace.

Row datasets are the exception: they qualify every statement with the dataset's keyspace
themselves, so a `.yaml` / `.json` / `.xml` / `.csv` dataset lands in the right place regardless of
what the session had current.
## Spring Boot

`@SpringBootTest` works, and since 5.3.0 it needs no wiring: annotate the class with
`@EmbeddedCassandra` and the node's address is published into the test's `Environment` before the
context refreshes, so Boot's own auto-configured `CqlSession` connects to it.

```java
@SpringBootTest
@TestExecutionListeners(value = CassandraUnitTestExecutionListener.class,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
@CassandraDataSet({"cql/widgetSchema.cql", "rows/widgets.yaml"})
@EmbeddedCassandra
class WidgetBootTest {

    @Autowired
    CqlSession session;                 // Boot's own bean, pointed at the embedded node

    @Test
    void readsTheFixture() {
        long rows = session.execute(
                "select count(*) from cassandra_unit_keyspace.widget").one().getLong(0);
        assertThat(rows).isEqualTo(1);
    }
}
```

`MERGE_WITH_DEFAULTS` matters: `@TestExecutionListeners` replaces Boot's default listeners
otherwise, and dependency injection is one of them.

Qualify the table name. The dataset is loaded through the embedded server's own session, and the
`USE <keyspace>` that goes with it applies to *that* session, not to the bean Boot built — see
[Qualify your table names](#qualify-your-table-names).

### What gets published

| Property | Value |
|---|---|
| `spring.cassandra.contact-points` | `EmbeddedCassandraServerHelper.getHost()` |
| `spring.cassandra.port` | `EmbeddedCassandraServerHelper.getNativeTransportPort()` |
| `spring.cassandra.local-datacenter` | `datacenter1` |

This exists because the defaults do not line up: Boot and the driver default to **9042**, while the
embedded node listens on **9142** (`cu-cassandra.yaml`) — deliberately, so it cannot collide with a
Cassandra you are running locally. Without the bridge the two never meet.

**The port cannot be written into `application-test.yml`.** With
`@EmbeddedCassandra(configuration = "cu-cassandra-rndport.yaml")` it is chosen at startup and does
not exist until the node is up. That is the case this feature is really for, and it is why the
values are published at runtime rather than documented as constants for you to copy.

**These win over values you set yourself.** The property source is added first, so a
`spring.cassandra.port` in your configuration — or in `@TestPropertySource` — is overridden by the
port the node is actually listening on. That is usually what you want, and it is the only thing that
can work with a random port. If you would rather your own values won:

```java
@EmbeddedCassandra(exposeProperties = false)
```

**Do not set `spring.cassandra.keyspace-name`.** Boot would build the session with
`withKeyspace(...)` during the refresh — before any dataset has had a chance to create that keyspace
— and the context would fail to start. Name the keyspace in `@CassandraDataSet(keyspace = "...")`
and qualify your queries instead.

`local-datacenter` mirrors what the shipped configurations report. A custom yaml that changes the
snitch changes it, and then it is yours to override.

### Boot with your own Cassandra

If the node is not the embedded one — a Testcontainers container, a shared cluster, Astra — then
none of the above applies, because there is no embedded server to publish an address for. Boot
already has a `CqlSession`; what it lacks is fixtures. Load them through that same bean with
`CqlDataSetExtension` and `SpringSessions`, from
[`cassandra-unit-dataset`](with-your-own-cassandra.md):

```java
@SpringBootTest
@Testcontainers
class WidgetContainerTest {

    @Container
    static final CassandraContainer cassandra = new CassandraContainer("cassandra:5.0");

    @DynamicPropertySource                      // point Boot at the container
    static void cassandraProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contact-points", cassandra::getHost);
        registry.add("spring.cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", cassandra::getLocalDatacenter);
    }

    @RegisterExtension
    static final CqlDataSetExtension fixtures = CqlDataSetExtension
            .using(SpringSessions.fromApplicationContext())
            .schemaOnce(CQLDataSetFactory.fromClassPath("cql/schema.cql", "mykeyspace"))
            .rowsPerTest(CQLDataSetFactory.fromClassPath(
                    "data/widget.yaml", false, false, "mykeyspace"))
            .build();

    @Test
    void readsTheFixture(CqlSession session) {  // the context's bean, resolved by the extension
        ...
    }
}
```

If you have `spring-boot-testcontainers` on the test classpath, Boot's own `@ServiceConnection` on
the container field replaces that `@DynamicPropertySource` method entirely. Either way the part that
matters here is the same: `SpringSessions` puts the fixtures into the session Boot built, rather
than into a second session of its own.

That path has no JDK ceiling and needs no surefire configuration, because nothing starts a Cassandra
daemon inside the test JVM. It is also the only one of the two that works on JDK 24+.

### Gotchas

- **Never call `closingSession()`** with `SpringSessions`. The session is a Spring bean and Spring
  closes it; doing it here would close it early, because Jupiter runs `@RegisterExtension` teardown
  *before* `SpringExtension`'s, and would hand the next test class a dead bean out of the context
  cache. It is off by default — leave it off.
- **`@DirtiesContext` has one shape that does not work** with `SpringSessions`: the session is
  resolved once per class, so `AFTER_EACH_TEST_METHOD` or a method-level `@DirtiesContext` leaves the
  extension holding a session whose context has been closed. Class-level `AFTER_CLASS` — the default
  — is fine.
- **Leave the `CqlSession` test parameter bare.** Both `SpringExtension` and `CqlDataSetExtension`
  resolve parameters, and Spring claims one only when it carries `@Autowired`, `@Qualifier` or
  `@Value`. Adding `@Autowired` to the parameter makes both claim it and Jupiter fails the test with
  *"Discovered multiple competing ParameterResolvers"*. Use a bare parameter, or an `@Autowired`
  field.
- **Pick one path per class.** The annotations start and load through the embedded server;
  `CqlDataSetExtension` loads through a session you own. Combining them in one test class means two
  different ideas of where the data went.
