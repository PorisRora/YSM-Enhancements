package dev.local.ysmenhancements.fabric.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import dev.local.ysmenhancements.fabric.ShortcutGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.class_310",remap=false)
public abstract class MinecraftMixin {
    @Inject(method="method_15995(Z)V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$focus(boolean active,CallbackInfo ci) {
        InputResetHooks.onFocus(active);
        if (!active) ShortcutGuard.end();
    }
    @Inject(method="method_1507(Lnet/minecraft/class_437;)V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$screen(@Coerce Object next,CallbackInfo ci) {
        InputResetHooks.onScreen(next); ShortcutGuard.end();
    }
    @Inject(method="method_18502(Lnet/minecraft/class_4071;)V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$overlay(@Coerce Object next,CallbackInfo ci) {
        InputResetHooks.onGui(next); ShortcutGuard.end();
    }
}
