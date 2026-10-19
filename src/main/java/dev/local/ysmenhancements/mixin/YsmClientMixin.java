package dev.local.ysmenhancements.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets={
    "com.elfmcys.yesstevemodel.Oo0Oo0Oooo00OoOOOO0O00o0", "com.elfmcys.yesstevemodel.oooOO0o0ooo0O000Oo0ooOoo",
    "com.elfmcys.yesstevemodel.O0o0Oo0Oo0Ooo0oO000o0OOO", "com.elfmcys.yesstevemodel.O0o0oOO0OOO0O0OooOooOo0o",
    "com.elfmcys.yesstevemodel.OOoOOoo00oOo0OOOoOooooO0", "com.elfmcys.yesstevemodel.o00oO0Ooo00oo0ooO0oOOOoO",
    "com.elfmcys.yesstevemodel.Ooo0o0Ooo0OO0oo0OOOoOooO", "com.elfmcys.yesstevemodel.oOoo0O0OO0OOOOo0O00OOooO",
    "com.elfmcys.yesstevemodel.O0ooooOoOO0ooO0000oo0ooo", "com.elfmcys.yesstevemodel.O000000ooOo0OoO00ooo0o0o"
},remap=false)
public abstract class YsmClientMixin {}
