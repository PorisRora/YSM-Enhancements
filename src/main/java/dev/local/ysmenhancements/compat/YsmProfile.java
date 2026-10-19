package dev.local.ysmenhancements.compat;

public enum YsmProfile {
    NEO("Oo0Oo0Oooo00OoOOOO0O00o0", "O0o0Oo0Oo0Ooo0oO000o0OOO", "OOoOOoo00oOo0OOOoOooooO0", "Ooo0o0Ooo0OO0oo0OOOoOooO", "O0ooooOoOO0ooO0000oo0ooo",
        "oOo0OO0O0o000OO0O000oo0o", "oOoo00O0o0oO0o0oO00OO0O0",
        "ooOooOO0oO00o00o0o0oOOoO", "OOo0O00Ooo00O0Ooo0OoOo0o", "oo0o0Oooo0OOO00OOo0OO000", "O0OO0O0o00o0o00oOoO0o0oO", "oo000ooO0O00oo0OO0ooO000", "O0O00oOooOo000oo00OoO0Oo",
        "OoOO0o0O00o00OoOO0OO0OOo", "O000OOo000oOOo0OO0oo0o0O", "OOO0oooOOo00OOooo0OooOOo", "O0oo0O0O0O0oO0oooOOo00o0", "OO0Oo0O00OOOo0oOo0oooooO",
        "OO000o0ooOooooOOOOO0Ooo0", "OOo0o0000Ooo0o00OO0oOOoO", "Oo0O0OoOo0O0oOoo0000O0oO", "O0ooOooo0ooOOo0OoOOOOooO", "O0O0o0Oo0Oo00O0OooO00oOo", "ooo00OoO00OOOO0oOooOo0Oo",
        "oo0o0Oooo0OOO00OOo0OO000",
        new String[]{"OoOOO0OOoO0OOOOOOOOooOo0", "OOooooo0o000OOoOO000O0Oo", "ooooOO000O0o0O000OOoo0oO", "OOo0o0oO0oooO0000ooOOO0o", "oo00o0oOOOooOOOo0ooOO0oo", "Ooo0o0Ooo0OO0oo0OOOoOooO"}),
    FORGE("oooOO0o0ooo0O000Oo0ooOoo", "O0o0oOO0OOO0O0OooOooOo0o", "o00oO0Ooo00oo0ooO0oOOOoO", "oOoo0O0OO0OOOOo0O00OOooO", "O000000ooOo0OoO00ooo0o0o",
        "Oo0Oo0o00O00Oo0OOoOOoooo", "o0OOooo0o0OO00OoOOOo0o0O",
        "Oo00o0OooOOo0ooOoo0oO0o0", "o0OOO0o0o0OOo000oO00o00O", "O0ooooOO0oOo000O0Oo00OOO", "OO0ooO00OoO00o0OO0OOooO0", "Ooo0O0000OO0OOO0o0Oo0oOO", "o0o0Oooo00OoOoOOOooo0000",
        "OOOo0OOOoOO0O00Oo00ooOOO", "ooooO0o00oO0Oo0OOo0O0O0o", "oOOo0Ooo0oOoo0O0OOOOo0oo", "oOo0o0000OOOO0OooooO00oo", "O0OooOo0oOOoOoOoOooO000o",
        "O00OOOooOoooOoo0o0o0oO0O", "oOOOo0OOO0ooooo0O00OO0o0", "OOOOo0O0oO0OOo0O0O0Oo0O0", "Ooo00oO0o0OoOO0o0ooOOOo0", "OO0O00oO0OOO00oOO0OoO0OO", "OO00OOOOo0Ooo0oo0o0Oo0OO",
        "O0ooooOO0oOo000O0Oo00OOO",
        new String[]{"o0OOOOOO0O0oO0o0Oo00Oooo", "O0O0oo0OOOOoo00o0oOoO00o", "Oo0o00ooO0O000OO0o00000o", "o0000oo0oOoOo000oOooO0Oo", "o0o000O000oOOO0O0o0OOo0o", "oOoo0O0OO0OOOOo0O00OOooO"});
    public static final String ROOT = "com.elfmcys.yesstevemodel.";
    public final String wheel, models, keys, lock, input, a, b, x, y, scroll, max, up, down, selected, menu, configs, path, selectedIndex, configMethod, playMethod, folderMethod, marker, animatable, entityMethod, search;
    public final String[] keyHandlers;
    YsmProfile(String wheel, String models, String keys, String lock, String input, String a, String b, String x, String y, String scroll, String max, String up, String down, String selected, String menu, String configs, String path, String selectedIndex, String configMethod, String playMethod, String folderMethod, String marker, String animatable, String entityMethod, String search, String[] keyHandlers) {
        this.wheel=wheel; this.models=models; this.keys=keys; this.lock=lock; this.input=input; this.a=a; this.b=b; this.x=x; this.y=y; this.scroll=scroll; this.max=max; this.up=up; this.down=down; this.selected=selected; this.menu=menu; this.configs=configs; this.path=path; this.selectedIndex=selectedIndex; this.configMethod=configMethod; this.playMethod=playMethod; this.folderMethod=folderMethod; this.marker=marker; this.animatable=animatable; this.entityMethod=entityMethod; this.search=search; this.keyHandlers=keyHandlers;
    }
    public String loaderRoot() { return this == NEO ? "net.neoforged.neoforge" : "net.minecraftforge"; }
    public static YsmProfile of(Object instance) { return ofName(instance.getClass().getName()); }
    public static YsmProfile ofName(String name) {
        for (YsmProfile p: values()) if (name.equals(ROOT+p.wheel)||name.equals(ROOT+p.models)||name.equals(ROOT+p.keys)||name.equals(ROOT+p.lock)||name.equals(ROOT+p.input)) return p;
        throw new IllegalArgumentException("Unsupported YSM class: " + name);
    }
}
