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

## Versioning

**`<cassandra-major>.<cassandra-unit-minor>.<cassandra-unit-patch>`.** The major is the embedded
Apache Cassandra major. The minor and patch are cassandra-unit's own, by ordinary semver. The
driver version never appears in the number.

| Release | Embedded C* | What moved |
|---|---|---|
| `5.0.0` | 5.0.8 | first Cassandra 5 release |
| `5.0.1` | 5.0.8 | a cassandra-unit bugfix |
| `5.1.0` | 5.0.8 | cassandra-unit features, no C* change |
| `5.2.0` | 5.0.8 | more cassandra-unit features, no C* change |
| `5.3.0` | 5.1.x | C* **minor** bump |
| `6.0.0` | 6.0.x | C* **major** bump |

The bump rules, in full:

- Embedded Cassandra **major** bump → cassandra-unit **major** bump.
- Embedded Cassandra **minor** bump → at least a cassandra-unit **minor** bump. The server
  changed behaviour underneath the user; that is not a patch.
- Embedded Cassandra **patch** bump → no bump is owed by itself. Ride along with whatever
  cassandra-unit release is next.
- A breaking change to **cassandra-unit's own** API → **minor** bump within the current Cassandra
  line, called out in the CHANGELOG. The major is spent on Cassandra, so it is not available to
  signal this. That is the accepted cost of the scheme: a known trade, not an oversight.
- **The driver floats.** It is picked for compatibility and recorded in the README matrix, never
  encoded in the version. `.github/dependabot.yml` ignores `cassandra-all` but deliberately does
  not ignore the driver, which is exactly this policy expressed in config.

### Why this is enforced by the build

`mvn validate` fails if the project version does not lead with `cu.cassandra.all.version`'s major
(the `enforce-version-scheme` rule in the root pom). The check exists because the project has
gone wrong here before, twice:

- Between `3.0.0.1` (2016) and `4.3.1.0` (2020) the number tracked the **DataStax driver**, not
  Cassandra. `4.3.1.0` is driver 4.3.1 on Cassandra **3.11.5** — it looks like Cassandra 4 and
  never was.
- On 2019-05-09 the scheme flipped mid-flight: `3.11.2.0` was released, then commit `716dcf0`
  renumbered the identical code and `3.7.1.0` went out seventeen minutes later. A **lower**
  version shipped after a higher one, so nobody who had resolved `3.11.2.0` could be moved
  forward by any version range.

When the embedded Cassandra major changes, bump `cu.cassandra.all.version` and the project version
together — the build will not let you forget.

## Releasing

Releases are cut by the **release** workflow in GitHub Actions, never from a laptop. It runs
`maven-release-plugin`, signs the artifacts, and uploads them to the
[Central Publisher Portal](https://central.sonatype.com). Actions → *release* → *Run workflow*:

| Input | Example | Notes |
|---|---|---|
| `releaseVersion` | `5.0.0` | one version for the parent and both jars — neither module declares its own. Must lead with the embedded Cassandra major; see [Versioning](#versioning) |
| `developmentVersion` | `5.0.1-SNAPSHOT` | what `main` is bumped to afterwards |
| `dryRun` | `true` | **leave it on the first time**: rehearses everything, pushes and uploads nothing |

The job declares `environment: central`, so it pauses for an approval before it can do anything.

Every run, dry or not, begins with a **pre-flight** `mvn -Prelease verify`. That is what exercises
GPG signing, the source jar and the javadoc jar Central requires, and it exists because
`useReleaseProfile=false` plus `releaseProfiles=release` means the `release` profile is otherwise
active only during `release:perform` — that is, *after* `main` has been bumped and the tag pushed.
Without the pre-flight, a bad signing key costs you a burnt version number and some git surgery.

**Dry-run first anyway.** On top of the pre-flight it rehearses the version arithmetic and the
commit-and-tag steps, writing `pom.xml.tag` / `pom.xml.next` and stopping there — nothing pushed,
nothing uploaded.

Then run for real. The upload always stops at a **VALIDATED** deployment: `autoPublish` is pinned
to `false` in the pom, so nothing reaches Central without a human. Check that state in the Portal
and press Publish there. Dropping a deployment is free; publishing is permanent and the version
number cannot be reused.

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
