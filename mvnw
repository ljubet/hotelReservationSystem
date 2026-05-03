#!/bin/sh
# Maven Wrapper script (standard distribution)
MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-""}
if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
  MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_EXEC="java"
else
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

exec "$JAVA_EXEC" -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" -Dmaven.home="$MAVEN_PROJECTBASEDIR/.mvn/wrapper" -Dmaven.wrapper.properties="$WRAPPER_PROPERTIES" org.apache.maven.wrapper.MavenWrapperMain "$@"
