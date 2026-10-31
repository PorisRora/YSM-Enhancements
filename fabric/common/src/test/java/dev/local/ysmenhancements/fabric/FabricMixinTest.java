package dev.local.ysmenhancements.fabric;

import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.testservice.TestMixinService;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.service.MixinService;
import static dev.local.ysmenhancements.RegressionTest.check;

public final class FabricMixinTest implements Opcodes {
    public static void main(String[] args)throws Exception {
        YsmProfile p=YsmProfile.valueOf(args[0]);
        try(var mc=new ZipFile(args[1]);var ysm=new ZipFile(args[2])) {
            TestMixinService.minecraft=mc;TestMixinService.ysm=ysm;
            MixinBootstrap.init();
            var environment=MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
            Mixins.addConfiguration("ysm_enhancements.fabric.mixins.json");
            var transformer=((TestMixinService)MixinService.getService()).transformer();
            for(String name:List.of("net.minecraft.class_310","net.minecraft.class_309","net.minecraft.class_312",YsmProfile.ROOT+p.wheel,YsmProfile.ROOT+p.models,YsmProfile.ROOT+p.keys)) {
                ZipFile jar=name.startsWith("net.")?mc:ysm;
                byte[] source=jar.getInputStream(jar.getEntry(name.replace('.','/')+".class")).readAllBytes();
                byte[] transformed=transformer.transformClass(environment,name,source);
                ClassNode node=new ClassNode();new ClassReader(transformed).accept(node,0);
                long hooks=node.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).filter(i->i instanceof MethodInsnNode c&&c.owner.startsWith("dev/local/ysmenhancements/")).count();
                check(hooks>0,"actual Mixin configuration applied to "+name);
                for(var m:node.methods)if((m.access&(ACC_NATIVE|ACC_ABSTRACT))==0)new Analyzer<>(new BasicVerifier()).analyze(node.name,m);
                System.out.println("PASS: real Sponge Mixin on actual "+p+" class "+name+" ("+hooks+" hooks)");
            }
        }
    }
}
