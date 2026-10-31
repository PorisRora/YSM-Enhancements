package dev.local.ysmenhancements.fabric;

import dev.local.ysmenhancements.*;
import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.fixes.ClientFixes;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static dev.local.ysmenhancements.RegressionTest.check;

/** Executes actual YSM pure methods; game/native initialization is deliberately isolated. */
public final class FabricRegressionTest implements Opcodes {
    static ClassNode node(ZipFile jar,String name) throws Exception {
        ClassNode n=new ClassNode();new ClassReader(jar.getInputStream(jar.getEntry(name.replace('.','/')+".class")).readAllBytes()).accept(n,0);return n;
    }
    static byte[] bytes(ClassNode n) {var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);n.accept(w);return w.toByteArray();}
    public static void main(String[] args) throws Exception {
        var p=YsmProfile.valueOf(args[0]);
        RegressionTest.main(new String[0]);
        Object screen=new Object();int[] old={0},fresh={0};
        ClientFixes.begin(screen);ClientFixes.track(screen,old);check(ClientFixes.current(screen,old),"current callback");
        ClientFixes.begin(screen);ClientFixes.track(screen,fresh);check(!ClientFixes.current(screen,old),"reject stale generation");
        ClientFixes.screenChanging(null);check(!ClientFixes.current(screen,fresh),"reject closed screen callbacks");
        check(ClientFixes.truth(-1)&&!ClientFixes.truth(0)&&!ClientFixes.truth(Float.NaN),"negative boolean values");
        try(var jar=new ZipFile(args[1])) {
            for(String name:List.of(p.wheel,p.models,p.keys)) {
                ClassNode n=node(jar,YsmProfile.ROOT+name);FabricPatchPlugin.patch(n);
                for(var m:n.methods)if((m.access&(ACC_NATIVE|ACC_ABSTRACT))==0)new Analyzer<>(new BasicVerifier()).analyze(n.name,m);
                new ClassReader(bytes(n));
                if(name.equals(p.wheel))numbers(n,p);
                if(name.equals(p.keys))lock(jar,n,p);
                System.out.println("PASS: actual "+p+" class transformed and operand-verified: "+name);
            }
            ClassNode cache=node(jar,YsmProfile.ROOT+p.input);
            var loader=new RegressionTest.ByteLoader(Map.of(cache.name.replace('/','.'),bytes(cache)));
            CacheBinding binding=new CacheDescriptor(cache.name,p.a,p.b).bind(loader);
            RegressionTest.install(binding);
            ((boolean[])binding.keys().get(null))[341]=true;((boolean[])binding.mouse().get(null))[7]=true;
            InputResetHooks.onGui(new Object());
            check(!((boolean[])binding.keys().get(null))[341]&&!((boolean[])binding.mouse().get(null))[7],"actual YSM cache cleared on GUI");
            ((boolean[])binding.keys().get(null))[76]=true;InputResetHooks.onKey(76,0);
            check(!((boolean[])binding.keys().get(null))[76],"actual YSM cache physical release");
        }
        System.out.println("PASS: "+p+" input, callbacks, numeric parser, lock press/repeat/context/mouse regression checks");
    }
    static void numbers(ClassNode patched,YsmProfile p)throws Exception {
        ClassNode out=new ClassNode();out.version=V17;out.access=ACC_PUBLIC;out.name="fixture/Numbers";out.superName="java/lang/Object";
        MethodNode ctor=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new VarInsnNode(ALOAD,0));ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(RETURN));out.methods.add(ctor);
        for(var original:patched.methods)if(original.desc.equals("(Ljava/lang/String;)F")) {
            MethodNode m=new MethodNode(ASM9,ACC_PUBLIC,original.name,original.desc,null,null);original.accept(m);m.access=ACC_PUBLIC;
            for(var i:m.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(patched.name))c.owner=out.name;
            out.methods.add(m);
        }
        Class<?> type=new RegressionTest.ByteLoader(Map.of("fixture.Numbers",bytes(out))).loadClass("fixture.Numbers");
        Object instance=type.getConstructor().newInstance();
        for(String value:List.of("1e-5","-2.5E+2","-1","0","1.5","true","on","null","NaN","Infinity","bad"))
            check((float)type.getMethod(p.b,String.class).invoke(instance,value)==ClientFixes.number(value),"actual numeric parser: "+value);
    }
    static void lock(ZipFile jar,ClassNode patched,YsmProfile p)throws Exception {
        Map<String,byte[]> classes=new HashMap<>();
        ClassNode pure=node(jar,YsmProfile.ROOT+p.lock);
        pure.methods.removeIf(m->!m.name.equals("<clinit>")&&!(m.desc.equals("()V")&&!m.name.equals("<init>"))&&!m.desc.equals("()Z"));
        classes.put(pure.name.replace('/','.'),bytes(pure));
        ClassNode callback=new ClassNode();callback.version=V17;callback.access=ACC_PUBLIC;callback.name="fixture/LockCallback";callback.superName="java/lang/Object";
        MethodNode target=patched.methods.stream().filter(m->m.name.equals(p.b)&&m.desc.equals("(Lnet/minecraft/class_304;)V")).findFirst().orElseThrow();
        target.access=ACC_PUBLIC|ACC_STATIC;callback.methods.add(target);classes.put("fixture.LockCallback",bytes(callback));
        // The actual default YSM KeyBinding subclass dispatches every raw true state.
        String builder=patched.methods.stream().filter(m->m.name.equals("<clinit>")).flatMap(m->Arrays.stream(m.instructions.toArray()))
            .filter(i->i instanceof TypeInsnNode t&&t.getOpcode()==NEW).map(i->((TypeInsnNode)i).desc).findFirst().orElseThrow();
        ClassNode binding=node(jar,builder+"$"+p.a);
        MethodNode ctor=binding.methods.stream().filter(m->m.name.equals("<init>")).findFirst().orElseThrow();
        String handler=Type.getArgumentTypes(ctor.desc)[4].getClassName();
        ClassNode handlerNode=node(jar,handler);
        classes.put(binding.name.replace('/','.'),bytes(binding));classes.put(handler,bytes(handlerNode));
        // The down-handler interface is referenced by the real registration lambda.
        String down=patched.methods.stream().filter(m->m.name.equals("<clinit>")).flatMap(m->Arrays.stream(m.instructions.toArray()))
            .filter(i->i instanceof InvokeDynamicInsnNode d&&d.name.equals("handleKeyDown"))
            .map(i->Type.getReturnType(((InvokeDynamicInsnNode)i).desc).getClassName()).findFirst().orElseThrow();
        classes.put(down,bytes(node(jar,down)));
        var loader=new RegressionTest.ByteLoader(classes);
        Class<?> cb=loader.loadClass("fixture.LockCallback"), downType=loader.loadClass(down),handlerType=loader.loadClass(handler),bindingType=loader.loadClass(binding.name.replace('/','.'));
        Method toggle=cb.getMethod(p.b,net.minecraft.class_304.class);
        Method defaultHandle=downType.getMethod("handle",net.minecraft.class_304.class,boolean.class);
        Object listener=Proxy.newProxyInstance(loader,new Class<?>[]{downType},(proxy,method,values)->{
            if(method.getName().equals("handle"))return InvocationHandler.invokeDefault(proxy,defaultHandle,values);
            if(method.getName().equals("handleKeyDown")){toggle.invoke(null,values[0]);return null;}
            throw new UnsupportedOperationException(method.toString());
        });
        var constructor=bindingType.getDeclaredConstructor(String.class,net.minecraft.class_3675.class_307.class,int.class,String.class,handlerType);constructor.setAccessible(true);
        var key=(net.minecraft.class_304)constructor.newInstance("lock",null,76,"ysm",listener);
        var locked=loader.loadClass(pure.name.replace('/','.')).getMethod(p.b);
        press(key,false,76,0,1);check((boolean)locked.invoke(null),"initial lock press");
        press(key,false,76,0,2);press(key,false,76,0,2);check((boolean)locked.invoke(null),"repeats do not toggle");
        ShortcutGuard.end();key.method_23481(true);check((boolean)locked.invoke(null),"held-key resync does not toggle");
        press(key,false,76,0,0);press(key,false,76,0,1);check(!(boolean)locked.invoke(null),"second physical press toggles once");
        var mc=net.minecraft.class_310.INSTANCE;
        mc.field_1755=new Object();press(key,false,76,0,1);check(!(boolean)locked.invoke(null),"GUI blocks lock");mc.field_1755=null;
        mc.field_18175=new Object();press(key,false,76,0,1);check(!(boolean)locked.invoke(null),"overlay blocks lock");mc.field_18175=null;
        mc.focused=false;press(key,false,76,0,1);check(!(boolean)locked.invoke(null),"focus loss blocks lock");mc.focused=true;
        key.modifierAllowed=false;press(key,false,76,0,1);check(!(boolean)locked.invoke(null),"provider modifier filter respected");key.modifierAllowed=true;
        press(key,false,257,0,1);check(!(boolean)locked.invoke(null),"unrelated physical event cannot authorize held-key resync");
        key.mouse=true;key.key=4;press(key,true,4,0,1);check((boolean)locked.invoke(null),"mouse-bound lock still works");
        ShortcutGuard.end();key.method_23481(true);check((boolean)locked.invoke(null),"mouse state resync ignored");
        key.mouse=false;key.scan=30;press(key,false,-1,30,1);check(!(boolean)locked.invoke(null),"scan-code binding works");
    }
    static void press(net.minecraft.class_304 key,boolean mouse,int code,int scan,int action) {
        ShortcutGuard.begin(mouse,code,scan,action);
        try {key.method_23481(action!=0);} finally {ShortcutGuard.end();}
    }
}
