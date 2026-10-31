package dev.local.ysmenhancements.fabric;

import java.util.*;
import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.transform.ClientPatchPlugin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.*;

public final class FabricPatchPlugin implements IMixinConfigPlugin, Opcodes {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target,String mixin) { return true; }
    public void acceptTargets(Set<String> mine,Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void postApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
    public void preApply(String name,ClassNode node,String mixin,IMixinInfo info) {
        if (mixin.endsWith("YsmFabricMixin")) patch(node);
    }
    public static void patch(ClassNode node) {
        YsmProfile p = YsmProfile.ofName(node.name.replace('/','.'));
        if (!node.name.endsWith("/"+p.keys)) { ClientPatchPlugin.patch(node); return; }
        MethodNode callback = node.methods.stream()
            .filter(m -> m.name.equals(p.b) && m.desc.equals("(Lnet/minecraft/class_304;)V") && (m.access&ACC_STATIC)!=0)
            .findFirst().orElseThrow(() -> new IllegalStateException("Unsupported YSM lock callback"));
        long toggles = Arrays.stream(callback.instructions.toArray()).filter(i -> i instanceof MethodInsnNode c
            && c.owner.equals((YsmProfile.ROOT+p.lock).replace('.','/')) && c.name.equals(p.a) && c.desc.equals("()V")).count();
        if (toggles != 1) throw new IllegalStateException("Unexpected YSM lock toggle count: "+toggles);
        InsnList guard = new InsnList();
        LabelNode allowed = new LabelNode();
        guard.add(new VarInsnNode(ALOAD,0));
        guard.add(new MethodInsnNode(INVOKESTATIC,"dev/local/ysmenhancements/fabric/ShortcutGuard","allow","(Ljava/lang/Object;)Z",false));
        guard.add(new JumpInsnNode(IFNE,allowed)); guard.add(new InsnNode(RETURN));
        guard.add(allowed); guard.add(new FrameNode(F_SAME,0,null,0,null));
        callback.instructions.insert(guard); callback.maxStack += 2;
    }
}
