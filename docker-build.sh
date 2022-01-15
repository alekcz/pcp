#!/usr/bin/env bash

echo $(cat resources/PCP_VERSION)
docker build -t pcp:$(cat resources/PCP_VERSION) .
# docker build -t pcp:latest .