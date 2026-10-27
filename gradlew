#!/bin/sh
# Standard Gradle wrapper launcher; wrapper JAR must be present in gradle/wrapper/.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR is missing. Install Gradle 8.11.1 or restore gradle/wrapper/gradle-wrapper.jar." >&2
  exit 1
fi
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
