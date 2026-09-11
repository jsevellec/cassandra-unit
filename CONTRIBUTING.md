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

The suite takes a few minutes: `reuseForks=false` means every test class starts its own
embedded Cassandra (about 3 seconds each). That is deliberate, see below.

## Things worth knowing before you change anything

**One embedded Cassandra per JVM is a permanent invariant.** Cassandra's `DatabaseDescriptor`,
`Schema` and `StorageService` hold static state that cannot be reset in-process. Please do not
send patches that try to restart the daemon with a different configuration, or that add
`stopEmbeddedCassandra`-then-start logic — it cannot be made to work without per-instance
classloader isolation of the kind Cassandra's own in-JVM dtest framework uses. `reuseForks=false`
is the supported answer.

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
