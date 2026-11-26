#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
suite=${1-all}
if [ "$#" -gt 1 ]; then echo 'usage: sh scripts/test.sh [suite]' >&2; exit 2; fi
case "$suite" in all|protocol|metadata|session|display) ;; *) echo "unknown suite: $suite" >&2; exit 2;; esac
: "${OXIWIGGLE_JDK:=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
if [ ! -x "$OXIWIGGLE_JDK/bin/javac" ]; then echo 'JDK 21 compiler missing' >&2; exit 1; fi
api_jar="$project_dir/.deps/extension-api-25.jar"
if [ ! -f "$api_jar" ]; then echo 'Bitwig API 25 JAR missing' >&2; exit 1; fi
recorded_hash=$(sed -n 's/^sha256=//p' dependencies/bitwig-api-25.properties)
actual_hash=$(shasum -a 256 "$api_jar" | cut -d ' ' -f 1)
if [ "$recorded_hash" != "$actual_hash" ]; then echo 'Bitwig API JAR checksum mismatch' >&2; exit 1; fi
recorded_version=$(sed -n 's/^version=//p' dependencies/jdk-21.properties)
actual_version=$("$OXIWIGGLE_JDK/bin/javac" -version 2>&1)
if [ "$actual_version" != "javac $recorded_version" ]; then echo "JDK version mismatch: $actual_version" >&2; exit 1; fi
rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' -print0 | xargs -0 "$OXIWIGGLE_JDK/bin/javac" --release 21 -encoding UTF-8 -proc:none -classpath "$api_jar" -d build/classes
find src/test/java -name '*.java' -print0 | xargs -0 "$OXIWIGGLE_JDK/bin/javac" --release 21 -encoding UTF-8 -proc:none -classpath "$api_jar:build/classes" -d build/test-classes
"$OXIWIGGLE_JDK/bin/java" -classpath "$api_jar:build/classes:build/test-classes:src/test/resources" io.github.wintocode.oxiwiggle.AllTests "$suite"
