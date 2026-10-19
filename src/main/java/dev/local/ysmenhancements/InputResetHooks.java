package dev.local.ysmenhancements;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/** Client-only bridge. Does not change vanilla keys, send packets or poll GLFW. */
public final class InputResetHooks {
    private static final System.Logger LOG = System.getLogger("ysm_enhancements");
    private static volatile CacheBinding binding;
    private static boolean disabled;
    private static long nextLookup;

    private InputResetHooks() {}

    public static void onFocus(boolean focused) {
        if (!focused) clearAll();
    }

    public static void onGui(Object newGui) {
        if (newGui != null) clearAll();
    }

    public static void onScreen(Object nextScreen) {
        dev.local.ysmenhancements.fixes.ClientFixes.screenChanging(nextScreen);
        onGui(nextScreen);
    }

    public static void onKey(int key, int action) {
        if (action == 0) apply(cache -> cache.releaseKey(key));
    }

    public static void onMouse(int button, int action) {
        if (action == 0) apply(cache -> cache.releaseMouse(button));
    }

    public static void clearAll() {
        apply(CacheBinding::clearAll);
    }

    private static void apply(CacheOperation operation) {
        if (!dev.local.ysmenhancements.config.Feature.INPUT_RESET.enabled()) return;
        CacheBinding current = resolve();
        if (current == null) return;
        try {
            operation.run(current);
        } catch (ReflectiveOperationException | RuntimeException error) {
            disable(error);
        }
    }

    private static CacheBinding resolve() {
        if (disabled) return null;
        if (binding != null) return binding;
        long now = System.nanoTime();
        if (now < nextLookup) return null;
        nextLookup = now + 1_000_000_000L;
        try {
            // Both loaders keep these public APIs. No loader or Minecraft
            // classes occur in this bridge's binary method signatures.
            ClassLoader loader = InputResetHooks.class.getClassLoader();
            Object modList = findModList(loader);
            if (modList == null) return null; // Minecraft can call hooks during bootstrap.
            var getFile = modList.getClass().getMethod("getModFileById", String.class);
            Object fileInfo = getFile.invoke(modList, "yes_steve_model");
            if (fileInfo == null) return null;
            Object file = fileInfo.getClass().getMethod("getFile").invoke(fileInfo);
            Path modPath = (Path) file.getClass().getMethod("getFilePath").invoke(file);
            CacheDescriptor descriptor = CacheDescriptor.discover(modPath);
            binding = descriptor.bind(loader);
            LOG.log(System.Logger.Level.INFO, "[YSM Enhancements] Bound keyboard/mouse cache: " + descriptor.owner());
            return binding;
        } catch (ReflectiveOperationException | java.io.IOException | RuntimeException | LinkageError error) {
            disable(error);
            return null;
        }
    }

    private static Object findModList(ClassLoader loader) throws ReflectiveOperationException {
        for (String name : new String[]{"net.neoforged.fml.ModList", "net.minecraftforge.fml.ModList"}) {
            try {
                return Class.forName(name, true, loader).getMethod("get").invoke(null);
            } catch (ClassNotFoundException ignored) {
                // The other loader name is expected to be absent.
            }
        }
        throw new ClassNotFoundException("Neither NeoForge nor Forge ModList is available");
    }

    private static void disable(Throwable error) {
        disabled = true;
        binding = null;
        if (error instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            error = invocation.getCause();
        }
        LOG.log(System.Logger.Level.ERROR,
                "[YSM Enhancements] PATCH DISABLED: could not access a supported YSM input cache. "
                        + "Check the YSM version and include this error in a bug report.", error);
    }

    @FunctionalInterface
    private interface CacheOperation {
        void run(CacheBinding cache) throws ReflectiveOperationException;
    }
}
