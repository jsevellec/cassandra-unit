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

Because the node runs in-process, the versions of guava, netty, jackson, snakeyaml and logback
are dictated by the embedded Cassandra release. They are pinned via Cassandra's own
`cassandra-parent` dependencyManagement rather than chosen here, so the way to pick up fixes in
those libraries is a `cassandra-all` upgrade.

## Reporting a vulnerability

Open a report via GitHub's private vulnerability reporting on this repository
("Security" tab, "Report a vulnerability"). Please do not open a public issue for something
exploitable.

Given that this is a volunteer-maintained test library, expect acknowledgement to take days
rather than hours.
