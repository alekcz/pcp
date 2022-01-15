#!/usr/bin/env bash

docker build -t pcp:$(cat resources/PCP_VERSION) .
docker build -t pcp:latest .