package dev.local.ysmenhancements.config;

import dev.local.ysmenhancements.RegressionTest;
import dev.local.ysmenhancements.compat.Reflect;
import java.lang.reflect.*;
import java.nio.file.*;

/** Loads real loader specs and checks actual TOML persistence in build/, without a game. */
public final class NativeConfigTest {
    public static void main(String[] args) throws Exception {
        boolean neo=args[0].equals("neo");Object spec=NativeConfig.prepare(neo);
        Path path=Path.of("build/native-config-"+args[0]+".toml").toAbsolutePath();Files.createDirectories(path.getParent());Files.deleteIfExists(path);
        Object builder=Reflect.call(Reflect.type("com.electronwill.nightconfig.core.file.CommentedFileConfig"),"builder",path);
        Reflect.call(builder,"sync");Object file=Reflect.call(builder,"build");Reflect.call(file,"load");
        if(neo){
            // Use the real sealed LoadedConfig. Lifecycle/event dispatch requires a running loader,
            // so this isolated check corrects first and writes through NightConfig directly on Neo.
            Reflect.call(spec,"correct",file);
            Constructor<?> ctor=Reflect.type("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructors()[0];ctor.setAccessible(true);
            Reflect.call(spec,"acceptConfig",ctor.newInstance(file,null,null));
        }else Reflect.call(spec,"setConfig",file);
        Field current=NativeConfig.class.getDeclaredField("spec");current.setAccessible(true);current.set(null,spec);
        for(Feature f:Feature.values())RegressionTest.check(f.enabled(),"default enabled "+f.key);
        NativeConfig.set(Feature.BUTTON_PANEL,false);NativeConfig.set(Feature.INPUT_RESET,false);
        if(neo)Reflect.call(file,"save");else NativeConfig.save();
        String toml=Files.readString(path);
        RegressionTest.check(toml.contains("buttonPanel = false")&&toml.contains("inputReset = false"),"real loader TOML persisted disabled switches");
        RegressionTest.check(!Feature.BUTTON_PANEL.enabled()&&!Feature.INPUT_RESET.enabled(),"live values use loader config");
        RegressionTest.check(Feature.STALE_CALLBACKS.enabled()&&Feature.NUMERIC_VALUES.enabled(),"other fixes remain enabled independently");
        RegressionTest.Fixture.keys[341]=true;RegressionTest.install(new dev.local.ysmenhancements.CacheBinding(RegressionTest.Fixture.class.getField("keys"),RegressionTest.Fixture.class.getField("mouse")));
        dev.local.ysmenhancements.InputResetHooks.onFocus(false);
        RegressionTest.check(RegressionTest.Fixture.keys[341],"disabled reset does not change YSM cache");
        NativeConfig.set(Feature.INPUT_RESET,true);dev.local.ysmenhancements.InputResetHooks.onFocus(false);
        RegressionTest.check(!RegressionTest.Fixture.keys[341],"enabling reset takes effect without restart");
        Reflect.call(file,"close");System.out.println("PASS: "+args[0]+" native config defaults, persistence, independent switches and live input gate");
    }
}
