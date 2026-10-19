package dev.local.ysmenhancements.loader;

import dev.local.ysmenhancements.config.NativeConfig;
import net.minecraftforge.fml.common.Mod;

@Mod("ysm_enhancements")
public final class ForgeEntrypoint {
    public ForgeEntrypoint() { NativeConfig.register(false, null); }
}
