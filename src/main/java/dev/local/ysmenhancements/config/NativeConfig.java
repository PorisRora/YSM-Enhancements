package dev.local.ysmenhancements.config;

import java.lang.reflect.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Registers an actual loader CLIENT config, not an independent settings file. */
public final class NativeConfig {
    public static final String FILE = "ysm_enhancements-client.toml";
    private static volatile Object spec;
    private static final Map<Feature, Object> VALUES = new EnumMap<>(Feature.class);
    private static Method loaded, get, set, save;
    private static boolean fabric;
    private NativeConfig() {}

    /** Fabric YSM already depends on Forge Config API Port; use its native registry. */
    public static synchronized void registerFabric(boolean modern) {
        if (spec != null) throw new IllegalStateException("YSM patch config registered twice");
        try {
            fabric = true;
            String root = modern ? "net.neoforged" : "net.minecraftforge";
            Class<?> api = Class.forName(modern
                ? "fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry"
                : "fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry");
            Class<?> type = Class.forName(root+".fml.config.ModConfig$Type");
            Object built = prepare(modern);
            api.getMethod("register",String.class,type,Class.forName(root+".fml.config.IConfigSpec"),String.class)
                .invoke(api.getField("INSTANCE").get(null),"ysm_enhancements",type.getField("CLIENT").get(null),built,FILE);
            spec = built;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not register Fabric YSM patch configuration",unwrap(error));
        }
    }

    public static synchronized void register(boolean neo, Object container) {
        if (spec != null) throw new IllegalStateException("YSM patch config registered twice");
        try {
            String root = neo ? "net.neoforged" : "net.minecraftforge";
            Object built = prepare(neo);
            Class<?> type = Class.forName(root + ".fml.config.ModConfig$Type");
            Object client = type.getField("CLIENT").get(null);
            Class<?> configSpec = Class.forName(root + ".fml.config.IConfigSpec");
            Object registration = neo ? container : Class.forName(root + ".fml.ModLoadingContext").getMethod("get").invoke(null);
            registration.getClass().getMethod("registerConfig", type, configSpec, String.class)
                    .invoke(registration, client, built, FILE);
            spec = built;
            Object dist = Class.forName(root + ".fml.loading.FMLEnvironment").getField("dist").get(null);
            if ("CLIENT".equals(dist.toString())) registerScreen(neo, container, registration);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not register the loader-native YSM patch configuration", unwrap(error));
        }
    }

    static Object prepare(boolean neo) throws ReflectiveOperationException {
        String specName = neo ? "net.neoforged.neoforge.common.ModConfigSpec" : "net.minecraftforge.common.ForgeConfigSpec";
        Class<?> builderClass = Class.forName(specName + "$Builder");
        Object builder = builderClass.getConstructor().newInstance();
        for (Feature feature : Feature.values()) {
            if (fabric && feature == Feature.MOUSE_SHORTCUTS) continue; // Already supported by Fabric YSM.
            builderClass.getMethod("comment", String.class).invoke(builder, feature.description);
            builderClass.getMethod("translation", String.class).invoke(builder, feature.translationKey());
            Object value = builderClass.getMethod("define", String.class, boolean.class)
                    .invoke(builder, feature.section + "." + feature.key, true);
            VALUES.put(feature, value);
        }
        Object built = builderClass.getMethod("build").invoke(builder);
        Class<?> configValue = Class.forName(specName + "$ConfigValue");
        get = configValue.getMethod("get"); set = configValue.getMethod("set", Object.class);
        loaded = built.getClass().getMethod("isLoaded"); save = built.getClass().getMethod("save");
        return built;
    }

    private static void registerScreen(boolean neo, Object container, Object registration) throws ReflectiveOperationException {
        if (neo) {
            Class<?> api = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Class<?> screen = Class.forName("net.minecraft.client.gui.screens.Screen");
            Constructor<?> ctor = Class.forName("net.neoforged.neoforge.client.gui.ConfigurationScreen", false, NativeConfig.class.getClassLoader())
                    .getConstructor(Class.forName("net.neoforged.fml.ModContainer"), screen);
            Object factory = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
                if (method.getName().equals("createScreen")) return ctor.newInstance(args[0], args[1]);
                if (method.getName().equals("toString")) return "YSM native config factory";
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                throw new UnsupportedOperationException(method.toString());
            });
            container.getClass().getMethod("registerExtensionPoint", Class.class, Supplier.class)
                    .invoke(container, api, (Supplier<Object>) () -> factory);
        } else {
            Class<?> api = Class.forName("net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory");
            Function<Object, Object> create = parent -> {
                try {
                    return Class.forName("dev.local.ysmenhancements.config.ForgeConfigScreen")
                            .getMethod("create", Object.class).invoke(null, parent);
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(unwrap(e)); }
            };
            Object factory = api.getConstructor(Function.class).newInstance(create);
            registration.getClass().getMethod("registerExtensionPoint", Class.class, Supplier.class)
                    .invoke(registration, api, (Supplier<Object>) () -> factory);
        }
    }

    public static boolean enabled(Feature feature) {
        if (fabric && feature == Feature.MOUSE_SHORTCUTS) return false;
        Object current = spec;
        if (current == null) return true; // Before the loader loads CLIENT configuration.
        try { return !((Boolean) loaded.invoke(current)) || (Boolean) get.invoke(VALUES.get(feature)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read " + feature.key, unwrap(e)); }
    }
    public static void set(Feature feature, boolean enabled) {
        try { set.invoke(VALUES.get(feature), enabled); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot set " + feature.key, unwrap(e)); }
    }
    public static void save() {
        try { save.invoke(spec); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot save YSM patch config", unwrap(e)); }
    }
    private static Throwable unwrap(Throwable e) {
        return e instanceof InvocationTargetException i && i.getCause() != null ? i.getCause() : e;
    }
}
