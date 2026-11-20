package dev.local.ysmenhancements;

import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.transform.ClientPatchPlugin;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public final class MaidSkinScreenTest implements Opcodes {
    // The screen constructed by YSM 2.6.5 NeoForge for OpenYsmMaidScreenEvent.
    private static final String MAID_SCREEN = YsmProfile.ROOT + "oOOOO00ooOoo0OOoooOOo0oO";

    public static final class SearchBox {
        public int cursorPos, highlightPos, displayPos;
        public String getValue() { return "maid skin"; }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        checkPicker(YsmProfile.NEO, "init", MAID_SCREEN);
        checkPicker(YsmProfile.OPEN, "m_7856_", YsmProfile.ROOT + "client.gui.TouhouMaidModelScreen",
                YsmProfile.ROOT + "forge.client.gui.TouhouMaidModelScreen");
        System.out.println("PASS: native maid and player model screens open and preserve search state on rebuild");
    }

    private static void checkPicker(YsmProfile profile, String initName, String... maidNames) throws ReflectiveOperationException {
        String modelName = YsmProfile.ROOT + profile.models;
        ClassNode models = screenClass(modelName, Object.class.getName());
        String boxType = Type.getInternalName(SearchBox.class);
        String boxDescriptor = Type.getDescriptor(SearchBox.class);
        models.visitField(ACC_PRIVATE, profile.search, boxDescriptor, null, null).visitEnd();

        // The native model list creates a fresh search box on each init.
        var init = models.visitMethod(ACC_PUBLIC, initName, "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitTypeInsn(NEW, boxType);
        init.visitInsn(DUP);
        init.visitMethodInsn(INVOKESPECIAL, boxType, "<init>", "()V", false);
        init.visitFieldInsn(PUTFIELD, models.name, profile.search, boxDescriptor);
        init.visitInsn(RETURN);
        init.visitMaxs(3, 1);
        init.visitEnd();

        ClientPatchPlugin.patch(models);
        Map<String, byte[]> classes = new HashMap<>();
        classes.put(modelName, bytes(models));
        for (String maidName : maidNames) classes.put(maidName, bytes(screenClass(maidName, modelName)));
        var loader = new RegressionTest.ByteLoader(classes);
        Field search = loader.loadClass(modelName).getDeclaredField(profile.search);
        search.setAccessible(true);
        for (String maidName : maidNames) openAndRebuild(loader.loadClass(maidName), search, initName);
        openAndRebuild(loader.loadClass(modelName), search, initName);
    }

    private static void openAndRebuild(Class<?> screenType, Field search, String initName) throws ReflectiveOperationException {
        Object screen = screenType.getConstructor().newInstance();
        var init = screenType.getMethod(initName);
        init.invoke(screen);
        SearchBox original = (SearchBox) search.get(screen);
        RegressionTest.check(original != null, "native init creates the search box on first open");
        original.cursorPos = 7;
        original.highlightPos = 4;
        original.displayPos = 2;

        init.invoke(screen);
        SearchBox rebuilt = (SearchBox) search.get(screen);
        RegressionTest.check(rebuilt != null && rebuilt != original, "native init rebuilds the search box");
        RegressionTest.check(rebuilt.cursorPos == 7 && rebuilt.highlightPos == 4 && rebuilt.displayPos == 2,
                "the inherited init restores the cursor, selection and viewport");
    }

    private static ClassNode screenClass(String name, String parent) {
        ClassNode node = new ClassNode();
        node.visit(V17, ACC_PUBLIC | ACC_SUPER, name.replace('.', '/'), null, parent.replace('.', '/'), null);
        var constructor = node.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
        return node;
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
