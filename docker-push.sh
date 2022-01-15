#!/usr/bin/env bash

docker image tag pcp:$(cat resources/PCP_VERSION) alekcz/pcp:$(cat resources/PCP_VERSION) 
docker image tag pcp:latest alekcz/pcp:latest
docker image push alekcz/pcp:$(cat resources/PCP_VERSION)
docker image push alekcz/pcp:latest