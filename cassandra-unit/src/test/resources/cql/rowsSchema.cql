CREATE TABLE widget (
    id       uuid PRIMARY KEY,
    label    text,
    tags     set<text>,
    created  timestamp,
    payload  blob,
    quantity bigint,
    ratio    double,
    addr     inet,
    props    map<text, int>
);

-- A row that already exists before any row dataset loads, so a test can tell "column absent from
-- the fixture" (leave it alone) apart from "column explicitly null" (overwrite it with a tombstone).
INSERT INTO widget (id, label) VALUES (00000000-0000-0000-0000-000000000009, 'preexisting');

-- A UDT and a tuple, so the documented "a UDT is a map keyed by field name" and "a tuple is its
-- CQL literal" are covered by a test rather than only by the docs.
CREATE TYPE address (street text, zip int);
CREATE TABLE place (
    id    uuid PRIMARY KEY,
    addr  frozen<address>,
    coord tuple<int, text>
);
