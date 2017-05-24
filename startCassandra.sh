#!/bin/bash

rm -rf temp
mkdir temp
sh runnable/script/cu-loader -f ../samples/dataSetDefaultValues.yaml -h localhost -p 9042 -y runnable/samples/cassandra.yaml -t 20000