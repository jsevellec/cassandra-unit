#!/bin/bash

mvn install dependency:copy-dependencies -DoutputDirectory=lib

mkdir tmp

cp -R cassandra-unit/src/main/cli/* tmp

mkdir tmp/lib
cp -R cassandra-unit/lib tmp/

cp ~/.m2/repository/org/cassandraunit/cassandra-unit/3.1.4.0-SNAPSHOT/cassandra-unit-3.1.4.0-SNAPSHOT.jar tmp/lib/

