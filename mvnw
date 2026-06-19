#!/bin/sh
MAVEN_PROJECTBASEDIR=$(pwd)
MAVEN_HOME="$MAVEN_PROJECTBASEDIR/.mvn/wrapper"
exec "$JAVA_HOME/bin/java" -jar "$MAVEN_HOME/maven-wrapper.jar" "$@"
