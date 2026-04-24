#!/bin/sh

export JAVA_CLASSPATH=/app:/opt/jmeter/lib/ext/*:/app/lib/*:/metersphere:/metersphere/lib/*:/standalone/lib/*
export JAVA_MAIN_CLASS=io.metersphere.Application
export MS_VERSION=`cat /tmp/MS_VERSION`

sh /deployments/run-java.sh


