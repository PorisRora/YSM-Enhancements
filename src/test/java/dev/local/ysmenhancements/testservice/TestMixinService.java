package dev.local.ysmenhancements.testservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

/** A test-only service for applying the real Sponge Mixin transformer. */
public final class TestMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public static final Map<String, byte[]> BYTECODE = new HashMap<>();
    public static ZipFile minecraft;
    public static ZipFile baseMinecraft;
    public static ZipFile ysm;
    @Override public String getName() { return "YSM Enhancements isolated test"; }
    @Override public boolean isValid() { return true; }
    @Override public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.DEFAULT; }
    @Override public IClassProvider getClassProvider() { return this; }
    @Override public IClassBytecodeProvider getBytecodeProvider() { return this; }
    @Override public ITransformerProvider getTransformerProvider() { return null; }
    @Override public IClassTracker getClassTracker() { return null; }
    @Override public IMixinAuditTrail getAuditTrail() { return null; }
    @Override public Collection<String> getPlatformAgents() { return List.of(); }
    @Override public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("ysm-enhancements-test"); }
    @Override public InputStream getResourceAsStream(String name) { return getClass().getClassLoader().getResourceAsStream(name); }
    @Override public URL[] getClassPath() { return new URL[0]; }
    @Override public Class<?> findClass(String name) throws ClassNotFoundException { return findClass(name, true); }
    @Override public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException { return Class.forName(name, initialize, getClass().getClassLoader()); }
    @Override public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException { return findClass(name, initialize); }
    @Override public ClassNode getClassNode(String name) throws IOException, ClassNotFoundException { return getClassNode(name, false, 0); }
    @Override public ClassNode getClassNode(String name, boolean runTransformers) throws IOException, ClassNotFoundException { return getClassNode(name, runTransformers, 0); }
    @Override public ClassNode getClassNode(String name, boolean runTransformers, int flags) throws IOException, ClassNotFoundException {
        byte[] bytes = BYTECODE.get(name.replace('/', '.'));
        String resource = name.replace('.', '/') + ".class";
        if (bytes == null && minecraft != null) {
            var entry = minecraft.getEntry(resource);
            if (entry != null) try (var stream = minecraft.getInputStream(entry)) { bytes = stream.readAllBytes(); }
        }
        if (bytes == null && baseMinecraft != null) {
            var entry = baseMinecraft.getEntry(resource);
            if (entry != null) try (var stream = baseMinecraft.getInputStream(entry)) { bytes = stream.readAllBytes(); }
        }
        if (bytes == null && ysm != null) {
            var entry = ysm.getEntry(resource);
            if (entry != null) try (var stream = ysm.getInputStream(entry)) { bytes = stream.readAllBytes(); }
        }
        if (bytes == null) {
            try (var stream = getResourceAsStream(resource)) {
                if (stream == null) throw new ClassNotFoundException(name);
                bytes = stream.readAllBytes();
            }
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, flags);
        return node;
    }
    public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
}
