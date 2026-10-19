package dev.local.ysmenhancements.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.MouseHandler", remap = false)
public abstract class MouseInputMixin {
    @Inject(method = {"onPress(JIII)V", "m_91530_(JIII)V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$release(long window, int button, int action, int modifiers, CallbackInfo ci) {
        InputResetHooks.onMouse(button, action);
    }

    @Inject(method = {"releaseMouse()V", "m_91602_()V"}, at = @At("HEAD"), require = 1, allow = 1)
    private void ysmInputReset$ungrab(CallbackInfo ci) {
        InputResetHooks.clearAll();
    }
}
