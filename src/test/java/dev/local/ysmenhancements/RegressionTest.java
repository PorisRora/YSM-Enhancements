package dev.local.ysmenhancements;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Isolated regression tests: never loads YSM's native code or a game world. */
public final class RegressionTest implements Opcodes {
    private static int assertions;

    public static final class Fixture {
        public static volatile boolean[] keys = new boolean[349];
        public static volatile boolean[] mouse = new boolean[8];
    }

    public static void main(String[] args) throws Exception {
        CacheBinding binding = new CacheBinding(Fixture.class.getField("keys"), Fixture.class.getField("mouse"));
        install(binding);
        exercise(binding);
        if (args.length > 0) exerciseOriginalJar(Path.of(args[0]));
        System.out.println("PASS: " + assertions + " input regression assertions");
    }

    public static void install(CacheBinding cache) throws Exception {
        Field field = InputResetHooks.class.getDeclaredField("binding");
        field.setAccessible(true);
        field.set(null, cache);
    }

    private static boolean[] keys(CacheBinding cache) throws Exception { return (boolean[]) cache.keys().get(null); }
    private static boolean[] mouse(CacheBinding cache) throws Exception { return (boolean[]) cache.mouse().get(null); }

    private static void exercise(CacheBinding cache) throws Exception {
        // Ctrl down -> focus loss -> release outside the game -> focus return.
        keys(cache)[341] = true;
        keys(cache)[345] = true;
        mouse(cache)[0] = true;
        InputResetHooks.onFocus(false);
        check(!keys(cache)[341] && !keys(cache)[345] && !mouse(cache)[0], "focus loss clears both devices");
        InputResetHooks.onKey(341, 0);
        InputResetHooks.onFocus(true);
        check(!keys(cache)[341], "returning to the window cannot restore stale Ctrl");

        // Inventory, chat and pause menus all enter through the same GUI hook.
        for (String gui : new String[]{"inventory", "chat", "pause", "loading overlay"}) {
            keys(cache)[341] = true;
            mouse(cache)[7] = true;
            InputResetHooks.onGui(gui);
            check(!keys(cache)[341] && !mouse(cache)[7], gui + " clears cache");
            InputResetHooks.onKey(341, 0);
            InputResetHooks.onGui(null);
            check(!keys(cache)[341], gui + " closing does not resurrect input");
        }

        // Preserve other held keys and avoid turning presses into toggles.
        keys(cache)[341] = true;
        keys(cache)[87] = true;
        InputResetHooks.onKey(341, 1);
        InputResetHooks.onKey(341, 2);
        InputResetHooks.onFocus(true);
        InputResetHooks.onGui(null);
        check(keys(cache)[341] && keys(cache)[87], "press/repeat/focus gain do not reset held keys");
        boolean[] oldSnapshot = keys(cache);
        InputResetHooks.onKey(341, 0);
        check(!keys(cache)[341] && keys(cache)[87], "release clears only the released key");
        check(keys(cache) != oldSnapshot && oldSnapshot[341], "reset publishes a fresh volatile snapshot");
        mouse(cache)[0] = true;
        mouse(cache)[1] = true;
        InputResetHooks.onMouse(0, 0);
        check(!mouse(cache)[0] && mouse(cache)[1], "release clears only the released mouse button");

        for (int invalid : new int[]{-1, Integer.MIN_VALUE, 349, Integer.MAX_VALUE}) InputResetHooks.onKey(invalid, 0);
        for (int invalid : new int[]{-1, 8, Integer.MAX_VALUE}) InputResetHooks.onMouse(invalid, 0);
        check(keys(cache)[87] && mouse(cache)[1], "unknown/out-of-range events do not corrupt state");
        InputResetHooks.clearAll();
        InputResetHooks.clearAll();
        check(!keys(cache)[87] && !mouse(cache)[1], "ungrab/reset is idempotent");
        InputResetHooks.onKey(341, 1);
        check(!keys(cache)[341], "patch never manufactures a key-down state");
    }

    private static void exerciseOriginalJar(Path jar) throws Exception {
        CacheDescriptor descriptor = CacheDescriptor.discover(jar);
        byte[] original;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            original = zip.getInputStream(zip.getEntry(descriptor.owner() + ".class")).readAllBytes();
        }
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        Map<String, byte[]> isolatedClasses = new HashMap<>();
        isolatedClasses.put(descriptor.owner().replace('/', '.'), original);
        MethodNode keyMethod = node.methods.stream().filter(m -> m.desc.contains("InputEvent$Key;")).findFirst().orElseThrow();
        String eventOwner = keyMethod.desc.substring(2, keyMethod.desc.indexOf(';'));
        isolatedClasses.put(eventOwner.replace('/', '.'), eventClass(eventOwner));
        String gateOwner = null;
        for (MethodNode method : node.methods) {
            if (method.desc.contains("InputEvent$MouseButton$Post;")) {
                String mouseOwner = method.desc.substring(2, method.desc.indexOf(';'));
                isolatedClasses.put(mouseOwner.replace('/', '.'), eventClass(mouseOwner));
            }
            if (method == keyMethod) {
                for (var insn : method.instructions) {
                    if (insn instanceof MethodInsnNode call && call.getOpcode() == INVOKESTATIC && call.desc.equals("()Z")) {
                        isolatedClasses.put(call.owner.replace('/', '.'), gateClass(call.owner, call.name));
                        if (!call.name.equals("isAvailable")) gateOwner = call.owner;
                    }
                }
            }
        }
        if (gateOwner == null) throw new AssertionError("Missing isInGame gate in original YSM input handler");
        ClassLoader loader = new ByteLoader(isolatedClasses);
        CacheBinding binding = descriptor.bind(loader);
        Class<?> eventType = loader.loadClass(eventOwner.replace('/', '.'));
        Method handler = loader.loadClass(descriptor.owner().replace('/', '.')).getMethod(keyMethod.name, eventType);
        var eventConstructor = eventType.getConstructor(int.class, int.class);
        Field gate = loader.loadClass(gateOwner.replace('/', '.')).getField("active");
        handler.invoke(null, eventConstructor.newInstance(341, 1));
        check(keys(binding)[341], "original jar records Ctrl down");
        gate.setBoolean(null, false);
        handler.invoke(null, eventConstructor.newInstance(341, 0));
        check(keys(binding)[341], "original jar reproduces discarded release while out of game");
        install(binding);
        InputResetHooks.onKey(341, 0);
        check(!keys(binding)[341], "patch clears actual YSM cache despite the closed gate");
        gate.setBoolean(null, true);
        handler.invoke(null, eventConstructor.newInstance(341, 1));
        check(keys(binding)[341], "normal YSM key press continues after repair");
        exercise(binding);
        System.out.println("Verified original class: " + descriptor.owner() + " from " + jar.getFileName());
    }

    private static byte[] eventClass(String name) {
        ClassWriter writer = base(name);
        writer.visitField(ACC_PUBLIC, "index", "I", null, null).visitEnd();
        writer.visitField(ACC_PUBLIC, "action", "I", null, null).visitEnd();
        var ctor = writer.visitMethod(ACC_PUBLIC, "<init>", "(II)V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(ALOAD, 0);
        ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        for (int i = 1; i <= 2; i++) {
            ctor.visitVarInsn(ALOAD, 0); ctor.visitVarInsn(ILOAD, i);
            ctor.visitFieldInsn(PUTFIELD, name, i == 1 ? "index" : "action", "I");
        }
        ctor.visitInsn(RETURN); ctor.visitMaxs(0, 0); ctor.visitEnd();
        for (String method : new String[]{"getKey", "getButton", "getAction"}) {
            var getter = writer.visitMethod(ACC_PUBLIC, method, "()I", null, null);
            getter.visitCode(); getter.visitVarInsn(ALOAD, 0);
            getter.visitFieldInsn(GETFIELD, name, method.equals("getAction") ? "action" : "index", "I");
            getter.visitInsn(IRETURN); getter.visitMaxs(0, 0); getter.visitEnd();
        }
        writer.visitEnd(); return writer.toByteArray();
    }

    private static byte[] gateClass(String name, String methodName) {
        ClassWriter writer = base(name);
        writer.visitField(ACC_PUBLIC | ACC_STATIC, "active", "Z", null, null).visitEnd();
        var init = writer.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        init.visitCode(); init.visitInsn(ICONST_1); init.visitFieldInsn(PUTSTATIC, name, "active", "Z");
        init.visitInsn(RETURN); init.visitMaxs(0, 0); init.visitEnd();
        var method = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, methodName, "()Z", null, null);
        method.visitCode(); method.visitFieldInsn(GETSTATIC, name, "active", "Z");
        method.visitInsn(IRETURN); method.visitMaxs(0, 0); method.visitEnd();
        writer.visitEnd(); return writer.toByteArray();
    }

    private static ClassWriter base(String name) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC, name, null, "java/lang/Object", null);
        return writer;
    }

    public static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    public static final class ByteLoader extends ClassLoader {
        private final Map<String, byte[]> classes;
        public ByteLoader(Map<String, byte[]> classes) { super(RegressionTest.class.getClassLoader()); this.classes = classes; }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!classes.containsKey(name)) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) { byte[] bytes = classes.get(name); type = defineClass(name, bytes, 0, bytes.length); }
                if (resolve) resolveClass(type);
                return type;
            }
        }
    }
}
