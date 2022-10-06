# D&D Minis Warband Incubator

A simple web application to build [D&D Minis](https://ddmguild.org/) warbands easily. 

This project uses https://quarkus.io, the Supersonic Subatomic Java Framework.

## Test data

The test data located in [main/src/data/creatures.json](https://github.com/mkouba/ddmwi/blob/main/src/data/creatures.json) are parsed from the xml files attached to the DDMWarbandTool application (windows only, downloaded from http://irafay.com/DDM/).

## How to build

> mvn clean install

## How to run in the dev mode

> mvn compile quarkus:dev
