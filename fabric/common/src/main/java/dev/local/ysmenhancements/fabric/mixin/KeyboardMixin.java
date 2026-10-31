package dev.local.ysmenhancements.fabric.mixin;

import dev.local.ysmenhancements.InputResetHooks;
import dev.local.ysmenhancements.fabric.ShortcutGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.class_309",remap=false)
public abstract class KeyboardMixin {
    @Inject(method="method_1466(JIIII)V",at=@At("HEAD"),require=1,allow=1)
    private void ysm$begin(long window,int key,int scan,int action,int modifiers,CallbackInfo ci) {
        InputResetHooks.onKey(key,action); ShortcutGuard.begin(false,key,scan,action);
    }
    @Inject(method="method_1466(JIIII)V",at=@At("RETURN"),require=1)
    private void ysm$end(long window,int key,int scan,int action,int modifiers,CallbackInfo ci) { ShortcutGuard.end(); }
}
