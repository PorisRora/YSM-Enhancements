package dev.local.ysmenhancements.transform;

import dev.local.ysmenhancements.compat.RuntimeNames;
import dev.local.ysmenhancements.compat.YsmProfile;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** OpenYSM's GitHub release uses Forge key events. Its later builds register Architectury callbacks. */
final class OpenYsmInputPatch implements Opcodes {
    private static final String RAW = "dev/local/ysmenhancements/fixes/OpenYsmInput";
    private static final String FORGE = "dev/local/ysmenhancements/fixes/ShortcutFixes";
    private static final String KEY_CALLBACK = "()Ldev/architectury/event/events/client/ClientRawInputEvent$KeyPressed;";
    private OpenYsmInputPatch() {}

    static void patch(ClassNode node) {
        YsmProfile p = YsmProfile.OPEN;
        boolean architectury = node.methods.stream().anyMatch(m -> m.name.equals("register") && m.desc.equals("()V"));
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof InvokeDynamicInsnNode lambda && method.name.equals("register")
                        && lambda.name.equals("keyPressed") && lambda.desc.equals(KEY_CALLBACK)
                        && !node.name.equals(YsmProfile.internal(p.input))) {
                    // Keep only OpenYSM's own key actions, in their original registration order.
                    InsnList remember = new InsnList();
                    remember.add(new InsnNode(DUP));
                    remember.add(call(RAW,"rememberKeyAction","(Ljava/lang/Object;)V"));
                    method.instructions.insert(lambda, remember);
                }
                if (!(instruction instanceof MethodInsnNode c)) continue;
                if (node.name.equals(YsmProfile.internal(p.keys)) && method.name.equals("isKeyPressed")) {
                    if (c.owner.equals("rip/ysm/api/client/KeyMappingFactory") && c.name.equals("isActiveAndMatches"))
                        method.instructions.set(c,call(RAW,"rawMatches","(Ljava/lang/Object;II)Z"));
                    else if (isKeyMatch(c))
                        method.instructions.set(c,call(FORGE,"deviceMatches","(Ljava/lang/Object;II)Z"));
                } else if (node.name.equals(YsmProfile.internal(p.lock)) && isKeyMatch(c)) {
                    method.instructions.set(c,call(architectury ? RAW : FORGE,
                            architectury ? "lockMatches" : "openLockMatches","(Ljava/lang/Object;II)Z"));
                } else if (node.name.equals(YsmProfile.internal(p.input)) && c.owner.equals(node.name)
                        && c.name.equals("onMouseInput") && c.desc.equals("(II)V")) {
                    InsnList after = new InsnList();
                    after.add(new VarInsnNode(ALOAD,0));
                    for (int i=1;i<=3;i++) after.add(new VarInsnNode(ILOAD,i));
                    after.add(call(RAW,"onMouse","(Ljava/lang/Object;III)V"));
                    method.instructions.insert(c,after);
                } else if (c.owner.equals(YsmProfile.internal("config.GeneralConfig"))
                        && c.name.equals("effectiveModernRoulette") && c.desc.equals("()Z")) {
                    // Choose the classic screen with our panel only at the native Z selection point.
                    method.instructions.insert(c,call(RAW,"useModernWheel","(Z)Z"));
                }
            }
            if (node.name.equals(YsmProfile.internal(p.input)) && method.name.equals("onMouseInput")
                    && method.desc.equals("(Lnet/minecraftforge/client/event/InputEvent$MouseButton$Post;)V")) {
                for (AbstractInsnNode insn : method.instructions.toArray()) if (insn.getOpcode()==RETURN) {
                    InsnList after=new InsnList();
                    after.add(new VarInsnNode(ALOAD,0));
                    after.add(call(FORGE,"openMouseEvent","(Ljava/lang/Object;)V"));
                    method.instructions.insertBefore(insn,after);
                }
            }
        }
    }
    private static boolean isKeyMatch(MethodInsnNode call) {
        return call.owner.equals(RuntimeNames.internal("net/minecraft/client/KeyMapping"))
                && call.desc.equals("(II)Z") && (call.name.equals("matches") || call.name.equals("m_90832_") || call.name.equals("method_1417"));
    }
    private static MethodInsnNode call(String owner,String name,String descriptor) {
        return new MethodInsnNode(INVOKESTATIC,owner,name,descriptor,false);
    }
}
