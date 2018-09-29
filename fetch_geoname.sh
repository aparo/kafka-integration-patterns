#!/usr/bin/env bash
wget http://download.geonames.org/export/dump/allCountries.zip
unzip allCountries.zip
mv allCountries.txt /opt/data/
