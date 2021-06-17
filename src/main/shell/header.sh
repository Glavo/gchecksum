#!/usr/bin/env bash
@ 2>/dev/null # 2>nul & echo off & goto BOF
:
exec java -Xmx512m -XX:+UseG1GC $JAVA_OPTS -cp "$0" 'org.glavo.checksum.Main' "$@"
exit

:BOF
setlocal
@echo off
java -Xmx512m -XX:+UseG1GC %JAVA_OPTS% -cp "%~dpnx0" org.glavo.checksum.Main %*
endlocal
exit /B %errorlevel%
