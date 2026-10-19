package dev.local.ysmenhancements;

import java.lang.reflect.Field;

/** All writes happen on Minecraft's client/input thread. */
public record CacheBinding(Field keys, Field mouse) {
    public void clearAll() throws IllegalAccessException {
        clear(keys);
        clear(mouse);
    }

    public void releaseKey(int key) throws IllegalAccessException {
        if (key >= 32) release(keys, key);
    }

    public void releaseMouse(int button) throws IllegalAccessException {
        release(mouse, button);
    }

    private static void clear(Field field) throws IllegalAccessException {
        boolean[] state = (boolean[]) field.get(null);
        for (boolean pressed : state) {
            if (pressed) {
                // Publish a fresh array through YSM's volatile reference so
                // animation workers can observe the reset as well.
                field.set(null, new boolean[state.length]);
                return;
            }
        }
    }

    private static void release(Field field, int index) throws IllegalAccessException {
        boolean[] state = (boolean[]) field.get(null);
        if (index < 0 || index >= state.length || !state[index]) return;
        boolean[] released = state.clone();
        released[index] = false;
        field.set(null, released);
    }
}
