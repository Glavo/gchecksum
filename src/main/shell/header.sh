#!/usr/bin/env bash
exec java -Xmx512m -XX:+UseG1GC $JAVA_OPTS $GCHECKSUM_JAVA_OPTS -cp "$0" 'org.glavo.checksum.Main' "$@"
exit 1
