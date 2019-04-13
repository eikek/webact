#!/usr/bin/env bash

name=$(basename $1)
if [ -n "$2" ]; then
    name="$2"
fi
curl -XPUT -H"Contexnt-Type: text/plain" --data-binary @"$1" http://localhost:8011/scripts/$name
