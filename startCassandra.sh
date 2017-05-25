#!/bin/bash

rm -rf temp
mkdir temp
sh runnable/script/cu-starter -p 9042 -s runnable/samples/schema.cql -d runnable
