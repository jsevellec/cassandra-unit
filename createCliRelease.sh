#!/bin/bash

mvn install dependency:copy-dependencies -DoutputDirectory=lib

rm -rf runnable
mkdir runnable

cp -R cassandra-unit/src/main/cli/* runnable

mkdir runnable/lib
cp -R cassandra-unit/lib runnable/

cp cassandra-unit/target/cassandra-unit-3.1.4.0-SNAPSHOT.jar runnable/lib/