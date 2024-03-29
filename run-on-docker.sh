#!/bin/sh

# Run on cluster of two containers on ports 8080 & 8081

docker network create distributedcounter

docker run --rm -d --network distributedcounter -p 8080:8080 --name distributedcounter1 localhost:6000/bkoprucu/distributedcounter
docker run --rm -d --network distributedcounter -p 8081:8080 --name distributedcounter2 localhost:6000/bkoprucu/distributedcounter
