#!/bin/sh
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir"
rm -f build/OxiWiggle.bwextension
sh scripts/test.sh
: "${OXIWIGGLE_JDK:=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
mkdir -p build/package/META-INF
rm -rf build/package
mkdir -p build/package/META-INF
cp -R build/classes/. build/package/
cp -R src/main/resources/. build/package/
cp LICENSE build/package/META-INF/LICENSE
"$OXIWIGGLE_JDK/bin/jar" --create --file build/OxiWiggle.bwextension -C build/package .
echo 'PASS build/OxiWiggle.bwextension'
