package dev.local.ysmenhancements.fabric;

import dev.local.ysmenhancements.*;
import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.config.NativeConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntrypoint implements ClientModInitializer {
    @Override public void onInitializeClient() {
        String mc = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
        YsmProfile profile = switch(mc) {
            case "1.20.1" -> YsmProfile.FABRIC_1201;
            case "1.21.1" -> YsmProfile.FABRIC_1211;
            default -> throw new IllegalStateException("Unsupported Minecraft version: "+mc);
        };
        InputResetHooks.useFabricCache(new CacheDescriptor(YsmProfile.ROOT+profile.input,profile.a,profile.b));
        NativeConfig.registerFabric(profile.modern());
    }
}
