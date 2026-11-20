# AGENTS.md — Biogenesis unified tree (`javafx` branch)

## What this is
Biogenesis artificial-life sim: 0.9 vanilla + X-Mod merged onto Java 17,
mid-migration from Swing to JavaFX 21. Sim core and `.bio`/net formats stay
unchanged; UI is ported leaves-first (dialogs → canvas → shell).

## Toolchain
- JDK 17 (`/opt/homebrew/opt/openjdk@17`, `javac --release 17`). No Ant/Gradle.
- Vendored: `lib/jts-core-1.15.0.jar`, `lib/javafx/javafx-*-21-mac-aarch64.jar`
  (base, graphics, controls, swing). mac-aarch64 only.
- `FX_MODULES=javafx.controls,javafx.graphics,javafx.swing`.

## Commands (`build.sh` is canonical)
- `./build.sh` — incremental compile (skips when up to date)
- `./build.sh test` — headless gate: `SimSmoke` (500 deterministic ticks,
  asserts population survives) + `FxLogicTest` (save-dialog filename rules)
- `./build.sh jar` / `./build.sh run` — self-contained jar (JTS bundled) / launch
- `./build.sh clean` — remove `classes/` + jar
- Before committing: `./build.sh test`, then `rm -rf classes biogenesis.jar`
  so build outputs never land in git.

## Architecture notes
- `WorldView.java` seam: `World`/`Organism` talk to the view only through it.
  Headless tests attach a no-op view.
- `Fx.java` is a TEMPORARY Swing→FX bridge (`run`/`ask`/alerts/file choosers).
  Deleted once `MainWindow` becomes FX. Its only branchy logic —
  `withAppendedExt` (save-dialog extension rule incl. PNG keep-as-typed) — is
  covered by `FxLogicTest`. Alert/file-chooser wrappers are thin; verify by eyeball.
- Ported windows use `Stage` + `APPLICATION_MODAL`. Established pattern:
  constructor takes parent, `Fx.run(this::buildAndShow)`, OK writes prefs,
  Cancel closes. Preview canvases render via `GeneticCode.draw` into a
  `BufferedImage` → `SwingFXUtils.toFXImage` (keep this bridge; port renderers
  to `GraphicsContext` only after `VisibleWorld`/`MainWindow` are done).
- Serial contract: `FILE_VERSION 800`, `VERSION 901` in `Utils.java`.
  i18n via `Messages.java` + 7 `messages*.properties` (keep 0 missing keys).

## Gotchas (learned the hard way)
- DO NOT programmatically close modal FX dialogs cross-thread in tests
  (`stage.close()` from another thread's `Fx.ask`, Robot keypresses, etc.).
  After a `showAndWait` teardown the Glass event loop on macOS nondeterministically
  stops dispatching `runLater` tasks and everything hangs. Dismiss via button
  `.fire()` on the FX thread at most — or better, keep automated tests headless
  and verify dialogs by eyeball. Production (user clicks buttons) is unaffected.
- Headful eyeball flow (throwaway, NEVER commit): `/tmp/dbg/.../DebugDialogs.java`
  boots `MainWindow` and fires one window per mode arg (`config|connections|
  about|params|lab`). Keep verification lightweight — no screenshot harnesses.
- `MainWindow` may only exist once per JVM; test launchers must reuse frames.

## State (update as you go)
- Done: Phase 0 seam, Phase 1 FX toolchain, Phase 2 net windows, dialog sweep,
  `ParamDialog`, `LabWindow`+`ColorComboBox`, headless test gate,
  `StatisticsWindow`+`InfoToolbar`+`GeneticCodePanel` (FX, JFXPanel-hosted toolbar),
  `VisibleWorld` canvas (JFXPanel+Canvas, coalesced redraw, FX ContextMenus).
- Next: `MainWindow` shell (last Swing). One window per commit,
  SimSmoke green each time.
