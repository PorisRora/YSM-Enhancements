package dev.local.ysmenhancements.testservice;

import dev.local.ysmenhancements.CacheBinding;
import dev.local.ysmenhancements.RegressionTest;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

public final class MixinSmokeTest implements Opcodes {
    private static final String MC = "net.minecraft.client.Minecraft";
    private static final String KEY = "net.minecraft.client.KeyboardHandler";
    private static final String MOUSE = "net.minecraft.client.MouseHandler";
    private static final String SCREEN = "net.minecraft.client.gui.screens.Screen";
    private static final String OVERLAY = "net.minecraft.client.gui.screens.Overlay";

    public static void main(String[] args) throws Exception {
        boolean actual = args.length > 0;
        if (actual) TestMixinService.minecraft = new ZipFile(args[0]);
        else fixtures();
        if (args.length > 1) TestMixinService.baseMinecraft = new ZipFile(args[1]);
        if (args.length > 2) TestMixinService.ysm = new ZipFile(args[2]);
        MixinBootstrap.init();
        var environment = MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("ysm_enhancements.mixins.json");
        var service = (TestMixinService) MixinService.getService();
        var transformer = service.transformer();
        Map<String, byte[]> transformed = new HashMap<>();
        for (String name : new String[]{MC, KEY, MOUSE}) {
            byte[] original;
            if (actual) {
                try (var stream = TestMixinService.minecraft.getInputStream(TestMixinService.minecraft.getEntry(name.replace('.', '/') + ".class"))) {
                    original = stream.readAllBytes();
                }
            } else original = TestMixinService.BYTECODE.get(name);
            byte[] output = transformer.transformClass(environment, name, original);
            ClassNode node = new ClassNode();
            new ClassReader(output).accept(node, 0);
            int hookCalls = 0;
            for (var method : node.methods) for (var insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals("dev/local/ysmenhancements/InputResetHooks")) hookCalls++;
            }
            int expected = name.equals(MC) ? 3 : name.equals(KEY) ? 1 : 2;
            RegressionTest.check(hookCalls == expected, name + " expected " + expected + " hooks, found " + hookCalls);
            transformed.put(name, output);
            System.out.println("PASS: real Mixin transformer inserted " + hookCalls + " hooks in " + name);
        }
        if (!actual) executeFixtures(transformed);
        else TestMixinService.minecraft.close();
        if (TestMixinService.baseMinecraft != null) TestMixinService.baseMinecraft.close();
        if (TestMixinService.ysm != null) {
            var p = TestMixinService.ysm.getEntry((dev.local.ysmenhancements.compat.YsmProfile.ROOT + dev.local.ysmenhancements.compat.YsmProfile.NEO.wheel).replace('.', '/') + ".class") != null
                    ? dev.local.ysmenhancements.compat.YsmProfile.NEO : dev.local.ysmenhancements.compat.YsmProfile.FORGE;
            // Keep both Minecraft archives open while resolving the YSM superclass hierarchy.
            TestMixinService.minecraft = new ZipFile(args[0]);
            TestMixinService.baseMinecraft = new ZipFile(args[1]);
            for (String simple : new String[]{p.wheel,p.models,p.keys,p.lock,p.input}) {
                String name = dev.local.ysmenhancements.compat.YsmProfile.ROOT + simple;
                byte[] original;
                try (var stream=TestMixinService.ysm.getInputStream(TestMixinService.ysm.getEntry(name.replace('.','/')+".class"))) {original=stream.readAllBytes();}
                byte[] output=transformer.transformClass(environment,name,original);
                ClassNode node=new ClassNode();new ClassReader(output).accept(node,0);
                long hooks=node.methods.stream().flatMap(m->java.util.Arrays.stream(m.instructions.toArray()))
                    .filter(i->i instanceof MethodInsnNode c&&c.owner.startsWith("dev/local/ysmenhancements/")).count();
                RegressionTest.check(hooks>0,"YSM plugin applied to "+name);
                for(var m:node.methods) if((m.access&(ACC_ABSTRACT|ACC_NATIVE))==0)
                    new org.objectweb.asm.tree.analysis.Analyzer<>(new org.objectweb.asm.tree.analysis.BasicVerifier()).analyze(node.name,m);
                System.out.println("PASS: real Mixin + client plugin inserted "+hooks+" hooks in "+simple);
            }
            TestMixinService.ysm.close();TestMixinService.minecraft.close();TestMixinService.baseMinecraft.close();
        }
        System.out.println("PASS: " + (actual ? "actual Minecraft jar " + args[0] : "transformed hook execution"));
    }

    private static void executeFixtures(Map<String, byte[]> classes) throws Exception {
        classes.put(SCREEN, TestMixinService.BYTECODE.get(SCREEN));
        classes.put(OVERLAY, TestMixinService.BYTECODE.get(OVERLAY));
        var loader = new RegressionTest.ByteLoader(classes);
        var cache = new CacheBinding(RegressionTest.Fixture.class.getField("keys"), RegressionTest.Fixture.class.getField("mouse"));
        RegressionTest.install(cache);
        Object minecraft = loader.loadClass(MC).getConstructor().newInstance();
        Object keyboard = loader.loadClass(KEY).getConstructor().newInstance();
        Object mouse = loader.loadClass(MOUSE).getConstructor().newInstance();
        RegressionTest.Fixture.keys[341] = true;
        minecraft.getClass().getMethod("setWindowActive", boolean.class).invoke(minecraft, false);
        RegressionTest.check(!RegressionTest.Fixture.keys[341], "injected focus handler clears Ctrl");
        for (String gui : new String[]{SCREEN, OVERLAY}) {
            RegressionTest.Fixture.keys[341] = true;
            Class<?> type = loader.loadClass(gui);
            minecraft.getClass().getMethod(gui.equals(SCREEN) ? "setScreen" : "setOverlay", type).invoke(minecraft, type.getConstructor().newInstance());
            RegressionTest.check(!RegressionTest.Fixture.keys[341], "injected GUI handler clears Ctrl");
        }
        RegressionTest.Fixture.keys[341] = true;
        keyboard.getClass().getMethod("keyPress", long.class, int.class, int.class, int.class, int.class).invoke(keyboard, 1L, 341, 0, 0, 0);
        RegressionTest.check(!RegressionTest.Fixture.keys[341], "injected key-release handler clears Ctrl");
        RegressionTest.Fixture.mouse[0] = true;
        mouse.getClass().getMethod("onPress", long.class, int.class, int.class, int.class).invoke(mouse, 1L, 0, 0, 0);
        RegressionTest.check(!RegressionTest.Fixture.mouse[0], "injected mouse-release handler clears button");
        RegressionTest.Fixture.keys[345] = true;
        mouse.getClass().getMethod("releaseMouse").invoke(mouse);
        RegressionTest.check(!RegressionTest.Fixture.keys[345], "injected cursor-ungrab handler clears right Ctrl");
    }

    private static void fixtures() {
        addFixture(SCREEN, new String[0][0]);
        addFixture(OVERLAY, new String[0][0]);
        addFixture(MC, new String[][]{{"setWindowActive", "(Z)V"}, {"setScreen", "(Lnet/minecraft/client/gui/screens/Screen;)V"}, {"setOverlay", "(Lnet/minecraft/client/gui/screens/Overlay;)V"}});
        addFixture(KEY, new String[][]{{"keyPress", "(JIIII)V"}});
        addFixture(MOUSE, new String[][]{{"onPress", "(JIII)V"}, {"releaseMouse", "()V"}});
    }

    private static void addFixture(String name, String[][] methods) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
        var ctor = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode(); ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(RETURN); ctor.visitMaxs(0, 0); ctor.visitEnd();
        for (String[] definition : methods) {
            var method = writer.visitMethod(ACC_PUBLIC, definition[0], definition[1], null, null);
            method.visitCode(); method.visitInsn(RETURN); method.visitMaxs(0, 0); method.visitEnd();
        }
        writer.visitEnd(); TestMixinService.BYTECODE.put(name, writer.toByteArray());
    }
}
