# Troubleshooting

Failure modes whose error message does not point at the cause. Roughly in order of how often they
bite.

## `The forked VM terminated without properly saying goodbye`

```
[ERROR] The forked VM terminated without properly saying goodbye. VM crash or System.exit called?
[ERROR] Process Exit Code: 3
```

The test JVM died before surefire could talk to it, so there is no stack trace. Two causes, in
order of likelihood.

**1. The surefire `argLine` is missing.** An embedded Cassandra 5.0 needs the JPMS
`--add-exports` / `--add-opens` flags and the `jamm` agent; without them the JVM fails during
startup. This is the most common first-run problem. See
[Getting started](getting-started.md#2-configure-surefire-mandatory).

**2. You are on the wrong JDK.** Check with **`mvn -v`**, not `java -version` — see below.

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

## Tests pass alone but fail together

Usually one of:

- **Unqualified table names.** The dataset load issues `USE <keyspace>` on the shared session, so
  the current keyspace is JVM-global. Qualify as `keyspace.table`.
- **`keyspaceDeletion` disabled** on a dataset that a later test expects to be empty.
- **A test calling `cleanEmbeddedCassandra()`**, which drops every non-system keyspace for
  everything else in that JVM.

## The suite is slow

Expect ~3 seconds per embedded startup. With `reuseForks=false` that is once per test class, which
is the cost of isolation. To trade isolation for speed, allow fork reuse and let tests share one
node — then every class must use the same configuration. If neither trade-off is acceptable, the
in-process design is probably the wrong tool.

---

Still stuck? Include your JDK (`mvn -v`), CassandraUnit version, and whether the `argLine` is
configured when you [open an issue](https://github.com/jsevellec/cassandra-unit/issues) — those
three answer most questions immediately.
