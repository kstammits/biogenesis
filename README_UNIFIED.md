# Unified Biogenesis — 0.9 vanilla + X-Mod + Efficiency (Java 17)

Derived work in progress, maintained by Karl Stamm (https://github.com/kstammits,
kstammits) — this tree will eventually live there. It combines the GPL-2.0-or-later
lineages credited in About/COPYRIGHT (vanilla, X-Mod, Efficiency, ColorMod fixes)
with a Java 17 + JavaFX 21 port; the whole tree stays under GPL-2.0-or-later
(`gpl.txt`), with third-party notices in `NOTICE-JTS.txt` / `NOTICE-ICONS.txt`.

Branch `unified-0.9-xmod`. Requires JDK 17 (Homebrew `openjdk@17` preferred, falls back to `JAVA_HOME`/`PATH`).

## Run
- `./build.sh run` — launch the app. First run compiles (~98 classes); repeats skip the compile and start instantly. Pass-through args after `run` go to the app.
- `./build.sh` — compile only (skipped when up to date); `./build.sh jar` — build `biogenesis.jar`; `./build.sh clean` — remove `classes/` + jar.
- No Ant/Gradle needed. Legacy `src/build.xml`, `src/uninstall.mf` and `src/launch_biogenesis` were removed; `build.sh` is the canonical build.

## Layout (matches official `biogenesis_src_0_9.zip`)
- `src/biogenesis/*.java` — vanilla 0.9 source (symmetry 1-8, backup button, DE/PT/RU, STRtree perf)
- `src/biogenesis/images/`, `src/biogenesis/messages/` (7 locales)
- `src/biogenesis.mf`, `src/biogenesis.desktop` (Linux packaging, kept for reference)
- `lib/jts-core-1.15.0.jar` — required by `World.java` (`org.locationtech.jts`, STRtree collision). Not in git history; vendored from Maven Central. Vanilla 0.9 jars bundle the same version.
- `lib/javafx/` — JavaFX 21.0.13 (base, graphics, controls; mac-aarch64) from Maven Central (`org.openjfx`). Toolchain for the UI port; `build.sh` puts it on the module path. Swing build is unaffected.
- `build.sh` — canonical Java 17 build, no Ant/Gradle: `./build.sh run` launches the app

Root `changelog`, `copyright`, `gpl.txt` are still 0.8-era; canonical 0.9 `changelog` is in the release zip (`readme.txt` 2019-07-21).

## Why this base
- `master` = 0.8 flat, no deps, but missing 0.9 fixes.
- `origin/xmod` (v0.2.2, 2015-01-27, 64 ahead) = X-Mod gameplay depth: alternative mutations, rates vs probabilities with detailed balance, spontaneous mutations, stress-based aging replacing max-age, min/comfort/body-bound energy, yellow-length energy split. No JTS, packaged as `organisms/segments/world/...` (from `new_design`).
- Ported from `origin/EfficiencyOptions` (`TylerEdits`): efficiency toggle (visible-only cull), zoom in/out with coordinate conversion, prefs persistence. 0.9 vanilla already had STRtree + FPS fixes; the toggle/cull/zoom delta is what's merged here.
- Skipped: `Rick'sNNMod` (experimental brains/eyes), `ColorMod` 1.32 local (obsolete vs online 2.01), `PortingColorMod` (incomplete WIP port).

## What was merged (single tree, done)
`origin/xmod` can't fast-forward (packaged vs flat `src/biogenesis/`). Ported functional slices from `new_design..xmod` onto the flat tree:
1. Mutation rates + detailed balance (`GeneticCode.java`, `Gene.java`)
2. Minor/major mutations: split/merge/swap/flip/add/remove, clone gene
3. Spontaneous mutations + params (`ParamDialog.java`, `World.java`)
4. Stress vs age: stress resistance, min/comfort/body-bound energy, yellow-length split, growth/repro stress gates
5. Fixes: children gene-sharing, border-freeze, div-by-0 stats, clipboard sync mutations, prefs save
6. Version bump: `FILE_VERSION 700 -> 800`, `Utils.VERSION 900 -> 901` (old saves/prefs/net peers incompatible by design)

Reference: `git show origin/xmod:readme.txt`, `git log --oneline new_design..xmod`.

## Verify (Java 17)
- `./build.sh` → 99 classes, zero `-Xlint:removal` warnings
- `./build.sh run` launches `biogenesis.MainWindow`
- Rust port later: this tree is the spec baseline.

## Sim/view seam (`WorldView`)
`src/biogenesis/WorldView.java` decouples the simulation from Swing: `World`/`Organism`
talk to the view only through this interface (repaints, selection, stats events).
`VisibleWorld` implements it today; the JavaFX canvas view will implement it next.
Headless test gate (no UI, no display needed) — sim core plus the
save-dialog filename rules (`Fx.withAppendedExt`, the only branchy logic in
the Swing→FX bridge; the rest is thin dialog wrappers verified by eyeball):
- `./build.sh test` — incremental compile, then `SimSmoke` (500 ticks,
deterministic seed, asserts population survives) and `FxLogicTest`
(extension auto-append on/off, null-safe cancel path).
