# CassandraUnit documentation

> **These docs describe the 5.x line.** 5.0.0 is on Maven Central. The row datasets described in
> [Datasets](datasets.md) ship in 5.1.0, and the driver-only `cassandra-unit-dataset` artifact
> described in [Using your own Cassandra](with-your-own-cassandra.md) ships in 5.2.0.
>
> 5.0.0 deliberately breaks compatibility with 4.3.1.0 — it upgrades the embedded server from
> Cassandra 3.11.5 to 5.0.8, requires JDK 17, and removes the command line tools, the shaded
> artifact and the 4.x XML/JSON/YAML dataset formats. If you are using **4.3.1.0 or earlier**, these
> pages will not match what you have installed; see [Migrating from 4.x](migrating-from-4.md) for
> exactly what changed.
>
> These pages replace the project wiki, which had drifted so far that it documented classes and
> annotation attributes that never existed. Documentation now lives in the repository so that it
> is versioned with the code and reviewed alongside it.

## Start here

Pick the one that matches what you already have:

| | |
|---|---|
| **I already have a Cassandra** — Testcontainers, a local node, ScyllaDB, Astra | [Using your own Cassandra](with-your-own-cassandra.md) — the `cassandra-unit-dataset` artifact. No embedded server, no JVM flags, no JDK ceiling. |
| **I want one started for me, in-process** | [Getting started](getting-started.md) — the `cassandra-unit` artifact. **Read the surefire section first**: an embedded Cassandra 5.0 cannot start without extra JVM flags, and skipping them produces a confusing failure. |

Either way, [Datasets](datasets.md) is the page that matters most: the two dataset kinds — `.cql`
scripts, and YAML/JSON/XML/CSV row datasets — how values are converted, how to load several files
together, and how keyspaces are created and dropped between tests. It applies to both artifacts.

## Reference

| | |
|---|---|
| [Using your own Cassandra](with-your-own-cassandra.md) | `cassandra-unit-dataset`: loading fixtures through a `CqlSession` you supply, the `CqlDataSetExtension`, and what the artifact deliberately leaves out. |
| [Embedded server](embedded-server.md) | The `EmbeddedCassandraServerHelper` API: starting, configuring ports and directories, cleaning between tests, and the one-instance-per-JVM constraint. |
| [Spring integration](spring.md) | `cassandra-unit-spring`: the `@EmbeddedCassandra`, `@CassandraDataSet` and `@CassandraUnit` annotations with Spring's TestContext framework. |
| [Troubleshooting](troubleshooting.md) | The failure modes that are hard to diagnose from their error messages. Check here first if a test JVM dies without explanation. |
| [Migrating from 4.x](migrating-from-4.md) | Everything removed or changed in 5.0.0, and what to do instead. |

Release-by-release detail lives in [CHANGELOG.md](../CHANGELOG.md). How to build and contribute is
in [CONTRIBUTING.md](../CONTRIBUTING.md).

## What CassandraUnit is, in one paragraph

CassandraUnit is two things, and you can take either.

**The fixture loader** turns a YAML, JSON, XML, CSV or CQL file into rows in a real keyspace, with
every value converted using the column's actual type read from the live schema — so a `text` column
holding `"1"` stays the string `"1"`, and `uuid`, `timestamp`, `blob`, collections and UDTs all work
without you hand-formatting CQL literals. It runs against any `CqlSession` you hand it.

**The embedded server** starts a real Apache Cassandra node inside your test JVM, for when you want
one and would rather not run Docker. That in-process design is the source of both its convenience
and its constraints: exactly one Cassandra per JVM, your test JVM needs the JVM flags a Cassandra
server needs, and your JDK is the server's JDK.

The loader does not need the server. If you are already using [Testcontainers' Cassandra
module](https://java.testcontainers.org/modules/databases/cassandra/), it gives you the node and
`cassandra-unit-dataset` gives you the data — `withInitScript` is one CQL file and nothing else.
