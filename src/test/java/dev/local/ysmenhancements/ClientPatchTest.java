package dev.local.ysmenhancements;

import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.fixes.ClientFixes;
import dev.local.ysmenhancements.transform.ClientPatchPlugin;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public final class ClientPatchTest implements Opcodes {
    public static void main(String[] args) throws Exception {
        Object screen=new Object();int[] old={0},fresh={0};
        ClientFixes.begin(screen);ClientFixes.track(screen,old);
        RegressionTest.check(ClientFixes.current(screen,old),"current async callback allowed");
        ClientFixes.begin(screen);ClientFixes.track(screen,fresh);
        RegressionTest.check(!ClientFixes.current(screen,old),"late old callback rejected");
        RegressionTest.check(ClientFixes.current(screen,fresh),"new callback remains active");
        ClientFixes.screenChanging(null);
        RegressionTest.check(!ClientFixes.current(screen,fresh),"closed screen callback rejected");
        RegressionTest.check(ClientFixes.truth(-1)&&!ClientFixes.truth(0)&&!ClientFixes.truth(Float.NaN),"nonzero checkbox truth");
        RegressionTest.check(ClientFixes.number("1e-5")==1e-5f&&ClientFixes.number("-2.5E+2")==-250,"scientific notation");
        RegressionTest.check(ClientFixes.number("null")==0&&ClientFixes.number("broken")==0&&ClientFixes.number("true")==1,"safe scalar fallback");
        if(args.length==0)throw new IllegalArgumentException("Pass -PysmJar for actual YSM bytecode tests");
        try(ZipFile jar=new ZipFile(args[0])) {
            YsmProfile p=jar.getEntry((YsmProfile.ROOT+YsmProfile.NEO.wheel).replace('.','/')+".class")!=null?YsmProfile.NEO:YsmProfile.FORGE;
            for(String name:new String[]{p.wheel,p.models,p.keys,p.lock,p.input}) {
                ClassNode n=new ClassNode();try(var stream=jar.getInputStream(jar.getEntry((YsmProfile.ROOT+name).replace('.','/')+".class"))){new ClassReader(stream.readAllBytes()).accept(n,0);}
                ClientPatchPlugin.patch(n);
                for(MethodNode m:n.methods) if((m.access&(ACC_ABSTRACT|ACC_NATIVE))==0) new Analyzer<>(new BasicVerifier()).analyze(n.name,m);
                ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);n.accept(out);new ClassReader(out.toByteArray());
                if(name.equals(p.wheel)) parserExecution(n,p);
                System.out.println("PASS: bytecode structure and operand verification " + p+" "+name);
            }
        }
        System.out.println("PASS: client patch regression checks");
    }
    private static void parserExecution(ClassNode patched,YsmProfile p) throws Exception {
        String owner="fixture/PatchedNumber";ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        out.visit(V17,ACC_PUBLIC,owner,null,"java/lang/Object",null);
        var ctor=out.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
        for(MethodNode method:patched.methods) if(method.desc.equals("(Ljava/lang/String;)F")) {
            method.access=ACC_PUBLIC;
            for(var i:method.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(patched.name))c.owner=owner;
            method.accept(out);
        }
        out.visitEnd();Class<?> type=new RegressionTest.ByteLoader(Map.of(owner.replace('/','.'),out.toByteArray())).loadClass(owner.replace('/','.'));
        var parse=type.getMethod(p.b,String.class);Object obj=type.getConstructor().newInstance();
        for(String number:new String[]{"1e-5","-1","0","1.5","true","null"}) {
            float result=(Float)parse.invoke(obj,number);
            RegressionTest.check(result==ClientFixes.number(number),"actual transformed number parser "+number);
        }
    }
}
