package dev.local.ysmenhancements.fixes;

import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.config.Feature;

public final class ShortcutFixes {
    private record Mouse(int button, YsmProfile profile) {}
    private static final ThreadLocal<Mouse> MOUSE = new ThreadLocal<>();
    private ShortcutFixes() {}
    public static boolean deviceMatches(Object mapping,int key,int scan) {
        Mouse mouse=MOUSE.get();
        return mouse==null ? (boolean)Reflect.call(mapping,"matches|m_90832_",key,scan)
                : (boolean)Reflect.call(mapping,"matchesMouse|m_90830_",mouse.button);
    }
    public static boolean matches(Object event, Object mapping) {
        return deviceMatches(mapping,(int)Reflect.call(event,"getKey"),(int)Reflect.call(event,"getScanCode"))
                && modifier(mapping);
    }
    private static boolean modifier(Object mapping) {
        Object modifier=Reflect.call(mapping,"getKeyModifier");
        return modifier.equals(Reflect.call(modifier.getClass(),"getActiveModifier"));
    }
    public static boolean lockMatches(Object mapping, int key, int scan) {
        return lockMatches(mapping,key,scan,null);
    }
    public static boolean openLockMatches(Object mapping, int key, int scan) {
        return lockMatches(mapping,key,scan,YsmProfile.OPEN);
    }
    private static boolean lockMatches(Object mapping, int key, int scan,YsmProfile profile) {
        Mouse mouse=MOUSE.get();
        if (!deviceMatches(mapping,key,scan)) return false;
        if (mouse == null && !Feature.SHORTCUT_CONTEXT.enabled()) return true;
        if (!modifier(mapping) || !(Boolean)Reflect.call(Reflect.call(mapping,"getKeyConflictContext"),"isActive")) return false;
        YsmProfile p = mouse != null ? mouse.profile : profile != null ? profile : profileForModifier(mapping);
        return (Boolean)Reflect.call(Reflect.type(YsmProfile.ROOT+p.keys),p.member("isPlayerReady",p.a));
    }
    private static YsmProfile profileForModifier(Object mapping) {
        return Reflect.call(mapping,"getKeyModifier").getClass().getName().startsWith("net.neoforged.") ? YsmProfile.NEO : YsmProfile.FORGE;
    }
    public static void mouseEvent(Object event) {
        YsmProfile p=event.getClass().getName().startsWith("net.neoforged.") ? YsmProfile.NEO : YsmProfile.FORGE;
        mouseEvent(event,p);
    }
    public static void openMouseEvent(Object event) { mouseEvent(event,YsmProfile.OPEN); }
    private static void mouseEvent(Object event,YsmProfile p) {
        if (!Feature.MOUSE_SHORTCUTS.enabled() || MOUSE.get()!=null || ((Number)Reflect.call(event,"getAction")).intValue()!=1) return;
        if (!(Boolean)Reflect.call(Reflect.type(YsmProfile.ROOT+p.keys),p.member("isPlayerReady",p.a))) return;
        int button=((Number)Reflect.call(event,"getButton")).intValue();
        if (button<0 || button>7) return;
        try {
            Object keyEvent=Reflect.type(p.loaderRoot()+".client.event.InputEvent$Key")
                    .getConstructor(int.class,int.class,int.class,int.class)
                    .newInstance(-1,-1,1,((Number)Reflect.call(event,"getModifiers")).intValue());
            MOUSE.set(new Mouse(button,p));
            for (String handler:p.keyHandlers) Reflect.call(Reflect.type(YsmProfile.ROOT+handler),p.member("onKeyInput",p.a),keyEvent);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("YSM mouse shortcut dispatch",e); }
        finally { MOUSE.remove(); }
    }
}
