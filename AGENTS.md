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
- Tests: `lib/junit-4.13.2.jar` + `lib/hamcrest-core-1.3.jar` vendored (JUnit 4,
  single-jar; JUnit 5's multi-jar is overkill here). `build.sh test` runs
  SimSmoke + FxLogicTest mains, then `JUnitCore` with the `test/biogenesis/*Test`
  suite (headless; FX modules stay on the path because tests touch FX classes).

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
- Done: sim-rate presets — `Utils.SIM_RATE_PRESETS` (10/30/60/120/240/500 +
  uncapped→1ms floor), `ParamDialog` dropdown replaces raw ms field, legacy
  DELAY snaps to nearest on load, `applySimDelay` retargets a running sim,
  render locked at `MAX_RENDER_FPS=30`, governor backoff guard can't spin at
  1-2ms. Ticks-per-fire batching (`batchSizeForDelay`: 1:1 up to 60fps, ~16ms
  of sim per FX round-trip above) — uncapped holds 1000tps headful with no
  backoff. Deadline pacing (self-rearming one-shot chain, EWMA overhead
  compensation, rate-derived nominals: batch rule round(25/delay)) — 60/120/240
  hold 59/120/238 headful; FPS readout smoothed + computed over >=250ms.
  New keys `T_SIM_RATE`/`T_TICKS_PER_SECOND`/`T_UNCAPPED` in all 7 locales
  (byte-exact edit, ISO-8859 preserved). RATECHECK (mapping + all-locale keys) +
  SimSmoke + FxLogicTest green; General tab snapshotted.
- Done: chrome fixes — `InfoToolbar` binds managed to visible (invisible panel
  reserved 64px under the world, floating the scrollbar; now collapses to 0),
  `T_MANAGE_CONNECTIONS` shortened to `Connections...` in all 7 locales
  (toolbar row 122→105px, buttons uniform; DE `Verfolgung abbrechen` still
  wraps — accepted residual), status bar rebuilt as `HBox` of locked-width
  right-aligned slots (FPS/Time/Pop/O2/CO2 re-measured per locale) + hgrow
  message tail. EN+DE snapshot-verified at 59/1240fps (`/tmp/dbg/.../ShotChrome`).
- Next: Karl's backlog (bg color, fit-window zoom, branching saves). SimSmoke
  green each time.
- Done: cleanup sweep 1 (safety) — `InCorridor` single-monitor queue,
  `Organism.symmetric` method-local scratch, `World.addOrganism` + net-list
  mutations under their documented locks (check-then-act atomic,
  `closeServer` clears in place so the lock object never swaps),
  `startLifeProcess`/`scheduleNext` synchronized + volatile `timerGen`.
- Done: cleanup sweep 2 (persistence) — XXE-hardened `BioXMLParser`
  (internal DTD kept, external entities/XInclude/DTD/schema blocked),
  `Utils.SERIAL_FILTER` allow-list on all three `ObjectInputStream` sites,
  atomic `.bgw` writes (temp + rename), try-with-resources on load/save/XML,
  `colDetTree` transient + rebuilt in `readObject`, SAXParse-vs-content
  error mapping. Round-trip + reject-all verified headless.
- Done: cleanup sweep 3 (dead weight) — ~37 meaningless `serialVersionUID`s
  removed (sim classes keep `FILE_VERSION`: save compat), mnemonics retired
  (`StdAction` field, `Messages.getMnemonic`, ~343 bundle keys), dead
  `T_BACKUP_*`/`T_GENES3`/`T_NODE`/`T_COLONYPLUS` keys removed (all 7 bundles
  at 358-key parity, runtime-verified), `Fx.parseIntOrKeep/parseDoubleOrKeep`
  for the 35 keep-old-value catches, `Fx.readError` for unowned load errors
  (LabWindow keeps its owned alert), stale Swing comments swept,
  `BareBonesBrowserLaunch` reduced to `Desktop` (no Application subclass, so
  no HostServices), `StatisticsWindow.dispose()` → `close()`.
  `BioXMLParser.colorToString` kept but renamed to `colorToXmlToken`: it emits
  file-format tokens, not display names like `Utils.colorToString`.
- Done: JUnit 4 suite — `SimMathTest` (pacing/clamp math), `DeterminismTest`
  (seed-12345 trace: pop 41), `GeneticCodeXmlTest` (round-trip + XXE
  internal-ok/external-blocked), `WorldSaveTest` (filter round-trip +
  reject-all), `BundleParityTest` (232 keys × 6 locales, count pinned),
  `PrefsRoundTripTest` (all 46 table prefs + geometry on a temp node).
  13 tests green.
- Done: shrink sweep — 123 unused bundle keys × 7 files purged (verified
  no dynamic lookups besides the 7 color keys; parity test pins 232),
  `T_MAGENTA/PINK/ORANGE` orphaned by the `colorToString` removal,
  dead methods/fields/commented blocks gone, single-sentence javadocs
  collapsed to one-liners, restating `@return`s dropped, actions to
  `StdAction.of` + `allActions` loops (`MainWindow` 22 classes→2,
  `VisibleWorld` 13→1, `Fx.menuItemFor/iconView` shared, write-only menu
  fields dropped), `Utils` prefs mirror → `Pref` table with node-param
  overloads, `ParamDialog` gene switch → arrays, `InfoToolbar`/
  `StatisticsWindow` row helpers, `LabWindow` spinner/combo factories,
  `Fx.ownedError` replaces both private alerts. `build.sh test` also passes
  FX flags to FxLogicTest (its `ownedError(Stage)` signature forces FX
  classes at load).
## Backlog (Karl's list — do NOT start unasked)
- FPS limits: DONE via presets (governor kept as backoff-only under the preset
  ceiling, auto-pause past 1000ms kept as safety net).
- Window zooms: continuous Ctrl/Cmd+wheel + pinch to cursor done; remaining:
  fit-window, persisted per-world.
- Background color: world canvas is hardcoded black (`VisibleWorld`, panels);
  make it a preference.
- Sim-speed target (decoupled FPS): sim ticks are currently locked to the paint
  loop (`startLifeProcess`/`lifeProcess`); split into independent sim-rate and
  render-rate targets.
- Someday: branching save-files (versioned/forked `.bgw` lineages).
