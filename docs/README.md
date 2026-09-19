# CassandraUnit documentation

> **These docs describe the 5.x line**, and 5.2.0 is the current release on Maven Central.
> Everything on these pages is in it, including the fluent assertion API in
> [Asserting in code](assertions-fluent.md) and the Java dataset builder, both added by 5.2.0.
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

Either way, two pages matter most. [Datasets](datasets.md) is one: the two dataset kinds — `.cql`
scripts, and row datasets written as YAML/JSON/XML/CSV or built in Java — how values are converted,
how to load several files together, and how keyspaces are created and dropped between tests. It
applies to both artifacts.

Asserting is the other, and nothing else in the Java/Cassandra ecosystem does it: stating what a
table should hold *after* a test, not only what it held before. It comes two ways, sharing one
comparison — [Asserting in code](assertions-fluent.md) for a single value or row count, and
[Asserting with a dataset file](assertions.md) for every row a table should hold.

## Reference

| | |
|---|---|
| [Asserting in code](assertions-fluent.md) | The fluent `CqlAssertions` API: navigating session to keyspace to table to row, the method reference, and the optional AssertJ dependency. |
| [Asserting with a dataset file](assertions.md) | `@ExpectedCassandraDataSet`: comparing a table against an expected dataset, the Cassandra-specific comparison rules, and the failure report. |
| [Using your own Cassandra](with-your-own-cassandra.md) | `cassandra-unit-dataset`: loading fixtures through a `CqlSession` you supply, the `CqlDataSetExtension`, and what the artifact deliberately leaves out. |
| [Embedded server](embedded-server.md) | The `EmbeddedCassandraServerHelper` API: starting, configuring ports and directories, cleaning between tests, and the one-instance-per-JVM constraint. |
| [Spring integration](spring.md) | `cassandra-unit-spring`: the `@EmbeddedCassandra`, `@CassandraDataSet` and `@CassandraUnit` annotations with Spring's TestContext framework. |
| [Troubleshooting](troubleshooting.md) | The failure modes that are hard to diagnose from their error messages. Check here first if a test JVM dies without explanation. |
| [Migrating from 4.x](migrating-from-4.md) | Everything removed or changed in 5.0.0, and what to do instead. |

Release-by-release detail lives in [CHANGELOG.md](https://github.com/jsevellec/cassandra-unit/blob/main/CHANGELOG.md). How to build and contribute is
in [CONTRIBUTING.md](https://github.com/jsevellec/cassandra-unit/blob/main/CONTRIBUTING.md).

## What CassandraUnit is, in one paragraph

CassandraUnit is three things, and you can take any of them.

**The fixture loader** turns a YAML, JSON, XML, CSV or CQL file — or a builder, in Java — into rows
in a real keyspace, with every value converted using the column's actual type read from the live
schema — so a `text` column
holding `"1"` stays the string `"1"`, and `uuid`, `timestamp`, `blob`, collections and UDTs all work
without you hand-formatting CQL literals. It runs against any `CqlSession` you hand it.

**The assertions** run the comparison the other way: given a table, is it what it should be? Either
as a file listing every row, or as fluent code checking one value. Both read the column's real type
from the live schema, exactly as the loader does, so the two directions agree about what a value is.

**The embedded server** starts a real Apache Cassandra node inside your test JVM, for when you want
one and would rather not run Docker. That in-process design is the source of both its convenience
and its constraints: exactly one Cassandra per JVM, your test JVM needs the JVM flags a Cassandra
server needs, and your JDK is the server's JDK.

The loader does not need the server. If you are already using [Testcontainers' Cassandra
module](https://java.testcontainers.org/modules/databases/cassandra/), it gives you the node and
`cassandra-unit-dataset` gives you the data — `withInitScript` is one CQL file and nothing else.
