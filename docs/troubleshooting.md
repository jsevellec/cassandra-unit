# Troubleshooting

Failure modes whose error message does not point at the cause. Roughly in order of how often they
bite.

## `The forked VM terminated without properly saying goodbye`

```
[ERROR] The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Process Exit Code: 1
```

The test JVM died before surefire could talk to it, so there is no stack trace. Three causes, in
order of likelihood.

**1. You are on the wrong JDK.** Check with **`mvn -v`**, not `java -version` — see below.

**2. A `-javaagent` or `-XX` flag of your own is wrong.** Anything the JVM cannot honour at launch
kills the fork before surefire gets a word out of it. The message prints the full forked command
line; read what it actually launched.

**3. On 5.3.0 and earlier: the `argLine` is configured but `maven-dependency-plugin` is not.** Those
versions need the `jamm` agent, and its path is a property — `${com.github.jbellis:jamm:jar}` —
resolved by that plugin's `properties` goal. Without it surefire launches the fork with the literal
string. Look a few lines above surefire's message, where the JVM says so itself:

```
Error opening zip file or JAR manifest missing : ${com.github.jbellis:jamm:jar}
Error occurred during initialization of VM
```

From 5.4.0 there is no agent and no such property, so this one cannot happen — see
[the surefire section](getting-started.md#the-configuration).

Note that a **missing** `argLine` does not produce this error at all. That failure looks completely
different; see below.

## `ExceptionInInitializerError` / `IllegalAccessException` when the node starts

```
java.lang.ExceptionInInitializerError
    at org.apache.cassandra.config.DatabaseDescriptor.resolveCommitLogWriteDiskAccessMode(DatabaseDescriptor.java:1501)
    at org.apache.cassandra.config.DatabaseDescriptor.initializeCommitLogDiskAccessMode(DatabaseDescriptor.java:2909)
    at org.apache.cassandra.config.DatabaseDescriptor.applySimpleConfig(DatabaseDescriptor.java:640)
    at org.apache.cassandra.config.DatabaseDescriptor.applyAll(DatabaseDescriptor.java:455)
    at org.apache.cassandra.config.DatabaseDescriptor.daemonInitialization(DatabaseDescriptor.java:263)
    at org.cassandraunit.utils.EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.java:153)
    … extension and JUnit frames …
Caused by: java.lang.RuntimeException: java.lang.IllegalAccessException: access to public member failed:
    sun.nio.ch.DirectBuffer.cleaner … from class org.apache.cassandra.io.util.FileUtils (unnamed module @10742304)
    at org.apache.cassandra.io.util.FileUtils.<clinit>(FileUtils.java:106)
```

The JPMS flags are missing. Unlike the case above, the fork starts fine and the failure lands in
the test, on the call that starts the node — an `IllegalAccessException` naming a `sun.*` or
`jdk.internal.*` member is always a missing `--add-opens`.

Take [the whole set](getting-started.md#the-configuration). The flags are not independent, and
whichever one you leave out simply moves the failure to the next class that needs it.

## Do I actually need the `argLine`?

Only a **JVM that starts the embedded node** does. The flags are read at JVM launch, so the unit is
the forked JVM — the surefire *execution*, not the module and not the dependency. Depending on
`cassandra-unit` while your tests all run against a Cassandra of your own needs none of it, and
`cassandra-unit-dataset` never does.

Mirror image of the `reuseForks` note further down: the simple thing is to set it module-wide and
forget it, since flags on a JVM that never starts a node cost nothing. Split it per execution only
when you want one execution kept clean — [Mixed modules](embedded-server.md#mixed-modules).

## `mvn -v` disagrees with `java -version`

A version manager shim can override `JAVA_HOME` for Maven only, so exporting `JAVA_HOME` appears to
work and changes nothing:

```
$ export JAVA_HOME=$(/usr/libexec/java_home -v 17)
$ java -version      # 17
$ mvn -v             # 21 — the shim won
```

`jenv` does this, and it is easy to lose an hour to. `which mvn` showing something under
`~/.jenv/shims/` confirms it. Work around it by calling the real Maven directly:

```
JAVA_HOME=$(/usr/libexec/java_home -v 17) /opt/homebrew/bin/mvn test
```

or by setting the JDK in the version manager itself. The build's enforcer rule catches a wrong JDK
with a readable message, but only once Maven starts on the JDK you did not intend.

## `Unsupported class file major version` / enforcer rejects the JDK

The supported JDK range is **17 only**, and there is no way around it:

- Cassandra 5.0 dropped Java 8 and supports 11 and 17; this project targets 17.
- No released Cassandra supports JDK 18–23.
- JDK 24+ cannot work: Cassandra's `ThreadAwareSecurityManager` calls
  `System::setSecurityManager`, terminally deprecated and throwing there.

If you need a newer JDK for your application, run the tests on 17 or move to
[Testcontainers](https://java.testcontainers.org/modules/databases/cassandra/), which decouples the
server's JDK from yours.

## `UnsupportedOperationException: We can't launch two Cassandra configurations in the same JVM`

Two test classes asked for different yaml files and surefire ran them in the same JVM. There is one
Cassandra per JVM and that is permanent — see
[Embedded server](embedded-server.md#one-cassandra-per-jvm).

```xml
<configuration>
    <reuseForks>false</reuseForks>
</configuration>
```

## Which artifact am I using?

`cassandra-unit-dataset` is the fixture layer alone; `cassandra-unit` is that plus the embedded
server, and depends on it. If `EmbeddedCassandraServerHelper` or `CassandraUnitExtension` will not
resolve, you have the dataset artifact and want `cassandra-unit` — see
[Your first test — your own Cassandra](with-your-own-cassandra.md) for which is which.

You never need both: `cassandra-unit` brings the other in at compile scope, with identical package
and class names.

### "Package org.cassandraunit in both module ..."

`org.cassandraunit` and `org.cassandraunit.utils` are split across the two jars. That is invisible
on the classpath, which is where surefire puts test dependencies, and it is only an error when both
jars are on the **module path** at once. `cassandra-unit` can never go there anyway — it needs
`add-opens ...=ALL-UNNAMED` and `jdk.internal.*` reflection. If you hit
this, put them on the classpath.

Only `cassandra-unit-dataset` declares an `Automatic-Module-Name`
(`org.cassandraunit.dataset`). `cassandra-unit` deliberately does not: it can never be a module, and
declaring one makes the javadoc tool resolve against the module path, where a broken jar in
`cassandra-all`'s transitive tree (`sjk-core`, which has a class in the default package) fails the
build.

## Address already in use / `BindException`

The default configuration uses storage 7010, ssl storage 7011 and native transport 9142 — chosen to
avoid a locally running Cassandra on 9042 — but they are still fixed ports. When several builds
share a machine, switch to the random-port configuration:

```java
EmbeddedCassandraServerHelper.startEmbeddedCassandra(
        EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE);
```

and ask for the port rather than hardcoding it:

```java
int port = EmbeddedCassandraServerHelper.getNativeTransportPort();
```

Note the ports are probed by opening and closing a socket, then bound a moment later when the
daemon starts. That window is small but real; a retry is the only defence.

## `NoClassDefFoundError: com/datastax/oss/driver/api/core/CqlSession`

You are on 4.3.1.0 or earlier, where the driver was declared `<optional>true</optional>` while
being imported unconditionally. Declare the driver yourself, or upgrade — 5.0.0 makes it a required
dependency, which is the actual fix.

## `[ERROR] 'dependencies.dependency.systemPath' ... ${jmc5.path}`

```
[ERROR] 'dependencies.dependency.systemPath' for com.jrockit.mc:com.jrockit.mc.common:jar
        must specify an absolute path but is ${jmc5.path}/plugins/...
```

**Harmless, and not caused by CassandraUnit.** Maven reads `cassandra-parent` as the parent pom of
`cassandra-all`, and that pom declares system-scoped JMC and VisualVM artifacts with unresolved
properties. It is logged at ERROR level but the build succeeds. Only upstream Cassandra can fix it.

## `SigarException: no libsigar-...dylib in java.library.path`

Also harmless. Cassandra's startup checks try to load the long-abandoned `sigar` native library and
tolerate its absence, which is common on Apple Silicon. Startup continues.

## `InvalidQueryException: Virtual keyspace 'system_views' is not user-modifiable`

An older CassandraUnit trying to drop Cassandra 4.0+ virtual keyspaces during cleanup. Fixed in
5.0.0, which recognises `system_views` and `system_virtual_schema` as system keyspaces.

## Data is not where `tmpDir` says

On 4.3.1.0 and earlier `tmpDir` relocated nothing but a copy of the yaml — the data, commitlog,
hints, saved caches and CDC directories kept the hardcoded `target/embeddedCassandra/*` paths.
Fixed in 5.0.0. On older versions, edit those paths in your own yaml instead.

## `could not prepare INSERT INTO ... unconfigured table`

A row dataset ran before its schema existed. A row dataset only inserts rows — whether it is a
`.yaml` / `.json` / `.xml` / `.csv` file or a builder — so something has to create the table first,
and order matters:

```java
CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml")
```

Schema first, rows after. If you are loading by hand with two `CQLDataLoader.load` calls, check the
second one has `keyspaceCreation` and `keyspaceDeletion` **off** — see the next entry.

## A row dataset loaded, and the table is empty afterwards

The second dataset dropped the keyspace the first had just populated. Loading by hand, every dataset
after the first needs both flags off:

```java
loader.load(CQLDataSetFactory.fromClassPath("cql/schema.cql",   true,  true,  "mykeyspace"));
loader.load(CQLDataSetFactory.fromClassPath("data/widget.yaml", false, false, "mykeyspace"));
```

`CQLDataSetFactory.fromClassPathAll("mykeyspace", "cql/schema.cql", "data/widget.yaml")` does this
for you and is the better answer.

## `cannot convert value [...] for column x.y of type ...`

The value in the file does not fit the column. The message names the dataset, the table, the column
and the type, and its tail says what specifically was wrong. Two common causes:

- **A decimal written to a `text` column.** `label: 1.10` in YAML is the *number* 1.1, and storing
  `"1.1"` would quietly contradict the file, so it is refused. Quote it: `label: "1.10"`. A whole
  number is fine unquoted — `label: 1` gives `"1"`.
- **An unquoted `blob`.** `payload: 0x0a0b` is the number 2571 to YAML. Quote it: `"0x0a0b"`.

Generally: if a value's YAML meaning differs from its CQL meaning, quote it.

## A column I set in the fixture came back null

Two different things look alike here:

- **The column is absent from that row** — it is left out of the generated `INSERT` entirely, so
  nothing is written. That is *unset*, and it deliberately does not overwrite an existing value.
- **The column is present with a `null` value** — a tombstone is written.

In **CSV an empty field means unset**, always; CSV has no way to express an explicit null. Use YAML
or JSON when you need a tombstone.

## `CSV datasets need jackson-dataformat-csv on the test classpath`

CSV is the only dataset format needing a dependency that is not already present, and it is declared
`optional` so it does not land on the classpath of projects that do not use it:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-csv</artifactId>
  <scope>test</scope>
</dependency>
```

No version is needed if you already import `com.fasterxml.jackson:jackson-bom`. YAML, JSON and XML
need nothing added.

## `Unsupported dataset extension` / `Cannot tell the format of ...`

The format comes from the file extension, and the supported set is `cql`, `yaml`, `yml`, `json`,
`xml`, `csv`. A dataset with no extension, or an unrecognised one, cannot be loaded. Rename the
file.

## `a CSV dataset cannot name its own table`

CSV is flat, so the table name comes from the filename: `data/widget.csv` loads into `widget`. This
error means the location had no usable stem. Name the file after the table.

## Tests pass alone but fail together

Usually one of:

- **Unqualified table names in a CQL script.** The dataset load issues `USE <keyspace>` on the
  shared session, so the current keyspace is JVM-global. Qualify as `keyspace.table`. Row datasets
  are not affected — they qualify every statement with their own keyspace.
- **`keyspaceDeletion` disabled** on a dataset that a later test expects to be empty.
- **A test calling `cleanEmbeddedCassandra()`**, which drops every non-system keyspace for
  everything else in that JVM.

## The suite is slow

Expect ~3 seconds per embedded startup. With `reuseForks=false` that is once per test class.

**`reuseForks=false` is usually applied far more widely than it needs to be.** It is only required
for classes that cannot share a JVM — ones starting Cassandra with a *different* configuration, or
asserting JVM-global state. Everything using the same configuration can share a single JVM and a
single Cassandra. Splitting surefire into two executions along that line is worth doing:

```xml
<executions>
  <execution>
    <id>default-test</id>
    <configuration>
      <reuseForks>true</reuseForks>
      <excludes><exclude>**/NeedsItsOwnConfigTest.java</exclude></excludes>
    </configuration>
  </execution>
  <execution>
    <id>isolated-config-tests</id>
    <goals><goal>test</goal></goals>
    <configuration>
      <reuseForks>false</reuseForks>
      <includes><include>**/NeedsItsOwnConfigTest.java</include></includes>
    </configuration>
  </execution>
</executions>
```

This project does exactly that, and it took its own suite from 22 Cassandra startups to 7, roughly
halving the build. Watch for two traps when classes start sharing a JVM: a test calling
`stopEmbeddedCassandra()` stops the server for everything after it, and a test asserting a
JVM-global setting such as the session request timeout will see whatever the first caller set.

If neither trade-off is acceptable, the in-process design is probably the wrong tool.

---

Still stuck? Include your JDK (`mvn -v`), CassandraUnit version, and whether the `argLine` is
configured when you [open an issue](https://github.com/jsevellec/cassandra-unit/issues) — those
three answer most questions immediately.
