package dev.local.ysmenhancements.config;

/** Stable, loader-independent keys. Each switch controls only its own feature. */
public enum Feature {
    BUTTON_PANEL("interface", "buttonPanel", "按钮面板替换轮盘", "Replace the Z radial menu with a model-driven button panel."),
    INPUT_RESET("fixes", "inputReset", "修复按键松开后仍保持", "Clear YSM input on focus loss, GUI opening and physical release."),
    SHORTCUT_CONTEXT("fixes", "shortcutContext", "修复锁定快捷键误触发", "Respect the configured modifier and in-game context for animation lock."),
    MOUSE_SHORTCUTS("fixes", "mouseShortcuts", "修复鼠标侧键快捷键", "Allow YSM shortcuts bound to mouse buttons to trigger."),
    CLIPPED_CONTROLS("fixes", "clippedControls", "修复隐藏控件被点击", "Reject clicks on model controls outside the visible viewport."),
    SCROLL_ARROWS("fixes", "scrollArrows", "修复滚动箭头状态", "Refresh scroll arrow availability after scrolling and loading controls."),
    NUMERIC_VALUES("fixes", "numericValues", "修复科学计数法数值读取", "Accept scientific notation when reading model variables."),
    SCALE_DEFAULTS("fixes", "scaleDefaults", "修复缩放滑条首次显示", "Use explicit zero-fallback constants in model scale expressions for initially out-of-range sliders; do not change model variables when opening the panel."),
    BOOLEAN_VALUES("fixes", "booleanValues", "修复负数开关状态", "Treat nonzero negative model values as checked."),
    SEARCH_CARET("fixes", "searchCaret", "修复搜索光标跳到末尾", "Keep the search caret and selection when rebuilding the model list."),
    STALE_CALLBACKS("fixes", "staleCallbacks", "修复旧回调造成控件重叠", "Discard model-control callbacks belonging to an old screen generation.");

    public final String section, key, label, description;
    Feature(String section, String key, String label, String description) {
        this.section = section; this.key = key; this.label = label; this.description = description;
    }
    public String translationKey() { return "ysm_enhancements.configuration." + key; }
    public boolean enabled() { return NativeConfig.enabled(this); }
}
