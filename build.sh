#!/bin/bash
# Java 17 build for unified tree (no Ant/Gradle required)
# Usage: ./build.sh [clean|jar|run]
#   (no arg)  compile if sources changed
#   jar       build biogenesis.jar if stale
#   run       launch the app (builds first if stale)
#   clean     remove build outputs
# Prefers Homebrew openjdk@17, falls back to JAVA_HOME / PATH java.
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
LIB="$ROOT/lib/jts-core-1.15.0.jar"
OUT="$ROOT/classes"
JARFILE="$ROOT/biogenesis.jar"
MARKER="$OUT/biogenesis/MainWindow.class"
if [ -x /opt/homebrew/opt/openjdk@17/bin/javac ]; then
  JAVAC=/opt/homebrew/opt/openjdk@17/bin/javac
  JAVA=/opt/homebrew/opt/openjdk@17/bin/java
  JAR=/opt/homebrew/opt/openjdk@17/bin/jar
else
  JAVAC=javac
  JAVA=java
  JAR=jar
fi

# JavaFX 21 (vendored in lib/javafx, mac-aarch64). Present so the toolchain
# is ready for the UI port; the Swing app doesn't need it yet.
FX_DIR="$ROOT/lib/javafx"
FX_MODULES="javafx.controls,javafx.graphics,javafx.swing"
FX_FLAGS=()
if ls "$FX_DIR"/javafx-*.jar >/dev/null 2>&1; then
  FX_FLAGS=(--module-path "$FX_DIR" --add-modules "$FX_MODULES")
fi
# True (0) when classes are missing or any source/lib is newer than them.
classes_stale() {
  [[ ! -f "$MARKER" ]] && return 0
  [[ "$LIB" -nt "$MARKER" ]] && return 0
  [[ -n $(find "$SRC/biogenesis" -name "*.java" -newer "$MARKER" -print -quit) ]] && return 0
  [[ -n $(find "$FX_DIR" -name "*.jar" -newer "$MARKER" -print -quit 2>/dev/null) ]] && return 0
  return 1
}

# True (0) when the jar is missing or classes are newer than it.
jar_stale() {
  [[ ! -f "$JARFILE" ]] && return 0
  [[ -n $(find "$OUT" -name "*.class" -newer "$JARFILE" -print -quit) ]] && return 0
  return 1
}

build_classes() {
  mkdir -p "$OUT"
  "$JAVAC" -nowarn --release 17 -cp "$LIB" "${FX_FLAGS[@]}" -d "$OUT" "$SRC"/biogenesis/*.java
  cp -R "$SRC"/biogenesis/images "$OUT"/biogenesis/ 2>/dev/null || true
  cp -R "$SRC"/biogenesis/messages "$OUT"/biogenesis/ 2>/dev/null || true
  echo "compiled $(ls "$OUT"/biogenesis/*.class | wc -l | tr -d ' ') classes"
}

build_jar() {
  # Self-contained: bundle JTS into the jar so `java -jar` works anywhere
  # (the -jar flag ignores external classpaths).
  local stage
  stage=$(mktemp -d)
  cp -R "$OUT/biogenesis" "$stage/"
  (cd "$stage" && "$JAR" xf "$LIB")
  (cd "$stage" && "$JAR" cfm "$JARFILE" "$SRC/biogenesis.mf" .)
  rm -rf "$stage"
  echo "built biogenesis.jar (self-contained, JTS bundled)"
}

if [[ "$1" == "clean" ]]; then
  rm -rf "$OUT" "$JARFILE"
  echo "cleaned"
  exit 0
fi

if classes_stale; then
  build_classes
else
  echo "classes up to date, skipping compile"
fi

if [[ "$1" == "jar" || "$1" == "run" ]]; then
  if jar_stale; then
    build_jar
  else
    echo "biogenesis.jar up to date"
  fi
fi

if [[ "$1" == "run" ]]; then
  exec "$JAVA" "${FX_FLAGS[@]}" -jar "$JARFILE" "${@:2}"
fi
