# YSM Enhancements — Fabric 1.1.0

[中文](README.md)

A client addon for official Yes Steve Model 2.6.5. Core fixes are shared with the Forge / NeoForge implementation; Fabric adapters, tests and artifacts are maintained separately for each supported Minecraft version.

| Minecraft | Required YSM | Artifact | Java | Minimum Fabric Loader |
|---|---|---|---|---|
| 1.20.1 | 2.6.5 Fabric | `ysm-enhancements-1.1.0-fabric-mc1.20.1.jar` | 17+ | 0.15.0 |
| 1.21.1 | 2.6.5 Fabric | `ysm-enhancements-1.1.0-fabric-mc1.21.1.jar` | 21+ | 0.16.0 |

As of November 1, 2026, these are the two Fabric targets provided by official YSM 2.6.5. Its minimum Minecraft version is 1.20.1. Older YSM 2.4.1 releases extend to 1.16.5 and require separate adapters. This table does not imply support for every intervening Minecraft patch release. [Official compatibility list](https://yesstevemodel.github.io/en/wiki/faq/)

## Features and fixes

- Port of the 1.1.0 button panel: automatic layout, full folder paths, rotatable and zoomable model preview, numeric editing, keyboard navigation and fallback to the original wheel.
- Animation lock toggles once per physical press. Repeats, state resynchronization, focus loss and GUI interaction cannot toggle it. Keyboard, scan-code and mouse bindings remain supported; provider modifier filtering is respected.
- Clears stale YSM input after focus loss, GUI opening, cursor release and physical key/button release.
- Fixes clicks on clipped wheel controls, outdated scroll arrows, scientific notation, negative boolean values, lost search caret/selection and stale callbacks from previous screens.
- Initializes out-of-range zero-valued scale sliders from explicit author-defined fallback expressions without modifying model variables.
- Adapts each Fabric slider implementation so typed values and arrow keys apply exactly one model callback.

Fabric YSM already supports mouse-bound shortcuts, so Forge-specific mouse forwarding and its redundant configuration option are omitted.

## Installation

Keep the matching official YSM 2.6.5 and its Fabric API dependency installed. Add exactly one matching addon JAR to the client's `mods` folder. Remove the previous addon JAR when upgrading. No addon installation is needed on the server.

Loader minimums follow the current official artifacts and their bundled dependencies. Both YSM artifacts declare Loader ≥ 0.15.0, but the 1.21.1 artifact bundles Forge Config API Port 21.1.3, which requires Loader ≥ 0.16.0. The effective minimums are therefore 0.15.0 and 0.16.0 respectively. Separately installed Fabric API or other mods may require newer loaders.

Settings are registered through YSM's Forge Config API Port dependency and stored in `config/ysm_enhancements-client.toml`. This Fabric build uses file-based configuration; edit it while the game is closed. Set `interface.buttonPanel = false` to restore the original wheel while retaining individual fixes.

## Verification

Both targets are checked against actual official YSM and Minecraft class files: real Sponge Mixin transformations, operand verification, reflection contracts, actual lock callbacks, actual slider arithmetic/input, real expression parsing and configuration persistence. They compile against Loader 0.15.0 / 0.16.0 APIs and are checked with the corresponding Mixin 0.8.5 / 0.8.7 and ASM 9.6 environments. Panel interaction uses isolated regression tests.

Both full clients were also launched in separate instances, and logs confirm entry into singleplayer worlds and configuration loading. Fabric API 0.90.7+1.20.1 and 0.102.0+1.21.1 were used respectively. The complete panel interaction suite, modpacks, multiplayer and all third-party keybinding provider combinations have not been play-tested. Modifier regression tests simulate provider filtering; they do not execute every ModernKeyBinding or Amecs release.

Initial local launches encountered Fabric API Mixin configuration resource-read failures. Original and generated cache files were inspected, and both instances started successfully after restarting without changing the addon JARs. The root cause remains unconfirmed; this is not listed as a bug fixed by the addon. See the [verification record](VERIFICATION.txt).

## Building

From the repository root, using JDK 21:

```powershell
.\gradlew.bat -p fabric assemble
```

Artifacts are written to `fabric/mc1201/build/libs/` and `fabric/mc1211/build/libs/`. Core source is shared through `src/main/java`; Fabric hooks live in `fabric/common`, with separate version mapping tables. These builds target Fabric's production intermediary namespace rather than a Loom named development environment.

To prepare hash-verified test inputs (approximately 180 MB, stored under ignored `build/fabric-fixtures`) and run all checks:

```powershell
python tools/prepare_fabric_fixtures.py
.\gradlew.bat -p fabric build
python tools/verify_fabric_artifacts.py
```

Tests do not modify the user's game instance, models or saves. Artifacts contain only addon code and a reduced name mapping table, with no Minecraft / YSM classes, native libraries or test substitutes.
