package dev.local.ysmenhancements.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.KeyboardHandler", remap = false)
public abstract class KeyboardInputMixin {
    @Inject(method = {"keyPress(JIIII)V", "m_90893_(JIIII)V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$release(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // Run before GUI handlers can consume the release, and before YSM's
        // isInGame guard can discard it. Press/repeat events remain untouched.
        InputResetHooks.onKey(key, action);
    }
}
