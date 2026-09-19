-- Schema for the expected-dataset tests.
--
-- Separate from rowsSchema.cql for two reasons. That file seeds a row at load time, which a strict
-- expectation would report as unexpected; and it has no table with clustering columns at all -
-- widget and place are both `id uuid PRIMARY KEY` - so intra-partition ordering, the one ordering
-- an expectation can assert, would be untestable.

CREATE TABLE widget (
    id       uuid PRIMARY KEY,
    label    text,
    tags     set<text>,
    created  timestamp,
    quantity bigint,
    ratio    double,
    props    map<text, int>
);

-- Clustering columns, so row order within a partition is a thing that exists, and a static column,
-- whose value repeats on every row of its partition.
CREATE TABLE event (
    day   text,
    at    timestamp,
    kind  text,
    total int static,
    PRIMARY KEY (day, at)
) WITH CLUSTERING ORDER BY (at DESC);

-- Counters cannot be loaded by a row dataset - there is no INSERT for them - but they read back as
-- a bigint, so they can be asserted. Seeded here.
CREATE TABLE tally (id text PRIMARY KEY, hits counter);
UPDATE tally SET hits = hits + 3 WHERE id = 'a';
