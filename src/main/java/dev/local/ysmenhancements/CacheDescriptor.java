package dev.local.ysmenhancements;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Identifies the cache by its input-handler structure, not obfuscated names. */
public record CacheDescriptor(String owner, String keyField, String mouseField) {
    public static CacheDescriptor discover(Path modPath) throws IOException {
        List<CacheDescriptor> candidates = new ArrayList<>();
        if (Files.isDirectory(modPath)) {
            Path packageRoot = modPath.resolve("com/elfmcys");
            if (Files.isDirectory(packageRoot)) {
                try (var files = Files.walk(packageRoot)) {
                    for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                        try (InputStream stream = Files.newInputStream(file)) {
                            addCandidate(stream.readAllBytes(), candidates);
                        }
                    }
                }
            }
        } else {
            try (ZipFile zip = new ZipFile(modPath.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().startsWith("com/elfmcys/") || !entry.getName().endsWith(".class")) continue;
                    try (InputStream stream = zip.getInputStream(entry)) {
                        addCandidate(stream.readAllBytes(), candidates);
                    }
                }
            }
        }
        if (candidates.size() != 1) {
            throw new IOException("Expected exactly one YSM input cache, found " + candidates.size());
        }
        return candidates.get(0);
    }

    private static void addCandidate(byte[] bytes, List<CacheDescriptor> candidates) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Set<String> arrayFields = new HashSet<>();
        for (var field : node.fields) {
            int flags = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE;
            if (field.desc.equals("[Z") && (field.access & flags) == flags
                    && (field.access & Opcodes.ACC_FINAL) == 0) arrayFields.add(field.name);
        }
        if (arrayFields.size() != 2) return;
        String keys = null;
        String mouse = null;
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_STATIC) == 0) continue;
            if (method.desc.matches("\\(Lnet/(?:minecraftforge|neoforged/neoforge)/client/event/InputEvent\\$Key;\\)V")) {
                keys = writtenArray(node.name, method, arrayFields, "getKey");
            } else if (method.desc.matches("\\(Lnet/(?:minecraftforge|neoforged/neoforge)/client/event/InputEvent\\$MouseButton\\$Post;\\)V")) {
                mouse = writtenArray(node.name, method, arrayFields, "getButton");
            }
        }
        if (keys != null && mouse != null && !keys.equals(mouse)) {
            candidates.add(new CacheDescriptor(node.name, keys, mouse));
        }
    }

    private static String writtenArray(String owner, MethodNode method, Set<String> arrays, String getter) {
        Set<String> fields = new HashSet<>();
        boolean writesBoolean = false;
        boolean readsInputIndex = false;
        boolean readsAction = false;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner) && field.desc.equals("[Z") && arrays.contains(field.name)) {
                fields.add(field.name);
            }
            if (instruction.getOpcode() == Opcodes.BASTORE) writesBoolean = true;
            if (instruction instanceof MethodInsnNode call && call.desc.equals("()I")) {
                readsInputIndex |= call.name.equals(getter);
                readsAction |= call.name.equals("getAction");
            }
        }
        return writesBoolean && readsInputIndex && readsAction && fields.size() == 1
                ? fields.iterator().next() : null;
    }

    public CacheBinding bind(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> type = Class.forName(owner.replace('/', '.'), true, loader);
        Field keys = type.getField(keyField);
        Field mouse = type.getField(mouseField);
        validate(keys, 349);
        validate(mouse, 8);
        return new CacheBinding(keys, mouse);
    }

    private static void validate(Field field, int expectedLength) throws ReflectiveOperationException {
        int flags = field.getModifiers();
        if (field.getType() != boolean[].class || !Modifier.isPublic(flags)
                || !Modifier.isStatic(flags) || !Modifier.isVolatile(flags) || Modifier.isFinal(flags)
                || !((field.get(null) instanceof boolean[] state) && state.length == expectedLength)) {
            throw new IllegalAccessException("Unsupported YSM input field: " + field);
        }
    }
}
