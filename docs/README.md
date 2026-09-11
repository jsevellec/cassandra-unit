# CassandraUnit documentation

> **These docs describe version 5.0.0, which is not released yet.**
>
> 5.0.0 deliberately breaks compatibility with 4.3.1.0 — it upgrades the embedded server from
> Cassandra 3.11.5 to 5.0.8, requires JDK 17, and removes the command line tools, the shaded
> artifact and the XML/JSON/YAML dataset formats. If you are using **4.3.1.0 or earlier**, these
> pages will not match what you have installed; see [Migrating from 4.x](migrating-from-4.md) for
> exactly what changed.
>
> These pages replace the project wiki, which had drifted so far that it documented classes and
> annotation attributes that never existed. Documentation now lives in the repository so that it
> is versioned with the code and reviewed alongside it.

## Start here

| | |
|---|---|
| [Getting started](getting-started.md) | Add the dependency, configure surefire, write a first passing test. **Read this before anything else** — an embedded Cassandra 5.0 cannot start without extra JVM flags, and skipping them produces a confusing failure. |
| [Datasets](datasets.md) | Writing the `.cql` scripts that create your schema and load data, and controlling how keyspaces are created and dropped between tests. |

## Reference

| | |
|---|---|
| [Embedded server](embedded-server.md) | The `EmbeddedCassandraServerHelper` API: starting, configuring ports and directories, cleaning between tests, and the one-instance-per-JVM constraint. |
| [Spring integration](spring.md) | `cassandra-unit-spring`: the `@EmbeddedCassandra`, `@CassandraDataSet` and `@CassandraUnit` annotations with Spring's TestContext framework. |
| [Troubleshooting](troubleshooting.md) | The failure modes that are hard to diagnose from their error messages. Check here first if a test JVM dies without explanation. |
| [Migrating from 4.x](migrating-from-4.md) | Everything removed or changed in 5.0.0, and what to do instead. |

Release-by-release detail lives in [CHANGELOG.md](../CHANGELOG.md). How to build and contribute is
in [CONTRIBUTING.md](../CONTRIBUTING.md).

## What CassandraUnit is, in one paragraph

CassandraUnit starts a real Apache Cassandra node **inside your test JVM** and loads a CQL script
into it, so tests run against Cassandra itself rather than a mock. That in-process design is the
source of both its convenience and its constraints: there is exactly one Cassandra per JVM, your
test JVM needs the JVM flags a Cassandra server needs, and your JDK is the server's JDK. If those
trade-offs do not suit you, [Testcontainers' Cassandra
module](https://java.testcontainers.org/modules/databases/cassandra/) runs a node in Docker
instead, with no coupling to your test process.
