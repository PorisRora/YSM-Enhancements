package dev.local.ysmenhancements.fabric;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Remaps a locally downloaded vanilla jar for bytecode-only Mixin/API tests.
 * This is not a game launcher and the generated jar must never be distributed. */
public final class MinecraftFixture {
    public static void main(String[] args) throws Exception {
        Map<String,String> classes=new HashMap<>(),fields=new HashMap<>(),methods=new HashMap<>();
        try(var tiny=new ZipFile(args[1])) {
            String owner="";
            for(String line:new String(tiny.getInputStream(tiny.getEntry("mappings/mappings.tiny")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                String[] p=line.stripTrailing().split("\t");
                if(p[0].equals("c")){owner=p[1];classes.put(owner,p[2]);}
                else if(p.length==5&&p[1].equals("m"))methods.put(owner+":"+p[3]+p[2],p[4]);
                else if(p.length==5&&p[1].equals("f"))fields.put(owner+":"+p[3]+p[2],p[4]);
            }
        }
        Map<String,List<String>> parents=new HashMap<>();
        try(var source=new ZipFile(args[0])) {
            for(var entry:Collections.list(source.entries()))if(entry.getName().endsWith(".class")) {
                var reader=new ClassReader(source.getInputStream(entry).readAllBytes());
                List<String> p=new ArrayList<>(Arrays.asList(reader.getInterfaces()));
                if(reader.getSuperName()!=null)p.add(0,reader.getSuperName());
                parents.put(reader.getClassName(),p);
            }
            Remapper remapper=new Remapper() {
                public String map(String name){return classes.getOrDefault(name,name);}
                String lookup(Map<String,String> map,String owner,String name,String desc) {
                    String direct=map.get(owner+":"+name+desc);if(direct!=null)return direct;
                    for(String parent:parents.getOrDefault(owner,List.of())){String inherited=lookup(map,parent,name,desc);if(inherited!=null)return inherited;}
                    return null;
                }
                public String mapMethodName(String owner,String name,String desc){String result=lookup(methods,owner,name,desc);return result==null?name:result;}
                public String mapFieldName(String owner,String name,String desc){String result=lookup(fields,owner,name,desc);return result==null?name:result;}
            };
            Path output=Path.of(args[2]);Files.createDirectories(output.getParent());
            try(var zip=new ZipOutputStream(Files.newOutputStream(output))) {
                for(var entry:Collections.list(source.entries()))if(entry.getName().endsWith(".class")) {
                    var reader=new ClassReader(source.getInputStream(entry).readAllBytes());var writer=new ClassWriter(0);
                    reader.accept(new ClassRemapper(writer,remapper),0);
                    zip.putNextEntry(new ZipEntry(remapper.map(reader.getClassName())+".class"));zip.write(writer.toByteArray());zip.closeEntry();
                }
            }
            System.out.println("Prepared vanilla bytecode fixture: "+output);
        }
    }
}
