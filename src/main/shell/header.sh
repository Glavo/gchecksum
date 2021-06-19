#!/usr/bin/env bash
_java=java
if test -n "$JAVA_HOME"; then
    _java="$JAVA_HOME/bin/java"
fi
if test -n "$GCHECKSUM_JAVA_HOME"; then
    _java="$GCHECKSUM_JAVA_HOME/bin/java"
fi
exec "$_java" -Xmx1g -XX:+UseG1GC $JAVA_OPTS $GCHECKSUM_JAVA_OPTS -cp "$0" 'org.glavo.checksum.Main' "$@"
exit 1
