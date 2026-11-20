package dev.local.ysmenhancements.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** The plugin adds missing 1.20.1 factories only for the legacy OpenYSM Forge build. */
@Mixin(targets = "net.minecraft.resources.ResourceLocation", remap = false)
public abstract class OpenYsmResourceLocationMixin {}
