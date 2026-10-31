package dev.local.ysmenhancements.fabric.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import dev.local.ysmenhancements.fabric.ShortcutGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.class_312",remap=false)
public abstract class MouseMixin {
    @Inject(method="method_1601(JIII)V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$begin(long window,int button,int action,int modifiers,CallbackInfo ci) {
        InputResetHooks.onMouse(button,action); ShortcutGuard.begin(true,button,0,action);
    }
    @Inject(method="method_1601(JIII)V",at=@At("RETURN"),require=1)
    private void ysm$end(long window,int button,int action,int modifiers,CallbackInfo ci) { ShortcutGuard.end(); }
    @Inject(method="method_1610()V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$ungrab(CallbackInfo ci) { InputResetHooks.clearAll(); ShortcutGuard.end(); }
}
