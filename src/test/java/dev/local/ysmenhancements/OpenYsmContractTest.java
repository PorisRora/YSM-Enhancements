package dev.local.ysmenhancements;

import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.transform.ClientPatchPlugin;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

/** Checks the UI and input hooks against a real OpenYSM jar supplied by the caller. */
public final class OpenYsmContractTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply the OpenYSM release jar with -PopenYsmJar=<path>");
        Path path = Path.of(args[0]);
        RegressionTest.check(CacheDescriptor.discover(path).equals(CacheDescriptor.openYsm()),"named input cache");
        YsmProfile p = YsmProfile.OPEN;
        try (ZipFile jar = new ZipFile(path.toFile())) {
            boolean shared = jar.getEntry("rip/ysm/gui/ModernAnimationRouletteScreen.class") != null;
            if (!shared) checkLegacyForgeFactories();
            Set<String> targets = new LinkedHashSet<>(List.of(p.wheel,p.models,p.keys,p.lock,p.input));
            targets.addAll(List.of(p.keyHandlers));
            String riding = "client.animation.predicate.LivingMovementAnimationPredicate";
            if (jar.getEntry("fabric.mod.json") == null) targets.add(riding);
            for (String name : targets) {
                ClassNode node = new ClassNode();
                try (var in = jar.getInputStream(jar.getEntry(YsmProfile.internal(name)+".class"))) {
                    new ClassReader(in).accept(node,0);
                }
                ClientPatchPlugin.patch(node);
                if (name.equals(riding)) checkMaidRiding(node);
                Set<String> calls=new LinkedHashSet<>();
                for (var method : node.methods) {
                    if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE))==0)
                        new Analyzer<>(new BasicVerifier()).analyze(node.name,method);
                    for (var insn : method.instructions) if (insn instanceof MethodInsnNode c
                            && c.owner.startsWith("dev/local/ysmenhancements/")) calls.add(c.name);
                }
                if (name.equals(p.wheel)) require(calls,"render","created","current","truth","arrows","clippedClick");
                if (name.equals(p.models)) require(calls,"captureCaret","restoreCaret");
                if (name.equals(p.keys)) require(calls,shared?"rawMatches":"deviceMatches");
                if (name.equals(p.lock)) require(calls,shared?"lockMatches":"openLockMatches");
                if (name.equals(p.input)) require(calls,shared?"onMouse":"openMouseEvent");
                if (shared && List.of(p.keyHandlers).contains(name)) require(calls,"rememberKeyAction");
                if (shared && name.equals("client.input.AnimationRouletteKey")) require(calls,"useModernWheel");
            }
        }
        System.out.println("PASS: OpenYSM client UI and input contracts for "+path.getFileName());
    }
    private static void checkMaidRiding(ClassNode node) {
        String player = "net/minecraft/world/entity/player/Player";
        for (var method : node.methods) if (method.name.equals("renderRidingAnimation")) {
            boolean living = false, playerGuard = false, checkedPlayer = false;
            for (var insn : method.instructions) {
                if (insn instanceof MethodInsnNode c && c.name.equals("getEntity") && c.getNext() instanceof TypeInsnNode cast)
                    living = cast.getOpcode() == Opcodes.CHECKCAST && cast.desc.equals("net/minecraft/world/entity/LivingEntity");
                if (insn instanceof TypeInsnNode type && type.desc.equals(player)) {
                    if (type.getOpcode() == Opcodes.INSTANCEOF) playerGuard = true;
                    if (playerGuard && type.getOpcode() == Opcodes.CHECKCAST) checkedPlayer = true;
                }
                if (insn instanceof MethodInsnNode c && c.name.equals("isPlayerCarrying")) {
                    RegressionTest.check(living && checkedPlayer,"riding must accept a maid and narrow only the player-only action");
                    return;
                }
            }
        }
        throw new AssertionError("missing OpenYSM riding predicate");
    }
    private static void checkLegacyForgeFactories() throws Exception {
        ClassNode resource=new ClassNode();
        resource.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"net/minecraft/resources/ResourceLocation",null,"java/lang/Object",null);
        ClientPatchPlugin plugin=new ClientPatchPlugin();
        String mixin="dev.local.ysmenhancements.mixin.OpenYsmResourceLocationMixin";
        plugin.preApply(resource.name,resource,mixin,null);
        List<MethodNode> added=List.copyOf(resource.methods);
        RegressionTest.check(added.size()==2,"install the two missing resource factories on Forge 47.2");
        for (MethodNode factory : added) {
            String params=factory.name.equals("parse")?"(Ljava/lang/String;)":"(Ljava/lang/String;Ljava/lang/String;)";
            RegressionTest.check(factory.desc.equals(params+"L"+resource.name+";"),"resource factory signature");
            new Analyzer<>(new BasicVerifier()).analyze(resource.name,factory);
            boolean delegates=false;
            for (var instruction : factory.instructions) if (instruction instanceof MethodInsnNode call)
                delegates=call.getOpcode()==Opcodes.INVOKESPECIAL && call.owner.equals(resource.name) && call.name.equals("<init>") && call.desc.equals(params+"V");
            RegressionTest.check(delegates,"delegate resource validation to the original constructor");
        }
        plugin.preApply(resource.name,resource,mixin,null);
        RegressionTest.check(resource.methods.equals(added),"preserve existing Forge resource factories");
    }
    private static void require(Set<String> calls,String... names) {
        for (String name : names) RegressionTest.check(calls.contains(name),"missing OpenYSM hook "+name);
    }
}
