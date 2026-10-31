package dev.local.ysmenhancements.compat;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Reflection bridge for the Fabric production namespace; identity on Forge/NeoForge. */
public final class RuntimeNames {
    private static final Map<String,String> CLASSES = new HashMap<>();
    private static final Map<String,String> FIELDS = new HashMap<>();
    private static final Map<String,String> METHODS = new HashMap<>();
    private static final Map<String,Set<String>> METHOD_ALIASES = new HashMap<>();
    private static final Map<String,Set<String>> LOCAL_ALIASES = new HashMap<>();
    private static final Pattern TYPE = Pattern.compile("L([^;]+);");
    static {
        try (InputStream in = RuntimeNames.class.getResourceAsStream("/ysm_enhancements.names.tsv")) {
            if (in != null) {
                List<String[]> rows = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().filter(s -> !s.startsWith("#")).map(s -> s.split("\t")).toList();
                for (String[] r : rows) if (r[0].equals("c")) CLASSES.put(r[1],r[2]);
                for (String[] r : rows) if (!r[0].equals("c")) {
                    String owner = CLASSES.getOrDefault(r[1],r[1]).replace('/','.');
                    if (r[0].equals("f")) FIELDS.put(owner+":"+r[2],r[3]);
                    else {
                        METHODS.put(r[1]+":"+r[2]+r[4],r[3]);
                        LOCAL_ALIASES.computeIfAbsent(owner+":"+r[3]+r[4],k->new HashSet<>()).add(r[2]);
                        if (r[3].startsWith("method_")) METHOD_ALIASES.computeIfAbsent(r[3],k->new HashSet<>()).add(r[2]);
                    }
                }
            }
        } catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    private RuntimeNames() {}
    public static String type(String name) { return CLASSES.getOrDefault(name.replace('.','/'),name.replace('.','/')).replace('/','.'); }
    public static String internal(String name) { return type(name).replace('.','/'); }
    public static String descriptor(String desc) { return TYPE.matcher(desc).replaceAll(m -> java.util.regex.Matcher.quoteReplacement("L"+internal(m.group(1))+";")); }
    public static String field(Class<?> owner,String name) { return field(owner.getName(),name); }
    public static String field(String owner,String name) { return FIELDS.getOrDefault(owner.replace('/','.')+":"+name,name); }
    public static boolean methodMatches(Method method,String requested) {
        return methodMatches(method.getDeclaringClass().getName(),method.getName(),org.objectweb.asm.Type.getMethodDescriptor(method),requested);
    }
    public static boolean methodMatches(String owner,String actual,String descriptor,String requested) {
        return actual.equals(requested) || METHOD_ALIASES.getOrDefault(actual,Set.of()).contains(requested)
            || LOCAL_ALIASES.getOrDefault(owner.replace('/','.')+":"+actual+descriptor,Set.of()).contains(requested);
    }
    public static String method(String owner,String name,String desc) {
        if (CLASSES.isEmpty()) return name;
        String mapped = METHODS.get(owner.replace('.','/')+":"+name+descriptor(desc));
        if (mapped == null) throw new IllegalStateException("Missing Fabric method mapping: "+owner+"."+name+desc);
        return mapped;
    }
}
