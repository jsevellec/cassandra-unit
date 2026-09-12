# Contributing

## Building

You need **JDK 17** — see the README for why that is the entire supported range. Then:

```
mvn verify
```

If the build fails immediately with an enforcer message about the Java version, check `mvn -v`
rather than `java -version`. Tools like `jenv` install a shim that overrides `JAVA_HOME` for
`mvn` only, which otherwise shows up as surefire's unhelpful
`The forked VM terminated without properly saying goodbye`.

The suite takes a couple of minutes. Starting Cassandra costs about three seconds, and one
Cassandra per JVM is a hard constraint (see below), so the suite is split into two surefire
executions:

- **`default-test`** runs everything that uses the default `cu-cassandra.yaml` in a **single**
  JVM, sharing one Cassandra.
- **`isolated-config-tests`** runs, with `reuseForks=false`, the handful of classes that cannot
  share: those starting Cassandra with their own yaml, plus two that assert JVM-global state
  (`EmbeddedCassandraServerHelperDefaultsTest` checks `stopEmbeddedCassandra()` when nothing was
  started, and `CQLDataLoadTestWithReadTimeout` asserts the session request timeout, which the
  first caller in a JVM wins).

That is 7 Cassandra startups rather than 22. **If you add a test that needs its own Cassandra
configuration, or that asserts JVM-global state, add it to the isolated execution in `pom.xml`** —
otherwise it will either fail or, worse, break the tests that run after it in the shared JVM.

## Things worth knowing before you change anything

**One embedded Cassandra per JVM is a permanent invariant.** Cassandra's `DatabaseDescriptor`,
`Schema` and `StorageService` hold static state that cannot be reset in-process. Please do not
send patches that try to restart the daemon with a different configuration, or that add
`stopEmbeddedCassandra`-then-start logic — it cannot be made to work without per-instance
classloader isolation of the kind Cassandra's own in-JVM dtest framework uses. A separate fork per
configuration is the supported answer — see the two surefire executions described above.

**Name test classes so they actually run.** Surefire's default includes are `Test*`, `*Test`,
`*Tests` and `*TestCase`. Nine classes in this repository were silently never executed for years
because they were named `CQLDataLoadTestWithJunitRule` and similar. The build now sets an
explicit `**/*Test*.java` include, so a name containing `Test` anywhere is fine — but check that
your new test appears in the output.

**`cu-cassandra.yaml` is minimal on purpose.** Cassandra's `YamlConfigurationLoader` rejects
unknown properties, so every key in it was checked against `org.apache.cassandra.config.Config`
for the pinned Cassandra version. Do not paste in blocks from Cassandra's shipped
`conf/cassandra.yaml`; add only keys you need and verify them against `Config`.

**Don't reach for driver schema metadata to enumerate keyspaces.** The driver's default
`refreshed-keyspaces` setting excludes `system` and `system_*`, so `getMetadata().getKeyspaces()`
answers a different question than it appears to. Query `system_schema` instead.

**Cassandra upgrades are not routine.** A `cassandra-all` bump changes which JDKs work and can
invalidate the yaml, so Dependabot is configured to leave it alone. Do those by hand, and run the
full suite afterwards.

## Pull requests

- One logical change per PR, with a test that fails before it and passes after.
- Explain *why* in the commit message, not just what. The history is the main documentation of
  why this codebase looks the way it does.
- CI runs `mvn verify` on JDK 17.
