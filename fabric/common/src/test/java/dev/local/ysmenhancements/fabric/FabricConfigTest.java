package dev.local.ysmenhancements.fabric;

import java.lang.reflect.Proxy;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

/** Supplies only the launcher environment flag required by ConfigValue.get(). */
public final class FabricConfigTest {
    public static void main(String[] args) throws Exception {
        dev.local.ysmenhancements.compat.Reflect.set(FabricLauncherBase.class,"launcher",(FabricLauncher)Proxy.newProxyInstance(FabricConfigTest.class.getClassLoader(),new Class<?>[]{FabricLauncher.class},(proxy,method,values)->{
            if(method.getName().equals("isDevelopment"))return false;
            throw new UnsupportedOperationException("Unexpected launcher dependency: "+method.getName());
        }));
        boolean modern=args[0].endsWith("1211");
        String root=modern?"net.neoforged":"net.minecraftforge";
        String registry=modern?"fuzs/forgeconfigapiport/fabric/api/neoforge/v4/NeoForgeConfigRegistry":"fuzs/forgeconfigapiport/api/config/v2/ForgeConfigRegistry";
        var node=new org.objectweb.asm.tree.ClassNode();
        try(var in=FabricConfigTest.class.getClassLoader().getResourceAsStream(registry+".class")) {
            new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        String prefix="(Ljava/lang/String;L"+root.replace('.','/')+"/fml/config/ModConfig$Type;L"+root.replace('.','/')+"/fml/config/IConfigSpec;Ljava/lang/String;)";
        dev.local.ysmenhancements.RegressionTest.check(node.methods.stream().anyMatch(m->m.name.equals("register")&&m.desc.startsWith(prefix)),"actual Fabric registry signature");
        dev.local.ysmenhancements.config.NativeConfigTest.main(args);
    }
}
