package dev.local.ysmenhancements.fabric;

import dev.local.ysmenhancements.compat.Reflect;
import dev.local.ysmenhancements.config.Feature;

/** Allows one lock toggle per physical press, never a repeat or state resynchronization. */
public final class ShortcutGuard {
    private record Event(boolean mouse,int key,int scan,int action) {}
    private static final ThreadLocal<Event> EVENT = new ThreadLocal<>();
    private ShortcutGuard() {}
    public static void begin(boolean mouse,int key,int scan,int action) { EVENT.set(new Event(mouse,key,scan,action)); }
    public static void end() { EVENT.remove(); }
    public static boolean allow(Object binding) {
        if (!Feature.SHORTCUT_CONTEXT.enabled()) return true;
        Event event = EVENT.get();
        if (event == null || event.action != 1) return false;
        Object mc = Reflect.minecraft();
        if (Reflect.get(mc,"screen") != null || Reflect.get(mc,"overlay") != null
            || Reflect.get(mc,"player") == null || !(Boolean)Reflect.call(mc,"isWindowActive")) return false;
        // ModernKeyBinding/Amecs apply their modifier and conflict-context rules
        // through isDown(). Do not bypass those rules with the callback's raw flag.
        if (!(Boolean)Reflect.call(binding,"isDown")) return false;
        boolean matches = (Boolean)(event.mouse ? Reflect.call(binding,"matchesMouse",event.key)
            : Reflect.call(binding,"matches",event.key,event.scan));
        if (matches) EVENT.remove(); // A second dispatch within the same press is not a second toggle.
        return matches;
    }
}
