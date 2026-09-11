# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 5.0.x | yes |
| 4.3.x and earlier | no |

Versions up to and including 4.3.1.0 embed Apache Cassandra 3.11.5 and its 2020-era dependency
tree, which carries a large number of published advisories (snakeyaml 1.11, jackson-databind
2.10.0, logback 1.2.3, netty 4.1.39, guava 18, spring 4.0.2, libthrift 0.12.0 and others). They
will not be patched. Upgrade to 5.0.x.

## Scope

CassandraUnit is a **test-scoped** library that starts a throwaway Apache Cassandra node inside
a test JVM. It is not intended for production use, and the embedded node is deliberately
configured for convenience rather than safety: authentication is off, durability is reduced, and
`cassandra.unsafesystem` is set to skip fsync. Do not point it at real data or expose its ports.

Because the node runs in-process, the versions of guava, netty, jackson, snakeyaml and lz4 are
largely dictated by the embedded Cassandra release, so the usual way to pick up fixes in them is
a `cassandra-all` upgrade.

As of 5.0.0 the compile/runtime dependency set (96 artifacts across both modules) has **no
known advisories** per [OSV](https://osv.dev). Reaching zero needed three versions pinned ahead
of what `cassandra-all` 5.0.8 itself resolves, each verified against the full test suite:

| Artifact | Cassandra pins | We pin | Why |
|---|---|---|---|
| `io.netty:*` (via `netty-bom`) | 4.1.130.Final | 4.1.137.Final | 5 advisories in 4.1.130 |
| `com.fasterxml.jackson:*` (via `jackson-bom`) | 2.19.2 | 2.22.1 | 5 advisories in 2.19.2 |
| `at.yawk.lz4:lz4-java` | 1.10.1 | 1.11.2 | CVE-2026-59949 (JVM crash via native XXHash) |

These overrides are a maintenance liability, not a permanent arrangement: they should be
re-checked on every `cassandra-all` upgrade and deleted once Cassandra catches up. Note also
that "no known advisories" is a statement about published data on a given day, not a guarantee.

## Reporting a vulnerability

Open a report via GitHub's private vulnerability reporting on this repository
("Security" tab, "Report a vulnerability"). Please do not open a public issue for something
exploitable.

Given that this is a volunteer-maintained test library, expect acknowledgement to take days
rather than hours.
