# YSM Enhancements — Fabric 1.2.0

[中文](README.md)

Install the addon JAR for your Minecraft version and choose one model mod from the table. Official YSM and OpenYSM share the mod ID `yes_steve_model`.

| Minecraft | Model mod | Artifact | Minimum Java | Minimum Fabric Loader |
|---|---|---|---|---|
| 1.20.1 | Official YSM 2.6.5 | `ysm-enhancements-1.2.0-fabric-mc1.20.1.jar` | 17 | 0.15.0 |
| 1.20.1 | OpenYSM 2.6.6.6 | `ysm-enhancements-1.2.0-fabric-mc1.20.1.jar` | 17 | 0.16.10 |
| 1.21.1 | Official YSM 2.6.5 | `ysm-enhancements-1.2.0-fabric-mc1.21.1.jar` | 21 | 0.16.0 |

Version 1.2.0 adds support for OpenYSM menus, controls and mouse shortcuts in the Z panel. Turning the panel off restores the wheel selected in OpenYSM. Both official YSM builds can use the native maid picker with Orihime, the Fabric port of Touhou Little Maid. OpenYSM 2.6.6.6 Fabric supports its native player picker and lacks upstream maid skin integration. [OpenYSM Fabric release](https://modrinth.com/mod/openysm/version/8Yg50ABK) · [Upstream maid integration](https://github.com/OpenYSM/OpenYSM/issues/72)

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

| Item | Forge / NeoForge 1.2.0 | Fabric 1.2.0 |
|---|---|---|
| Main interface | Model-driven Z button panel | The same panel features, adapted to each Fabric target |
| Official YSM fixes | 10 items | The nine items above. Official Fabric YSM handles mouse bindings itself |
| Animation lock | Modifier and interaction-context checks | Also handles Fabric callback repeats and state resynchronization |
| Configuration entry | Configuration screen in the Mods menu | Edit `config/ysm_enhancements-client.toml`; no in-game config screen currently |

Official YSM uses its native Fabric mouse bindings. With OpenYSM, the addon's `mouseShortcuts` setting controls mouse event forwarding.

## Installation

Keep the model mod chosen above and its required dependencies installed. Place the matching addon JAR in the client instance's `mods` folder. Remove the previous addon JAR when upgrading.

The minimum loaders in the table include the model mods' bundled dependencies. A separate Fabric API installation or modpack may require a newer loader.

Settings are registered through YSM's Forge Config API Port dependency and stored in `config/ysm_enhancements-client.toml`. This Fabric build uses file-based configuration; edit it while the game is closed. Set `interface.buttonPanel = false` to restore the original wheel while retaining individual fixes.

## Verification

Both official YSM targets passed real class and Mixin checks, input and slider tests, model expression tests and configuration persistence tests. OpenYSM 2.6.6.6 also passed its own interface, slider and expression checks.

In local clients running official YSM, the selected maid skin was visible in the world and the saved entity retained the model ID. OpenYSM passed player model search, Z panel, model checkbox, mouse shortcut and native wheel restoration checks. [Verification record](VERIFICATION.txt)

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
