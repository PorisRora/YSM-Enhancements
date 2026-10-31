package dev.local.ysmenhancements.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets={
    "com.elfmcys.yesstevemodel.oOOooOOooooOo0oo0O0oOoOo",
    "com.elfmcys.yesstevemodel.o0Ooo0OOo00oo0OoooOOO0Oo",
    "com.elfmcys.yesstevemodel.O0o0o0OoO0oOoo0oO0o0OoOo",
    "com.elfmcys.yesstevemodel.OOooO0Oo00O0OOoo0o0oO00o",
    "com.elfmcys.yesstevemodel.O0OOOOOoo00O0Ooo0oO0Oo00",
    "com.elfmcys.yesstevemodel.o00oooo0Oo0oO00OO000000O"
},remap=false)
public abstract class YsmFabricMixin {}
