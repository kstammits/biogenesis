#!/bin/bash
# Java 17 build for unified tree (no Ant/Gradle required)
# Usage: ./build.sh [clean|jar|run]
# Prefers Homebrew openjdk@17, falls back to JAVA_HOME / PATH java.
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
LIB="$ROOT/lib/jts-core-1.15.0.jar"
OUT="$ROOT/classes"
if [ -x /opt/homebrew/opt/openjdk@17/bin/javac ]; then
  JAVAC=/opt/homebrew/opt/openjdk@17/bin/javac
  JAVA=/opt/homebrew/opt/openjdk@17/bin/java
  JAR=/opt/homebrew/opt/openjdk@17/bin/jar
else
  JAVAC=javac
  JAVA=java
  JAR=jar
fi
[[ "$1" == "clean" ]] && rm -rf "$OUT" *.jar && echo "cleaned" && exit 0
mkdir -p "$OUT"
"$JAVAC" -nowarn --release 17 -cp "$LIB" -d "$OUT" "$SRC"/biogenesis/*.java
cp -R "$SRC"/biogenesis/images "$OUT"/biogenesis/ 2>/dev/null || true
cp -R "$SRC"/biogenesis/messages "$OUT"/biogenesis/ 2>/dev/null || true
echo "compiled $(ls "$OUT"/biogenesis/*.class | wc -l) classes"
if [[ "$1" == "jar" ]]; then
  "$JAR" cfm biogenesis.jar "$SRC/biogenesis.mf" -C "$OUT" biogenesis
  echo "built biogenesis.jar"
fi
if [[ "$1" == "run" ]]; then
  "$JAVA" -cp "$OUT:$LIB" biogenesis.MainWindow
fi
