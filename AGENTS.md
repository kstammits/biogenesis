# AGENTS.md — Biogenesis unified tree (`main` branch)

## What this is
Biogenesis artificial-life sim: 0.9 vanilla + X-Mod merged onto Java 17,
JavaFX 21 UI. Sim core and `.bio`/net formats stay
unchanged; UI is ported leaves-first (dialogs → canvas → shell).

## Toolchain
- JDK 17 (`/opt/homebrew/opt/openjdk@17`, `javac --release 17`). No Ant/Gradle.
- Vendored: `lib/jts-core-1.15.0.jar`, `lib/javafx/javafx-*-21-mac-aarch64.jar`
  (base, graphics, controls). mac-aarch64 only.
- `FX_MODULES=javafx.controls,javafx.graphics`.

## Commands (`build.sh` is canonical)
- `./build.sh` — incremental compile (skips when up to date)
- `./build.sh test` — headless gate: `SimSmoke` (500 deterministic ticks,
  asserts population survives) + `FxLogicTest` (save-dialog filename rules)
- `./build.sh bench [...]` — headless scaling benchmark (`test/biogenesis/PerfBench.java`,
  CSV to stdout; e.g. `--ns 50,200,800 --worlds 1000x560,3162x1778 --seeds 3
  --collide both --out scale.csv`). Ad-hoc, never shipped in the jar.
- `./build.sh jar` / `./build.sh run` — self-contained jar (JTS bundled) / launch
- `./build.sh clean` — remove `classes/` + jar
- Before committing: `./build.sh test`, then `rm -rf classes biogenesis.jar`
  so build outputs never land in git.

## Architecture notes
- `WorldView.java` seam: `World`/`Organism` talk to the view only through it.
  Headless tests attach a no-op view.
- `Fx.java` is the FX toolkit helper (`run`/`ask`/alerts/file choosers).
  Its only branchy logic — `withAppendedExt` (save-dialog extension rule
  incl. PNG keep-as-typed) — is covered by `FxLogicTest`. Alert/file-chooser
  wrappers are thin; verify by eyeball.
- Ported windows use `Stage` + `APPLICATION_MODAL`. Established pattern:
  constructor takes parent, `Fx.run(this::buildAndShow)`, OK writes prefs,
   Cancel closes. Preview canvases + `VisibleWorld` render via AWT
   (`World`/`GeneticCode.draw`) into a `BufferedImage` → `ImageFx.toFXImage`
   (manual PixelWriter copy). Karl wants the full `GraphicsContext` renderer
   port later, but the bridge is exact and A/B-benched ~20% faster than the
   previous converter at full-world size.
- `StdAction` is a plain FX command POJO (name/desc/iconPath/accelerator/
   enabled + `perform()`); the UI is pure JavaFX. Remaining AWT is renderers
   only (`Color`/`Graphics`/`BufferedImage` + `ImageFx`/`ImageIO` bridge).
- Serial contract: `FILE_VERSION 800`, `VERSION 902` in `Utils.java`.
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
  about|params|lab`). Snapshot harnesses are fine: `scene.snapshot()` → PNG,
  then `System.exit(0)` on the FX thread — never close the modal cross-thread.
- Ownerless `APPLICATION_MODAL` dialogs break macOS native fullscreen: the child
  opens in another Space (blank slide-out) and red-X close beachballs Glass.
  All dialogs must `initOwner(mainStage)` + `WINDOW_MODAL` (`MainWindow.getStage()`).
- `MainWindow` may only exist once per JVM; test launchers must reuse frames.

## State (update as you go)
- Done: Phase 0 seam, Phase 1 FX toolchain, Phase 2 net windows, dialog sweep,
  `ParamDialog`, `LabWindow`+`ColorComboBox`, headless test gate,
  `StatisticsWindow`+`InfoToolbar`+`GeneticCodePanel` (FX, embedded in MainWindow),
  `VisibleWorld` canvas (Pane+Canvas in FX ScrollPane, coalesced redraw, FX ContextMenus),
   `MainWindow` shell (Stage/MenuBar/ToolBar/ScrollPane, StdAction wrappers).
   UI is pure JavaFX — renderers still on AWT; `ImageFx` manual copy
   (pixel-exact, faster) with no extra jar or module flag.
- Done: FPS fix — `getFPS` now reports measured ticks/sec (was `1000/currentDelay`);
  sim decoupled from paint (`renderIntervalMs=33`, sim every tick, blit at most
  ~30fps); `VisibleWorld` reuses frame buffers + restores efficiency-mode
  viewport culling; governor gets a 40-tick settle cooldown (cold-start false
  positives gone). SimSmoke + FxLogicTest green.
- Done: 10fps-at-50-creatures root cause — NOT physics (headless: 0.15ms/tick
  at N=50, 0.39ms at N=400; AWT draw+copy ~1.2ms/frame flat). It was
  `requestRedraw` running inline on the FX thread: one full render per moved
  organism per tick (~940 renders/s saturating FX, sim starved behind `Fx.ask`).
  Now defers via `runLater` so bursts coalesce to one render; headful bench
  (throwaway `/tmp/dbg/.../FxBench`, 60→153 organisms) holds 154-200fps at
  delay=5, FX queue latency ≤1.4ms. Governor recovery threshold widened to
  symmetric 1.5x (was wedging one step up under FX overhead). SimSmoke trace
  bit-identical (seed 12345: pop=41, same O2/CO2) — sim untouched.
- Done: `./build.sh bench` + `test/biogenesis/PerfBench.java` — headless scaling
  matrix (seeds × N × world-size × collide on/off → CSV: mean/p95/max ms/tick,
  render ms/frame, seg stats, GC time). Findings here: sim ~0.07–0.15ms @50 →
  ~0.35ms @200 (collision ≈ half); render linear in pixels (1.2ms @560k px,
  8.8ms @5.6M px); default world caps ~150 creatures via die-off.
- Done: fullscreen dialog fix — all 5 dialogs (`NetConnections`, `NetConfig`,
  `Param`, `Lab`, `Statistics`) are now owned `WINDOW_MODAL` children of the
  main stage (was ownerless `APPLICATION_MODAL`: blank slide-out + beachball
  on close in macOS fullscreen). `NetConnections` gains a `Close` button
  (was red-X-only) + owned error alert; `Lab` import errors use an owned alert.
  `NetConnections` grid fix: `fitToWidth` + address column absorbs shortfall
  with ellipsis, port/Disconnect pinned to pref (long hostnames clipped
  Disconnect off-panel). All dialogs + all 5 param tabs verified via throwaway
  snapshot harness (`/tmp/dbg/.../ShotAll`). SimSmoke + FxLogicTest green.
- Next: Karl's backlog (FPS limits, zooms, bg color, branching saves — sim-speed
  decoupling done). SimSmoke green each time.

## Backlog (Karl's list — do NOT start unasked)
- FPS limits: governor (`MainWindow.currentDelay`, auto-pause past 1000ms) trips
  on cold starts; revisit caps/floor once the shell is FX.
- Window zooms: `zoomFactor`/`Utils.ZOOM_FACTOR` plumbing is in, but UX (zoom to
  cursor, fit-window, persisted per-world) is unfinished.
- Background color: world canvas is hardcoded black (`VisibleWorld`, panels);
  make it a preference.
- Sim-speed target (decoupled FPS): sim ticks are currently locked to the paint
  loop (`startLifeProcess`/`lifeProcess`); split into independent sim-rate and
  render-rate targets.
- Someday: branching save-files (versioned/forked `.bgw` lineages).
