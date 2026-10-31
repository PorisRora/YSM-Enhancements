# YSM Enhancements

[中文](README.md)

| Loader | Current release | Downloads and documentation |
|---|---|---|
| Fabric 1.20.1 / 1.21.1 | 1.2.0 | [Download](https://github.com/PorisRora/YSM-Enhancements/releases/tag/v1.2.0) · [Fabric guide](fabric/README.en.md) |
| Forge 1.20.1 / NeoForge 1.21.1 | 1.1.0 | [Download](https://github.com/PorisRora/YSM-Enhancements/releases/tag/v1.1.0) |

The following release notes describe Forge / NeoForge 1.1.0. See the Fabric guide for its minimum loaders, configuration and verification scope.

An independent third-party client patch for official YSM 2.6.5. It adds a model-driven Z button panel and loader-native configuration, and fixes input, shortcut, numeric parsing, and control handling issues. It does not bundle, overwrite, or modify the original YSM JAR, models, or saves. This project is not affiliated with YSM.

## What's new in 1.1.0 (since 1.0.0)

- **Redesigned interface:** A translucent dark panel, blue accents, toggle switches, a model contact shadow, and hover transitions.
- **Adaptive menu layout:** Up to three columns on wide screens, with pagination only when entries do not fit.
- **Improved folder navigation:** The title shows the full folder path, returning from a submenu restores the previous page, and the root back button no longer closes the panel unexpectedly.
- **Keyboard and scrolling controls:** Arrow-key navigation, Page Up and Page Down, and a draggable scrollbar. Keyboard focus scrolls off-screen parameters into view.
- **Improved value entry:** Existing values start selected, floating-point display artifacts are trimmed, and invalid input is highlighted in red.
- **Hidden-control input fix:** The panel handles arrow keys, Enter, and Space so they do not operate the hidden original wheel controls.
- **Hover and directional navigation fixes:** Returning to a page no longer resumes stale hover transitions, and Right correctly reaches a card's adjacent play button.
- **Error fallback:** Errors during panel rendering or input handling are logged, and the screen falls back to the original wheel.
- **Lower per-frame overhead:** Repeated reflection lookups, menu labels, and truncated text are cached.

## Features added to official YSM 2.6.5

- **Z button panel:** An optional replacement for the official radial menu, using the model author's menus, categories, parameters, and actions. Supports menu pagination and parameter scrolling.
- **Model preview controls:** Drag to rotate the entire model, zoom with the mouse wheel or ± buttons, and click the percentage button to reset the preview view and zoom.
- **Parameter entry and keyboard controls:** Enter slider values directly, press Enter to confirm or Esc to cancel, navigate with Tab and the arrow keys, and adjust sliders with Left and Right. These actions preserve the model author's callbacks.
- **Panel navigation and status indicators:** A translucent dark panel that marks selected, enabled, and focused states with a blue accent, with a full folder path in its title row and submenu and return indicators. Independent toggles are drawn as switches, while option groups mark the selected option with an accent border and dot.
- **Loader-native configuration:** Control the button panel and each fix independently through the Mods configuration screen. All options are enabled by default. Disable the button panel to use the official radial menu.

## Fixes to official YSM 2.6.5

1. **Stuck inputs:** Clear stale held-key states after focus loss, opening a screen, or releasing an input, preventing keys such as Ctrl from remaining active.
2. **Unintended animation-lock shortcuts:** Respect the configured modifier keys and active gameplay context.
3. **Mouse-bound shortcuts:** Allow YSM shortcuts assigned to mouse buttons to trigger the existing action logic.
4. **Clickable hidden controls:** Reject clicks on model controls outside the official radial menu's visible scroll area.
5. **Incorrect scroll arrow availability:** Refresh the arrows after scrolling or asynchronous control loading.
6. **Scientific notation parsing:** Correctly read model variable values such as `1e-5`.
7. **Incorrect initial scale slider display:** When an initial zero is outside the slider range, use an explicit fallback constant from the model's scale expression. For example, `v.roaming.player_size==0?1:v.roaming.player_size` initially displays 1. Works with both the official radial menu and the button panel, without changing model variables or sending synchronization when the screen opens.
8. **Negative checkbox values:** Display negative nonzero model variables as checked.
9. **Lost search caret and selection:** Preserve the caret, selection, and text viewport when rebuilding model search results.
10. **Overlapping controls from delayed callbacks:** Discard asynchronous control callbacks belonging to an outdated configuration page or a closed screen.

## Release information

- Release type: stable; tag: `v1.1.0`.
- Mod ID: `ysm_enhancements`; version: `1.1.0`.
- Download: `ysm-enhancements-1.1.0-forge-neoforge.jar`.
- Configuration: `config/ysm_enhancements-client.toml`.

## Supported environments

The same JAR supports these two combinations:

| Minecraft | Loader | YSM | Loader used for local verification |
|---|---|---|---|
| 1.21.1 | NeoForge 21.1.x | 2.6.5 NeoForge | 21.1.264 (1.0.0: 21.1.249) |
| 1.20.1 | Forge 47.x | 2.6.5 Forge | 47.4.16 (verified for 1.0.0; not rerun for 1.1.0) |

This combined JAR does not support Fabric; use the [separate Fabric builds](fabric/README.en.md). Other Minecraft versions, NeoForge for Minecraft 1.20.1, and other YSM versions are not supported. Use the Java version required by your Minecraft version. The Forge / NeoForge patch targets Java 17 bytecode and requires JDK 21 to build.

## Installation and configuration

1. Fully exit the game. Remove any `ysm-input-reset-*.jar` or other `ysm-enhancements-*.jar` already installed in the instance to avoid loading duplicate patches.
2. Keep official YSM 2.6.5 installed and place the patch JAR in the same instance's `mods` folder.
3. Start the game and open Mods → YSM Enhancements → Config.

NeoForge 1.21.1 uses the loader's ConfigurationScreen. Forge 1.20.1 uses a vanilla-widget screen in the same Mods configuration entry; press Done to save. No additional configuration-screen mod is needed.

Settings are saved through the loader's CLIENT configuration system. Reopen the Z panel after saving. Disabling the button panel restores the official radial menu while allowing the other fixes to remain enabled. Only the client needs this patch. To uninstall, exit the game and remove the patch JAR.

## Configuration options

| Option | Behavior |
|---|---|
| interface.buttonPanel | Replace the Z radial menu with a model-driven button panel |
| fixes.inputReset | Clear stale held-key and mouse-button states |
| fixes.shortcutContext | Respect animation-lock shortcut modifiers and gameplay context |
| fixes.mouseShortcuts | Dispatch mouse-bound shortcuts to the existing YSM action logic |
| fixes.clippedControls | Reject clicks on hidden controls outside the official radial menu's visible area |
| fixes.scrollArrows | Refresh arrow availability after scrolling or asynchronous loading |
| fixes.numericValues | Read scientific notation correctly |
| fixes.scaleDefaults | Initialize out-of-range zero scale slider displays from explicit model fallback constants |
| fixes.booleanValues | Display negative nonzero values as checked |
| fixes.searchCaret | Preserve the search caret, selection, and text viewport |
| fixes.staleCallbacks | Discard control callbacks from outdated pages or closed screens |

## Using the button panel

- The panel adapts to the GUI resolution, up to about 820×480 as a centered dialog at small GUI scales, and keeps its outer dimensions when changing categories, pages, or configurations.
- A soft shadow frames the translucent dark panel, whose toolbar, content, and bottom controls form three aligned rows. Hover and state changes fade briefly. The model preview occupies approximately half of the panel and stands on a lighter backdrop with a contact shadow, so dark models keep their outline. The animation-lock control in the top left is a switch. Drag horizontally to rotate and vertically to adjust pitch within ±45°. The mouse wheel and ± buttons zoom; the percentage button resets the preview. Hovering over empty preview space shows a short controls hint.
- Menu labels come from the model. Menus fill the available height, using up to three columns on wide screens, and paginate only when entries do not fit; the pager shares the zoom buttons' row. Parameter lists scroll, and their scrollbar can be clicked or dragged.
- The title row shows the full folder path (for example, “根目录 › 动态动作”), dropping leading folders when it is too long. Cards marked with `›` open a configuration or submenu; `‹` indicates a return action. A configuration card's play button triggers the author's associated action. The back button returns one level and is disabled at the root; use × to close the panel. Returning from a submenu reopens the page that led to it.
- Click a slider value to type: the value starts selected, so typing replaces it. Press Enter to confirm or Esc to cancel; unparseable values are tinted red. Tab and Shift+Tab move through targets in order, and the arrow keys move to the nearest target in that direction. On a slider, Left and Right adjust the value and Enter starts value entry. Page Up and Page Down change menu pages or scroll the parameter list. Keyboard focus scrolls clipped parameters into view.
- Independent toggles (switches, blue when on) and option groups reflect YSM's actual control state. The model author determines what a checked option does; it does not necessarily mean that a part is visible. While variables load, the list shows a loading message.
- Rebuilding the same parameter page preserves its list, scroll position, and keyboard focus while temporarily blocking stale controls. A different page starts at the top.
- Mouse clicks do not leave a focus outline; keyboard navigation displays a focus indicator. Panel buttons and sliders play the vanilla click sound. The panel handles the arrow keys, Enter, and Space itself, so they never reach the hidden original wheel controls. If the panel fails while drawing or handling input, it logs the error and falls back to the original wheel for that screen. Preview rendering restores the player's original pose, including on failure.

The preview uses the game's entity renderer and estimates its initial scale from the entity's bounding box. Large accessories or exaggerated animations may require manual zoom adjustment. Fixed panel labels are currently in Chinese; model labels use the model's own translations.

## Model parameters and defaults

The scale display fix reads explicit author-defined rules of the form “use a constant when the variable is zero, otherwise use the variable,” including reversed equality, inequality, and arithmetic combinations. Existing nonzero values, selectable zero values, ambiguous or conflicting rules, complex conditions, functions, and assignments retain their original behavior. It does not force every zero to 1 or infer part visibility.

There is no universal “reset all author defaults” action: the model format does not supply a complete common default definition. Resetting the preview changes only its view and zoom. This patch does not include fixes for roaming synchronization limits, high-level potion synchronization overflow, or SlashBlade-related issues.

## Verification

For 1.1.0, compilation, actual class transformations and Mixin injection, reflection contracts, slider behavior, model expressions, panel interaction, and OpenGL preview depth handling were checked on NeoForge 21.1.264. The Forge 1.20.1 tests and the native configuration read/write test were not rerun for this version; they passed for 1.0.0. See [the verification record](VERIFICATION.txt) for environments and details.

This release has not yet been launched in a complete Minecraft modpack for in-game visual verification.

For manual verification, check that the Mods configuration entry works, that disabling the button panel restores the radial menu, and that dragging, typing, and arrow-key slider adjustments change both the displayed value and the model. Test focus loss while holding Ctrl, rapidly switch parameter pages, and rotate the preview to inspect its sides and back. Keep `logs/latest.log` when reporting errors.

## Building and testing

Use JDK 21 and the included Gradle 9.5.1 wrapper. The first build downloads Gradle, Mixin, and ASM dependencies. Set `minecraftLibraries` to a local Minecraft libraries directory containing the Forge 1.20.1 and NeoForge 1.21.1 APIs listed in `build.gradle`. These dependencies are not packaged in the patch.

```powershell
.\gradlew.bat build '-PminecraftLibraries=C:/your-instance/.minecraft/libraries'
```

The test properties refer to the original YSM JAR (`ysmJar`), the loader-patched client JAR (`minecraftJar`), the full base client JAR (`minecraftBaseJar`), and the loader universal JAR (`loaderApiJar`). Set `configLoader` to `neo` or `forge`.

```powershell
.\gradlew.bat regressionTest clientPatchTest mixinSmokeTest apiContractTest sliderContractTest nativeConfigTest previewInteractionTest previewDepthTest scaleDefaultsTest `
  '-PminecraftLibraries=C:/your-instance/.minecraft/libraries' `
  '-PysmJar=C:/jars/ysm.jar' `
  '-PminecraftJar=C:/jars/loader-client.jar' `
  '-PminecraftBaseJar=C:/jars/client-srg.jar' `
  '-PloaderApiJar=C:/jars/loader-universal.jar' '-PconfigLoader=neo'
```

For Forge 1.20.1 Mixin testing, also supply `-PlegacyMixinJar=C:/jars/mixin-0.8.5.jar`. Optionally use `-PmodelZip=C:/models/model.zip` for read-only checks of model scale expressions. The tests do not start Minecraft, execute YSM's native model library, or read worlds. The depth test runs OpenGL in a temporary hidden GLFW window; the current test configuration uses Windows native libraries. The default `build` runs input regression tests; run the listed tasks explicitly for the other checks.

## License

The patch code is licensed under MIT. YSM and Minecraft retain their own licenses. Source packages do not contain their original or decompiled source code or model resources.
