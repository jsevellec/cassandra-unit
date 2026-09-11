# Spring integration

`cassandra-unit-spring` plugs CassandraUnit into Spring's TestContext framework, so the embedded
node starts and the dataset loads as part of the Spring test lifecycle.

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit-spring</artifactId>
    <version>5.0.0</version>
    <scope>test</scope>
</dependency>
```

Spring itself is a **`provided`** dependency: your application decides the Spring version. The
module is compiled against Spring 6.2 and also runs on Spring 7 — every `spring-test` API it uses
is still present there.

You still need the surefire configuration from [Getting started](getting-started.md#2-configure-surefire-mandatory).
It is not optional here either.

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

`tmpDir` cannot default to the helper's constant directly, because annotation defaults must be
compile-time constants and that value is computed at runtime from `java.io.tmpdir`. Empty therefore
means "use the library default".

> There are no `clusterName`, `host` or `port` attributes. Older documentation showed them; they do
> not exist. Host and port come from the yaml — ask
> `EmbeddedCassandraServerHelper.getNativeTransportPort()` at runtime, especially with the
> random-port configuration.

### `@CassandraDataSet`

| Attribute | Default | Meaning |
|---|---|---|
| `value` | `{}` | classpath locations of the `.cql` scripts |
| `keyspace` | `cassandra_unit_keyspace` | keyspace to create and load into |

With `value` empty, the listener looks for a dataset by convention at
`<FullyQualifiedTestClassName>-dataset.cql`, then `<SimpleClassName>-dataset.cql`, and logs
"No dataset will be loaded" if neither exists.

> The `type` attribute is gone in 5.0.0, along with the `DataSetFileExtensionEnum` it referenced.
> CQL is the only dataset format — see [Datasets](datasets.md).

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

## Qualify your table names

The dataset load issues `USE <keyspace>` on the shared session, so the "current keyspace" is
JVM-global mutable state. A test that queries `widget` instead of `mykeyspace.widget` will pass or
fail depending on what ran before it. Always qualify.

## Spring Boot

There is no Boot-specific starter. Use the annotations above in a `@SpringBootTest`, or skip this
module entirely and use `CassandraUnitExtension` from the core artifact alongside
`@SpringBootTest` — it is a plain JUnit 5 extension and composes fine.
