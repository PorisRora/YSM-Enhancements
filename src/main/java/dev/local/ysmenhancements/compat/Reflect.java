package dev.local.ysmenhancements.compat;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/** Cached lookup of the two supported runtime namespaces. */
public final class Reflect {
    private static final ConcurrentHashMap<String, Class<?>> TYPES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHODS = new ConcurrentHashMap<>();
    private Reflect() {}
    public static Class<?> type(String name) {
        Class<?> type = TYPES.get(name);
        if (type != null) return type;
        try { type = Class.forName(RuntimeNames.type(name)); } catch (ClassNotFoundException e) { throw new IllegalStateException(name, e); }
        TYPES.put(name, type);
        return type;
    }
    public static Field field(Class<?> type, String names) {
        return FIELDS.computeIfAbsent(type.getName() + ":" + names, key -> {
            for (String name : names.split("\\|")) for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                try { Field f = c.getDeclaredField(RuntimeNames.field(c,name)); f.setAccessible(true); return f; }
                catch (NoSuchFieldException ignored) { }
            }
            throw new IllegalStateException("Missing field " + key);
        });
    }
    public static Object get(Object target, String names) {
        try { return field(target instanceof Class<?> c ? c : target.getClass(), names).get(target instanceof Class<?> ? null : target); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static int integer(Object target, String name) { return ((Number)get(target, name)).intValue(); }
    public static void set(Object target, String names, Object value) {
        try { field(target instanceof Class<?> c ? c : target.getClass(), names).set(target instanceof Class<?> ? null : target, value); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
    }
    public static Object call(Object target, String names, Object... args) { return invoke(method(target, names, args), target, args); }
    /** Resolves like {@link #call}; hot paths keep the result instead of looking it up per call. */
    public static Method method(Object target, String names, Object... args) {
        Class<?> owner = target instanceof Class<?> c ? c : target.getClass();
        StringBuilder key = new StringBuilder(owner.getName()).append(':').append(names);
        for (Object arg : args) key.append(':').append(arg == null ? "null" : arg.getClass().getName());
        return METHODS.computeIfAbsent(key.toString(), k -> {
            for (String name : names.split("\\|")) for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (RuntimeNames.methodMatches(m,name) && !m.isBridge() && compatible(m.getParameterTypes(), args)) {
                        m.setAccessible(true); return m;
                    }
                }
            }
            for (Method m : owner.getMethods()) if (Arrays.stream(names.split("\\|")).anyMatch(n -> RuntimeNames.methodMatches(m,n)) && compatible(m.getParameterTypes(), args)) return m;
            throw new IllegalStateException("Missing method " + k);
        });
    }
    public static Object invoke(Method method, Object target, Object... args) {
        try { return method.invoke(target instanceof Class<?> ? null : target, args); }
        catch (ReflectiveOperationException e) {
            Throwable cause = e instanceof InvocationTargetException i ? i.getCause() : e;
            throw new IllegalStateException(method.getDeclaringClass().getName() + "." + method.getName(), cause);
        }
    }
    private static boolean compatible(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t.isPrimitive()) t = t == int.class ? Integer.class : t == double.class ? Double.class : t == float.class ? Float.class : t == boolean.class ? Boolean.class : t == long.class ? Long.class : t == char.class ? Character.class : t;
            if (args[i] == null ? types[i].isPrimitive() : !t.isInstance(args[i])) return false;
        }
        return true;
    }
    public static Object minecraft() { return call(type("net.minecraft.client.Minecraft"), "getInstance|m_91087_"); }
    public static String text(Object component) { return (String)call(component, "getString"); }
}
