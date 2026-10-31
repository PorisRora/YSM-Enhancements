package dev.local.ysmenhancements.fabric;

import dev.local.ysmenhancements.RegressionTest;
import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.fixes.ScaleDefaults;
import dev.local.ysmenhancements.ui.ButtonPanel;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static dev.local.ysmenhancements.RegressionTest.check;

/** Executes YSM's real Fabric slider arithmetic/input with rendering/model writes counted. */
public final class FabricSliderTest implements Opcodes {
    static ClassNode empty(String name,String parent) {
        ClassNode n=new ClassNode();n.version=V17;n.access=ACC_PUBLIC;n.name=name;n.superName=parent;
        MethodNode ctor=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new VarInsnNode(ALOAD,0));ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,parent,"<init>","()V",false));ctor.instructions.add(new InsnNode(RETURN));n.methods.add(ctor);return n;
    }
    public static void main(String[] args)throws Exception {
        var profile=YsmProfile.valueOf(args[0]);
        try(var ysm=new ZipFile(args[1]);var mc=new ZipFile(args[2])) {
            String owner=FabricRegressionTest.node(ysm,YsmProfile.ROOT+profile.slider()).superName;
            ClassNode source=FabricRegressionTest.node(ysm,owner),mathSource=FabricRegressionTest.node(mc,"net/minecraft/class_3532");
            ClassNode slider=empty(owner,"net/minecraft/class_357"),base=empty("net/minecraft/class_357","java/lang/Object"),math=empty("fixture/Math","java/lang/Object");
            Map<String,String> fields=new HashMap<>();Set<String> needed=new HashSet<>(),neededMath=new HashSet<>();
            for(var m:source.methods)for(String name:List.of("getValue","setValue","snapToNearest","keyPressed"))
                if(RuntimeNames.methodMatches(owner,m.name,m.desc,name))needed.add(m.name+m.desc);
            while(!needed.isEmpty()) {
                String id=needed.iterator().next();needed.remove(id);if(slider.methods.stream().anyMatch(m->(m.name+m.desc).equals(id)))continue;
                MethodNode m=source.methods.stream().filter(v->(v.name+v.desc).equals(id)).findFirst().orElseThrow();m.access=ACC_PUBLIC;
                for(var i:m.instructions) {
                    if(i instanceof FieldInsnNode f)fields.put(f.name,f.desc);
                    if(i instanceof MethodInsnNode c) {
                        if(c.owner.equals(mathSource.name)){neededMath.add(c.name+c.desc);c.owner=math.name;}
                        else if(c.owner.equals(owner)&&!Set.of("method_25346","method_25344").contains(c.name))needed.add(c.name+c.desc);
                    }
                }
                slider.methods.add(m);
            }
            while(!neededMath.isEmpty()) {
                String id=neededMath.iterator().next();neededMath.remove(id);if(math.methods.stream().anyMatch(m->(m.name+m.desc).equals(id)))continue;
                MethodNode m=mathSource.methods.stream().filter(v->(v.name+v.desc).equals(id)).findFirst().orElseThrow();
                for(var i:m.instructions){if(i instanceof FieldInsnNode)throw new AssertionError("Stateful math dependency");if(i instanceof MethodInsnNode c&&c.owner.equals(mathSource.name)){neededMath.add(c.name+c.desc);c.owner=math.name;}}
                math.methods.add(m);
            }
            for(var field:fields.entrySet()) {
                if(field.getKey().startsWith("field_"))base.fields.add(new FieldNode(ACC_PUBLIC,field.getKey(),field.getValue(),null,null));
                else slider.fields.add(new FieldNode(ACC_PUBLIC,field.getKey(),field.getValue(),null,null));
            }
            // Reflection resolves value/width on their actual declaring Minecraft classes.
            ClassNode widget=empty("net/minecraft/class_339","java/lang/Object");
            base.superName=widget.name;((MethodInsnNode)base.methods.get(0).instructions.get(1)).owner=widget.name;
            String width=RuntimeNames.field(widget.name,"width");base.fields.removeIf(f->f.name.equals(width));widget.fields.add(new FieldNode(ACC_PUBLIC,width,"I",null,null));
            slider.fields.add(new FieldNode(ACC_PUBLIC,"applications","I",null,null));
            MethodNode update=new MethodNode(ACC_PUBLIC,"method_25346","()V",null,null);update.instructions.add(new InsnNode(RETURN));slider.methods.add(update);
            MethodNode apply=new MethodNode(ACC_PUBLIC,"method_25344","()V",null,null);apply.instructions.add(new VarInsnNode(ALOAD,0));apply.instructions.add(new InsnNode(DUP));apply.instructions.add(new FieldInsnNode(GETFIELD,owner,"applications","I"));apply.instructions.add(new InsnNode(ICONST_1));apply.instructions.add(new InsnNode(IADD));apply.instructions.add(new FieldInsnNode(PUTFIELD,owner,"applications","I"));apply.instructions.add(new InsnNode(RETURN));slider.methods.add(apply);
            MethodNode getWidth=new MethodNode(ACC_PUBLIC,"method_25368","()I",null,null);getWidth.instructions.add(new IntInsnNode(SIPUSH,120));getWidth.instructions.add(new InsnNode(IRETURN));widget.methods.add(getWidth);
            Map<String,byte[]> all=new HashMap<>();for(var n:List.of(slider,base,widget,math))all.put(n.name.replace('/','.'),FabricRegressionTest.bytes(n));
            Class<?> type=new RegressionTest.ByteLoader(all).loadClass(owner.replace('/','.'));Object obj=type.getConstructor().newInstance();
            Reflect.set(obj,"minValue",0.0);Reflect.set(obj,"maxValue",2.0);Reflect.set(obj,"stepSize",.1);Reflect.set(obj,"width",120);
            ButtonPanel.setSliderValue(obj,1,false);verify(obj,1,1,"typed value applies once");
            ButtonPanel.setSliderValue(obj,1,false);verify(obj,1,1,"unchanged value does not write");
            ButtonPanel.keySlider(obj,262,0,0,false);verify(obj,1.1,2,"stepped right key applies once");
            ButtonPanel.keySlider(obj,263,0,0,false);verify(obj,1,3,"stepped left key applies once");
            ButtonPanel.setSliderValue(obj,99,false);verify(obj,2,4,"range clamp before apply");
            Reflect.set(obj,"stepSize",0.0);ButtonPanel.setSliderValue(obj,1,false);ButtonPanel.keySlider(obj,262,0,0,false);
            check(Reflect.integer(obj,"applications")==6&&((Number)Reflect.call(obj,"getValue")).doubleValue()>1,"continuous input applies once");
            Reflect.set(obj,"minValue",.2);Reflect.set(obj,"maxValue",3.0);Reflect.set(obj,"stepSize",.05);
            check((boolean)Reflect.call(ScaleDefaults.class,"display",obj,1.0,.2,3.0),"author default representable");
            verify(obj,1,6,"initial value does not write model");ButtonPanel.keySlider(obj,262,0,0,false);verify(obj,1.05,7,"first step starts from default");
            System.out.println("PASS: "+profile+" actual slider input, clamp, single callback and non-mutating default initialization");
        }
    }
    static void verify(Object slider,double value,int writes,String reason) {check(Math.abs(((Number)Reflect.call(slider,"getValue")).doubleValue()-value)<1e-9&&Reflect.integer(slider,"applications")==writes,reason);}
}
