package dev.local.ysmenhancements.fixes;

import dev.local.ysmenhancements.config.Feature;
import dev.local.ysmenhancements.compat.Reflect;
import dev.local.ysmenhancements.compat.YsmProfile;
import java.lang.ref.WeakReference;
import java.util.*;

public final class ClientFixes {
    private static final Map<Object, Set<int[]>> GENERATIONS = new WeakHashMap<>();
    private static final Map<Object, int[]> CARETS = new WeakHashMap<>();
    private static WeakReference<Object> active = new WeakReference<>(null);
    private ClientFixes() {}
    public static boolean enabled(String feature) { return Feature.valueOf(feature).enabled(); }
    public static float number(String text) {
        if (text == null || text.equals("null")) return 0;
        // Preserve the boolean spellings accepted by the original BooleanUtils path.
        if (Set.of("true","on","yes","y","t").contains(text.toLowerCase(Locale.ROOT))) return 1;
        try { float value = Float.parseFloat(text); return Float.isFinite(value) ? value : 0; }
        catch (NumberFormatException ignored) { return 0; }
    }
    public static boolean truth(float value) {
        return Feature.BOOLEAN_VALUES.enabled() ? !Float.isNaN(value) && value != 0 : value > 0;
    }
    public static synchronized void begin(Object screen) {
        GENERATIONS.put(screen, Collections.newSetFromMap(new IdentityHashMap<>()));
        active = new WeakReference<>(screen);
    }
    public static synchronized void track(Object screen, int[] token) {
        GENERATIONS.computeIfAbsent(screen, key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(token);
    }
    public static synchronized boolean current(Object screen, int[] token) {
        return !Feature.STALE_CALLBACKS.enabled() || GENERATIONS.getOrDefault(screen, Set.of()).contains(token);
    }
    public static synchronized void screenChanging(Object next) {
        Object previous = active.get();
        if (previous != null && previous != next) GENERATIONS.remove(previous);
    }
    public static void arrows(Object screen) {
        if (!Feature.SCROLL_ARROWS.enabled()) return;
        YsmProfile p = YsmProfile.of(screen);
        int pos = Reflect.integer(screen,p.scroll), max = Reflect.integer(screen,p.max);
        Object up=Reflect.get(screen,p.up), down=Reflect.get(screen,p.down);
        if (up!=null) Reflect.set(up,"active|f_93623_",pos>0);
        if (down!=null) Reflect.set(down,"active|f_93623_",pos<max);
    }
    public static boolean clippedClick(Object widget, double x, double translatedY, int button, Object screen, double y) {
        if (Feature.CLIPPED_CONTROLS.enabled()) {
            YsmProfile p = YsmProfile.of(screen);
            if (Reflect.type(YsmProfile.ROOT+p.marker).isInstance(widget)) {
                int base=Reflect.integer(screen,p.y);
                if (y<base-46 || y>=base+110) return false;
            }
        }
        return (Boolean)Reflect.call(widget,"mouseClicked|m_6375_",x,translatedY,button);
    }
    public static void captureCaret(Object screen) {
        if (!Feature.SEARCH_CARET.enabled()) return;
        Object box = Reflect.get(screen,YsmProfile.of(screen).search);
        if (box != null) CARETS.put(screen,new int[]{Reflect.integer(box,"cursorPos|f_94101_"),Reflect.integer(box,"highlightPos|f_94102_"),Reflect.integer(box,"displayPos|f_94100_")});
    }
    public static void restoreCaret(Object screen) {
        int[] saved=CARETS.remove(screen);
        if (saved == null || !Feature.SEARCH_CARET.enabled()) return;
        Object box=Reflect.get(screen,YsmProfile.of(screen).search);
        int length=((String)Reflect.call(box,"getValue|m_94155_")).length();
        Reflect.set(box,"cursorPos|f_94101_",Math.min(length,saved[0]));
        Reflect.set(box,"highlightPos|f_94102_",Math.min(length,saved[1]));
        Reflect.set(box,"displayPos|f_94100_",Math.min(length,saved[2]));
    }
}
