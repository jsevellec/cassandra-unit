#!/bin/bash

rm -rf temp
mkdir temp
sh runnable/script/cu-starter -p 9042 -t 20000 -s runnable/samples/schema.cql
