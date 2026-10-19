package dev.local.ysmenhancements.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.Minecraft", remap = false)
public abstract class MinecraftInputMixin {
    @Inject(method = {"setWindowActive(Z)V", "m_7440_(Z)V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$focus(boolean focused, CallbackInfo ci) {
        InputResetHooks.onFocus(focused);
    }

    @Inject(method = {"setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            "m_91152_(Lnet/minecraft/client/gui/screens/Screen;)V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$screen(@Coerce Object screen, CallbackInfo ci) {
        InputResetHooks.onScreen(screen);
    }

    @Inject(method = {"setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V",
            "m_91150_(Lnet/minecraft/client/gui/screens/Overlay;)V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$overlay(@Coerce Object overlay, CallbackInfo ci) {
        InputResetHooks.onGui(overlay);
    }
}
