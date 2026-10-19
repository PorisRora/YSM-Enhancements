package dev.local.ysmenhancements.loader;

import dev.local.ysmenhancements.config.NativeConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("ysm_enhancements")
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(ModContainer container) { NativeConfig.register(true, container); }
}
