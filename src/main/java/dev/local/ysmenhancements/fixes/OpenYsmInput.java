package dev.local.ysmenhancements.fixes;

import dev.local.ysmenhancements.compat.Reflect;
import dev.local.ysmenhancements.compat.RuntimeNames;
import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.config.Feature;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mouse translation for the named Architectury implementation's keyboard-only action callbacks. */
public final class OpenYsmInput {
    private static final CopyOnWriteArrayList<Object> KEY_ACTIONS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Integer> MOUSE = new ThreadLocal<>();
    private OpenYsmInput() {}

    public static void rememberKeyAction(Object action) { KEY_ACTIONS.addIfAbsent(action); }

    public static boolean useModernWheel(boolean configured) {
        return configured && !Feature.BUTTON_PANEL.enabled();
    }

    public static boolean rawMatches(Object binding,int key,int scan) {
        Integer button = MOUSE.get();
        if (button == null)
            return (boolean)Reflect.call(Reflect.type("rip.ysm.api.client.KeyMappingFactory"),"isActiveAndMatches",binding,key,scan);
        if (!(boolean)Reflect.call(binding,"matchesMouse|m_90830_",button)) return false;
        if (RuntimeNames.fabric()) return true;
        Object modifier=Reflect.call(binding,"getKeyModifier");
        return modifier.equals(Reflect.call(modifier.getClass(),"getActiveModifier"))
                && (boolean)Reflect.call(Reflect.call(binding,"getKeyConflictContext"),"isActive");
    }

    public static boolean lockMatches(Object binding,int key,int scan) {
        if (MOUSE.get()==null && !Feature.SHORTCUT_CONTEXT.enabled())
            return (boolean)Reflect.call(binding,"matches|m_90832_",key,scan);
        if (!ready() || !rawMatches(binding,key,scan)) return false;
        return RuntimeNames.fabric() || (boolean)Reflect.call(Reflect.call(binding,"getKeyConflictContext"),"isActive");
    }

    public static void onMouse(Object client,int button,int action,int modifiers) {
        if (action!=1 || button<0 || button>7 || MOUSE.get()!=null || !Feature.MOUSE_SHORTCUTS.enabled() || !ready()) return;
        MOUSE.set(button);
        try {
            for (Object callback : KEY_ACTIONS) Reflect.call(callback,"keyPressed",client,-1,-1,action,modifiers);
        } finally { MOUSE.remove(); }
    }
    private static boolean ready() {
        return (boolean)Reflect.call(Reflect.type(YsmProfile.ROOT+YsmProfile.OPEN.keys),"isPlayerReady");
    }
}
