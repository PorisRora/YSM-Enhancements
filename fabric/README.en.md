# YSM Enhancements — Fabric 1.1.0

[中文](README.md)

An independent third-party client addon for official YSM 2.6.5. It adds a model-driven Z button panel and fixes input state, animation lock, numeric parsing and control handling. It does not bundle, overwrite or modify the original YSM JAR, models or saves. This project is not affiliated with YSM.

| Minecraft | Required YSM | Artifact | Java | Minimum Fabric Loader |
|---|---|---|---|---|
| 1.20.1 | 2.6.5 Fabric | `ysm-enhancements-1.1.0-fabric-mc1.20.1.jar` | 17+ | 0.15.0 |
| 1.21.1 | 2.6.5 Fabric | `ysm-enhancements-1.1.0-fabric-mc1.21.1.jar` | 21+ | 0.16.0 |

Support follows current official YSM 2.6.5 and is limited to the two environments listed above, not every 1.20.x / 1.21.x release. [Official compatibility list](https://yesstevemodel.github.io/en/wiki/faq/)

## Features added over official YSM 2.6.5

- **Z button panel**: Replaces the official radial wheel with menus, folders, parameters and actions defined by the current model. Layout adapts to available space, uses up to three columns and paginates only when needed. Long parameter lists support scrolling and scrollbar dragging.
- **Model preview controls**: Drag to rotate the whole model, zoom with the wheel or +/- buttons, and click the percentage to reset the preview. A ground shadow helps position the model visually.
- **Numeric editing and keyboard controls**: Slider values can be edited directly, with the original text selected, concise decimal formatting and invalid-input highlighting. Enter confirms and Esc cancels. Tab, arrow keys and PageUp/PageDown are supported, with automatic scrolling to the focused control. Numeric edits and left/right keys apply exactly one model callback; panel input does not activate hidden wheel controls.
- **Navigation and state display**: A translucent dark panel with blue accents, switches and hover transitions. Full folder paths and `›` / `‹` indicators show navigation; returning from a subfolder restores its page, and the root back button does not accidentally close the panel. Choice groups and keyboard focus have distinct indicators.
- **Independent feature settings**: The panel and fixes are enabled by default and can be toggled independently in the configuration file. Disabling the panel restores the official wheel while retaining other enabled fixes.
- **Fallback and lower frame overhead**: Rendering or input errors are logged and fall back to the official wheel. Reflection lookups, menu labels and text fitting are cached to reduce per-frame work.

## Official issues fixed

These nine fixes use official YSM 2.6.5 Fabric as the baseline:

1. **Stuck input state**: Clears cached YSM key/button states after focus loss, GUI opening, cursor release and physical release, preventing keys such as Ctrl from remaining active.
2. **Accidental animation-lock toggles**: Toggles once per physical press, rejecting repeats, state resynchronization, focus loss and GUI input while respecting provider modifier filtering. Keyboard, scan-code and mouse bindings are supported.
3. **Clickable hidden controls**: Rejects clicks on model controls outside the official wheel's visible scrolling area.
4. **Incorrect scroll-arrow state**: Refreshes arrow availability after scrolling and asynchronous control loading.
5. **Scientific-notation parsing**: Correctly reads model values such as `1e-5`.
6. **Incorrect initial scale-slider display**: When a zero initial value falls outside the slider range, uses explicit fallback constants in the model's scale expressions. For example, `v.roaming.player_size==0?1:v.roaming.player_size` initially displays 1. Works in both interfaces without writing model variables or sending synchronization on opening. Existing nonzero values, selectable zero, complex conditions and conflicting rules retain their original behavior.
7. **Incorrect negative boolean state**: Displays negative nonzero model variables as checked.
8. **Lost search caret and selection**: Preserves the caret, selection and visible text position when rebuilding the model search list.
9. **Overlapping controls from delayed callbacks**: Discards callbacks from an older configuration page or a closed screen.

## Differences from Forge / NeoForge

| Item | Forge / NeoForge 1.1.0 | Fabric 1.1.0 |
|---|---|---|
| Main interface | Model-driven Z button panel | The same panel features, adapted to each Fabric target |
| Fixes | 10 items | The nine items above; mouse shortcuts are already supported upstream |
| Animation lock | Modifier and interaction-context checks | Also handles Fabric callback repeats and state resynchronization |
| Configuration entry | Configuration screen in the Mods menu | Edit `config/ysm_enhancements-client.toml`; no in-game config screen currently |

Fabric does not inject duplicate mouse-event forwarding or expose the redundant `mouseShortcuts` setting.

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
