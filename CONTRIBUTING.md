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

## Releasing

Releases are cut by the **release** workflow in GitHub Actions, never from a laptop. It runs
`maven-release-plugin`, signs the artifacts, and uploads them to the
[Central Publisher Portal](https://central.sonatype.com). Actions → *release* → *Run workflow*:

| Input | Example | Notes |
|---|---|---|
| `releaseVersion` | `5.0.0` | one version for all three artifacts — neither module declares its own |
| `developmentVersion` | `5.0.1-SNAPSHOT` | what `main` is bumped to afterwards |
| `dryRun` | `true` | **leave it on the first time**: rehearses everything, pushes and uploads nothing |
| `autoPublish` | `false` | `false` leaves the deployment sitting in the Portal for you to inspect and publish by hand |

The job declares `environment: central`, so it pauses for an approval before it can do anything.

Every run, dry or not, begins with a **pre-flight** `mvn -Prelease verify`. That is what exercises
GPG signing, the source jar and the javadoc jar Central requires, and it exists because
`useReleaseProfile=false` plus `releaseProfiles=release` means the `release` profile is otherwise
active only during `release:perform` — that is, *after* `main` has been bumped and the tag pushed.
Without the pre-flight, a bad signing key costs you a burnt version number and some git surgery.

**Dry-run first anyway.** On top of the pre-flight it rehearses the version arithmetic and the
commit-and-tag steps, writing `pom.xml.tag` / `pom.xml.next` and stopping there — nothing pushed,
nothing uploaded.

Then run for real with `autoPublish=false`, check the deployment reached state **VALIDATED** in
the Portal, and press Publish there. Dropping a deployment is free; publishing is permanent and
the version number cannot be reused.

### Secrets

| Where | Name | What |
|---|---|---|
| repository | `CENTRAL_USERNAME`, `CENTRAL_PASSWORD` | Central Portal *user token* (View Account → Generate User Token), not the Portal login. Repo-level because the snapshot deploy needs them on every push to `main`. |
| environment `central` | `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` | `gpg --armor --export-secret-keys <KEYID>`, and its passphrase. |

The **public** half of the GPG key must be on a keyserver (`gpg --keyserver keys.openpgp.org
--send-keys <KEYID>`). Central rejects a signature whose key it cannot find, and signing locally
succeeds regardless — so forgetting this fails late, during upload.

### Namespace ownership

Publishing requires that the Portal account owns the `org.cassandraunit` namespace. It came from
OSSRH, which was shut down on 2025-06-30 with its namespaces migrated, so it should already be
there — but confirm it before the first release, because there is no way to check from the command
line: the Publisher API exposes `upload`, `status`, `deployment/<id>` and download paths, and no
namespace endpoint.

Sign in at [central.sonatype.com/publishing/namespaces](https://central.sonatype.com/publishing/namespaces)
**with the account that was used for OSSRH**. If `org.cassandraunit` appears under an *OSSRH
Namespaces* heading with a *Migrate Namespace* button, press it; self-service migration covers a
namespace with no parent or child and three or fewer publishers.

Note what the public metadata does *not* tell you. This:

```
curl -s https://repo1.maven.org/maven2/org/cassandraunit/cassandra-unit/maven-metadata.xml
```

shows `<release>4.3.1.0</release>` from January 2020, which proves only that the namespace is taken
— not who holds it now.

The practical check is the snapshot deploy below: it authenticates with the same token and fails
the same way on an unowned namespace (`401` for a bad token, `403` for the namespace), but nothing
about it is permanent and no version number is spent. Do that before attempting a release.

### Snapshots

Every push to `main` deploys the current `-SNAPSHOT` to Central's snapshot repository, so the
unreleased version is usable without building from source:

```xml
<repository>
    <id>central-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
</repository>
```

That step skips anything whose pom carries a non-snapshot version, and skips
`[maven-release-plugin]` commits, so a release in progress cannot leak out through it.
