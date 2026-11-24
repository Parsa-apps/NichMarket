#!/bin/sh
#
# Gradle wrapper bootstrap for NichMarket
# (This is a minimal launcher; the wrapper jar + properties live in gradle/wrapper/)
#

# Resolve script directory (follows symlinks)
app_path=$0
while [ -h "$app_path" ]; do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*) app_path=$link ;;
      *)  app_path=$( dirname "$app_path" )/$link ;;
    esac
done
APP_HOME=$( cd "${app_path%/*}" > /dev/null 2>&1 && pwd ) || exit 1

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to start the JVM
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD=java
    command -v java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        exit 1
    }
fi

exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
