package dev.local.ysmenhancements;

import dev.local.ysmenhancements.compat.Reflect;
import dev.local.ysmenhancements.ui.ButtonPanel;
import dev.local.ysmenhancements.fixes.ScaleDefaults;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Executes the real loader slider arithmetic/input methods with only rendering and apply side effects stubbed. */
public final class SliderContractTest implements Opcodes {
    public static void main(String[] args) throws Exception {
        boolean neo=args[0].equals("neo");String original=neo?"net/neoforged/neoforge/client/gui/widget/ExtendedSlider":"net/minecraftforge/client/gui/widget/ForgeSlider";
        ClassNode source=read(args[1],original),mathSource=read(args[2],"net/minecraft/util/Mth");
        String owner="fixture/"+(neo?"NeoSlider":"ForgeSlider"),mathOwner=owner+"Math";
        Set<String> selected=neo?Set.of("getValue","setValue","setFractionalValue","snapToNearest","keyPressed"):
                Set.of("getValue","m_93611_","setSliderValue","snapToNearest","m_7933_");
        ClassNode slider=empty(owner),math=empty(mathOwner);Set<String> neededMath=new HashSet<>();Map<String,String> fields=new HashMap<>();
        for(MethodNode m:source.methods)if(selected.contains(m.name)){
            m.access=ACC_PUBLIC;
            for(AbstractInsnNode insn:m.instructions){
                if(insn instanceof FieldInsnNode f){fields.put(f.name,f.desc);f.owner=owner;}
                if(insn instanceof MethodInsnNode call){
                    if(call.owner.equals("net/minecraft/util/Mth")){neededMath.add(call.name+call.desc);call.owner=mathOwner;}
                    else if(call.owner.startsWith("net/"))call.owner=owner;
                }
            }
            slider.methods.add(m);
        }
        while(!neededMath.isEmpty()){
            String id=neededMath.iterator().next();neededMath.remove(id);
            if(math.methods.stream().anyMatch(m->(m.name+m.desc).equals(id)))continue;
            MethodNode m=mathSource.methods.stream().filter(v->(v.name+v.desc).equals(id)).findFirst().orElseThrow();
            for(AbstractInsnNode insn:m.instructions){
                if(insn instanceof FieldInsnNode)throw new AssertionError("Unexpected stateful math dependency");
                if(insn instanceof MethodInsnNode c&&c.owner.equals(mathSource.name)){neededMath.add(c.name+c.desc);c.owner=mathOwner;}
            }
            math.methods.add(m);
        }
        fields.put("minValue","D");fields.put("maxValue","D");fields.put("stepSize","D");fields.put("applications","I");
        for(var field:fields.entrySet())slider.fields.add(new FieldNode(ACC_PUBLIC,field.getKey(),field.getValue(),null,null));
        MethodNode update=new MethodNode(ACC_PUBLIC,neo?"updateMessage":"m_5695_","()V",null,null);update.instructions.add(new InsnNode(RETURN));slider.methods.add(update);
        MethodNode apply=new MethodNode(ACC_PUBLIC,neo?"applyValue":"m_5697_","()V",null,null);
        apply.instructions.add(new VarInsnNode(ALOAD,0));apply.instructions.add(new InsnNode(DUP));apply.instructions.add(new FieldInsnNode(GETFIELD,owner,"applications","I"));apply.instructions.add(new InsnNode(ICONST_1));apply.instructions.add(new InsnNode(IADD));apply.instructions.add(new FieldInsnNode(PUTFIELD,owner,"applications","I"));apply.instructions.add(new InsnNode(RETURN));slider.methods.add(apply);
        for(String name:new String[]{"getWidth","m_5711_"}){MethodNode m=new MethodNode(ACC_PUBLIC,name,"()I",null,null);m.instructions.add(new IntInsnNode(SIPUSH,120));m.instructions.add(new InsnNode(IRETURN));slider.methods.add(m);}
        Class<?> type=new RegressionTest.ByteLoader(Map.of(owner.replace('/','.'),bytes(slider),mathOwner.replace('/','.'),bytes(math))).loadClass(owner.replace('/','.'));
        Object obj=type.getConstructor().newInstance();Reflect.set(obj,"minValue",0.0);Reflect.set(obj,"maxValue",2.0);Reflect.set(obj,"stepSize",.1);
        Reflect.set(obj,neo?"width":"f_93618_",120);
        ButtonPanel.setSliderValue(obj,1,neo);check(obj,1,1,"typed value applies exactly once");
        ButtonPanel.setSliderValue(obj,1,neo);check(obj,1,1,"same value does not apply twice");
        ButtonPanel.keySlider(obj,262,0,0,neo);check(obj,1.1,2,"stepped right key updates model once");
        ButtonPanel.keySlider(obj,263,0,0,neo);check(obj,1,3,"stepped left key updates model once");
        ButtonPanel.setSliderValue(obj,99,neo);check(obj,2,4,"range clamps before apply");
        Reflect.set(obj,"stepSize",0.0);ButtonPanel.setSliderValue(obj,1,neo);
        ButtonPanel.keySlider(obj,262,0,0,neo);
        RegressionTest.check(Reflect.integer(obj,"applications")==6&&((Number)Reflect.call(obj,"getValue")).doubleValue()>1,"continuous key applies exactly once");
        Reflect.set(obj,"minValue",.2);Reflect.set(obj,"maxValue",3.0);Reflect.set(obj,"stepSize",.05);
        RegressionTest.check((Boolean)Reflect.call(ScaleDefaults.class,"display",obj,1.0,.2,3.0),"explicit author default representable in actual slider");
        check(obj,1,6,"default display initializes actual handle without a model write");
        ButtonPanel.keySlider(obj,262,0,0,neo);check(obj,1.05,7,"first input moves from author default rather than slider minimum");
        System.out.println("PASS: "+args[0]+" real slider input callbacks, clamp, non-mutating default handle and first step");
    }
    private static void check(Object obj,double value,int count,String label){RegressionTest.check(Math.abs(((Number)Reflect.call(obj,"getValue")).doubleValue()-value)<1e-9&&Reflect.integer(obj,"applications")==count,label);}
    private static ClassNode empty(String name){ClassNode n=new ClassNode();n.version=V17;n.access=ACC_PUBLIC;n.name=name;n.superName="java/lang/Object";MethodNode ctor=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new VarInsnNode(ALOAD,0));ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(RETURN));n.methods.add(ctor);return n;}
    private static byte[] bytes(ClassNode n){ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);n.accept(out);return out.toByteArray();}
    private static ClassNode read(String path,String name)throws IOException{try(ZipFile jar=new ZipFile(path);var in=jar.getInputStream(jar.getEntry(name+".class"))){ClassNode n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;}}
}
