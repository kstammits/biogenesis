# Unified Biogenesis — 0.9 vanilla + X-Mod port (Java 8)

Branch `unified-0.9-xmod`. Stays on Java 8 for now (system has 1.8.0_151/341 only).

## Layout (matches official `biogenesis_src_0_9.zip`)
- `src/biogenesis/*.java` — vanilla 0.9 source (symmetry 1-8, backup button, DE/PT/RU, STRtree perf)
- `src/biogenesis/images/`, `src/biogenesis/messages/` (7 locales)
- `src/build.xml`, `src/biogenesis.mf`, `src/uninstall.mf`, `src/launch_biogenesis`, `src/biogenesis.desktop`
- `lib/jts-core-1.15.0.jar` — required by `World.java` (`org.locationtech.jts`, STRtree collision). Not in git history; vendored from Maven Central. Vanilla 0.9 jars bundle the same version.
- `build.sh` — Java 8 build without Ant/Gradle: `./build.sh`, `./build.sh jar`, `./build.sh run`

Root `changelog`, `copyright`, `gpl.txt` are still 0.8-era; canonical 0.9 `changelog` is in the release zip (`readme.txt` 2019-07-21).

## Why this base
- `master` = 0.8 flat, builds clean on 8 with no deps, but missing 0.9 fixes.
- `origin/xmod` (v0.2.2, 2015-01-27, 64 ahead) = X-Mod gameplay depth: alternative mutations, rates vs probabilities with detailed balance, spontaneous mutations, stress-based aging replacing max-age, min/comfort/body-bound energy, yellow-length energy split. No JTS, packaged as `organisms/segments/world/...` (from `new_design`).
- Skipped: `EfficiencyOptions/TylerEdits` (already in 0.9 via STRtree + FPS fixes), `Rick'sNNMod` (experimental brains/eyes), `ColorMod` 1.32 local (obsolete vs online 2.01), `PortingColorMod` (incomplete WIP port).

## X-Mod port plan (single merged tree)
`origin/xmod` can't fast-forward (packaged vs flat `src/biogenesis/`). Port functional slices from `new_design..xmod` onto flat tree:
1. Mutation rates + detailed balance (`GeneticCode.java`, `Gene.java`)
2. Minor/major mutations: split/merge/swap/flip/add/remove, clone gene
3. Spontaneous mutations + params (`ParamDialog.java`, `World.java`)
4. Stress vs age: stress resistance, min/comfort/body-bound energy, yellow-length split, growth/repro stress gates
5. Fixes: children gene-sharing, border-freeze, div-by-0 stats, clipboard sync mutations, prefs save
6. `readme.txt` (X-Mod) + version bump

Reference: `git show origin/xmod:readme.txt`, `git log --oneline new_design..xmod`, online `x_mod_v0.2.2/readme.txt` + `ColorMod_2_01/` untouched for now.

## Verify (Java 8)
- `javac -cp lib/jts-core-1.15.0.jar -d /tmp/bio_check src/biogenesis/*.java` → 98 classes, unchecked warnings only
- `java -cp classes:lib/jts-core-1.15.0.jar biogenesis.MainWindow`
- Rust port later: this tree is the spec baseline.
