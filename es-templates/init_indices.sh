#!/usr/bin/env bash

curl -XDELETE http://0.0.0.0:9200/geonames-italy

curl -XDELETE http://0.0.0.0:9200/geonames-others

curl -H 'Content-Type: application/json' -XPUT http://0.0.0.0:9200/geonames-italy -d @settings.json

curl -H 'Content-Type: application/json' -XPUT http://0.0.0.0:9200/geonames-others -d @settings.json

