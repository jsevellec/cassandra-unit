CREATE KEYSPACE IF NOT EXISTS cas WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;

USE cas;

CREATE TABLE IF NOT EXISTS ticketgrantingticket (
    id text PRIMARY KEY,
    ticket text,
    ticket_granting_ticket_id text,
    expiration_bucket bigint
) WITH default_time_to_live = 5184000;

CREATE MATERIALIZED VIEW IF NOT EXISTS ticket_cleaner AS
SELECT expiration_bucket, ticket, id FROM ticketgrantingticket
WHERE id IS NOT NULL AND expiration_bucket IS NOT NULL AND ticket IS NOT NULL
PRIMARY KEY (expiration_bucket, id);

CREATE MATERIALIZED VIEW cas.proxygrantingticket
AS SELECT ticket_granting_ticket_id
FROM cas.ticketgrantingticket
WHERE ticket_granting_ticket_id IS NOT NULL
PRIMARY KEY ((ticket_granting_ticket_id), id);

CREATE TABLE IF NOT EXISTS serviceticket (
    id text PRIMARY KEY,
    ticket text
) WITH default_time_to_live = 60;

CREATE TABLE IF NOT EXISTS ticket_cleaner_lastrun (
    id text PRIMARY KEY,
    last_run bigint
);
