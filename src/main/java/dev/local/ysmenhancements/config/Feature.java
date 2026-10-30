package dev.local.ysmenhancements.config;

/** Stable, loader-independent keys. Each switch controls only its own feature. Labels live in the lang files. */
public enum Feature {
    BUTTON_PANEL("interface", "buttonPanel", "Replace the Z radial menu with a model-driven button panel."),
    INPUT_RESET("fixes", "inputReset", "Clear YSM input on focus loss, GUI opening and physical release."),
    SHORTCUT_CONTEXT("fixes", "shortcutContext", "Respect the configured modifier and in-game context for animation lock."),
    MOUSE_SHORTCUTS("fixes", "mouseShortcuts", "Allow YSM shortcuts bound to mouse buttons to trigger."),
    CLIPPED_CONTROLS("fixes", "clippedControls", "Reject clicks on model controls outside the visible viewport."),
    SCROLL_ARROWS("fixes", "scrollArrows", "Refresh scroll arrow availability after scrolling and loading controls."),
    NUMERIC_VALUES("fixes", "numericValues", "Accept scientific notation when reading model variables."),
    SCALE_DEFAULTS("fixes", "scaleDefaults", "Use explicit zero-fallback constants in model scale expressions for initially out-of-range sliders; do not change model variables when opening the panel."),
    BOOLEAN_VALUES("fixes", "booleanValues", "Treat nonzero negative model values as checked."),
    SEARCH_CARET("fixes", "searchCaret", "Keep the search caret and selection when rebuilding the model list."),
    STALE_CALLBACKS("fixes", "staleCallbacks", "Discard model-control callbacks belonging to an old screen generation.");

    public final String section, key, description;
    Feature(String section, String key, String description) {
        this.section = section; this.key = key; this.description = description;
    }
    public String translationKey() { return "ysm_enhancements.configuration." + key; }
    public boolean enabled() { return NativeConfig.enabled(this); }
}
