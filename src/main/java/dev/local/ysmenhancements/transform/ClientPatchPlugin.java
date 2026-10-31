package dev.local.ysmenhancements.transform;

import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.compat.RuntimeNames;
import dev.local.ysmenhancements.ui.ButtonPanel;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.*;

/** Exact-version transformations of user-provided YSM classes; no YSM code is redistributed. */
public final class ClientPatchPlugin implements IMixinConfigPlugin, Opcodes {
    private static final String FIX="dev/local/ysmenhancements/fixes/ClientFixes";
    private static final String KEYS="dev/local/ysmenhancements/fixes/ShortcutFixes";
    private static final String PANEL="dev/local/ysmenhancements/ui/ButtonPanel";
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target,String mixin) { return true; }
    @Override public void acceptTargets(Set<String> mine,Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void postApply(String name,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void preApply(String name,ClassNode node,String mixin,IMixinInfo info) {
        if (mixin.endsWith("YsmClientMixin")) patch(node);
    }
    public static void patch(ClassNode node) {
        YsmProfile p=YsmProfile.ofName(node.name.replace('/','.'));
        if (node.name.endsWith("/"+p.wheel)) wheel(node,p);
        else if (node.name.endsWith("/"+p.models)) {
            MethodNode init=find(node,mc(p,"init","m_7856_","()V"),"()V");
            init.instructions.insert(objectHook("captureCaret"));
            returns(init,()->objectHook("restoreCaret"));
        } else if (node.name.endsWith("/"+p.keys)) {
            MethodNode match=node.methods.stream().filter(m->m.desc.endsWith("Lnet/minecraft/client/KeyMapping;)Z")).findFirst().orElseThrow();
            match.instructions.clear(); match.tryCatchBlocks.clear(); if(match.localVariables!=null)match.localVariables.clear();
            match.instructions.add(new VarInsnNode(ALOAD,0)); match.instructions.add(new VarInsnNode(ALOAD,1));
            match.instructions.add(call(KEYS,"matches","(Ljava/lang/Object;Ljava/lang/Object;)Z")); match.instructions.add(new InsnNode(IRETURN));
            match.maxStack=2;match.maxLocals=2;
        } else if (node.name.endsWith("/"+p.lock)) {
            int count=0;
            for (MethodNode m:node.methods) for(AbstractInsnNode insn:m.instructions.toArray()) if(insn instanceof MethodInsnNode c && c.owner.equals("net/minecraft/client/KeyMapping") && c.desc.equals("(II)Z")) {
                m.instructions.set(c,call(KEYS,"lockMatches","(Ljava/lang/Object;II)Z")); count++;
            }
            require(count==1,"lock matcher",count);
        } else if (node.name.endsWith("/"+p.input)) {
            MethodNode mouse=node.methods.stream().filter(m->m.desc.contains("InputEvent$MouseButton$Post;")).findFirst().orElseThrow();
            returns(mouse,()->{InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(call(KEYS,"mouseEvent","(Ljava/lang/Object;)V"));return l;});
        }
        for (MethodNode m : node.methods) m.maxStack += 8;
    }
    private static void wheel(ClassNode node,YsmProfile p) {
        MethodNode init=find(node,mc(p,"init","m_7856_","()V"),"()V");
        init.instructions.insert(objectHook("begin"));
        MethodNode parser=find(node,p.b,"(Ljava/lang/String;)F");
        MethodNode original=rename(node,parser);
        LabelNode legacy=new LabelNode();
        parser.instructions.add(new LdcInsnNode("NUMERIC_VALUES")); parser.instructions.add(call(FIX,"enabled","(Ljava/lang/String;)Z"));
        parser.instructions.add(new JumpInsnNode(IFEQ,legacy)); parser.instructions.add(new VarInsnNode(ALOAD,1));
        parser.instructions.add(call(FIX,"number","(Ljava/lang/String;)F")); parser.instructions.add(new InsnNode(FRETURN));
        parser.instructions.add(legacy);parser.instructions.add(new FrameNode(F_SAME,0,null,0,null)); delegate(node,parser,original);
        int truth=0,track=0,guards=0,clip=0;
        for(MethodNode m:List.copyOf(node.methods)) {
            Type[] args=Type.getArgumentTypes(m.desc);
            if(m.name.startsWith("ysm$"))continue;
            if(args.length==3 && args[1].getDescriptor().equals("[I") && args[2].getDescriptor().equals("[I") && Type.getReturnType(m.desc).equals(Type.VOID_TYPE)) {
                InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ALOAD,2));l.add(call(FIX,"track","(Ljava/lang/Object;[I)V"));m.instructions.insert(l);track++;
            }
            if(Type.getReturnType(m.desc).equals(Type.VOID_TYPE) && Arrays.stream(args).anyMatch(t->t.getDescriptor().equals("Ljava/lang/String;")) && Arrays.stream(args).anyMatch(t->t.getDescriptor().equals("[I"))) {
                MethodNode old=rename(node,m);int slot=1;for(Type t:args){if(t.getDescriptor().equals("[I"))break;slot+=t.getSize();}
                LabelNode valid=new LabelNode();m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,slot));m.instructions.add(call(FIX,"current","(Ljava/lang/Object;[I)Z"));m.instructions.add(new JumpInsnNode(IFNE,valid));m.instructions.add(new InsnNode(RETURN));m.instructions.add(valid);m.instructions.add(new FrameNode(F_SAME,0,null,0,null));delegate(node,m,old);guards++;
            }
        }
        for(MethodNode m:node.methods) {
            boolean writes=false;
            for(AbstractInsnNode insn:m.instructions.toArray()) {
                if(insn instanceof FieldInsnNode f && f.getOpcode()==PUTFIELD && f.owner.equals(node.name) && (f.name.equals(p.scroll)||f.name.equals(p.max))) writes=true;
                if(insn.getOpcode()==FCMPL && previous(insn).getOpcode()==FCONST_0 && next(insn) instanceof JumpInsnNode jump && jump.getOpcode()==IFLE) {
                    // The only f > 0 test in either supported wheel class is checkbox initialization.
                    m.instructions.remove(previous(insn));m.instructions.set(insn,call(FIX,"truth","(F)Z"));jump.setOpcode(IFEQ);truth++;
                }
                if(insn instanceof MethodInsnNode c && c.owner.equals(RuntimeNames.internal("net/minecraft/client/gui/components/events/GuiEventListener")) && c.desc.equals("(DDI)Z") && (c.name.equals(mc(p,"mouseClicked","m_6375_","(DDI)Z")))) {
                    InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(DLOAD,3));m.instructions.insertBefore(c,l);
                    m.instructions.set(c,call(FIX,"clippedClick","(Ljava/lang/Object;DDILjava/lang/Object;D)Z"));clip++;
                }
            }
            if(writes && !m.name.equals("<init>")) returns(m,()->objectHook("arrows"));
        }
        MethodNode render=find(node,mc(p,"render","m_88315_","(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"),RuntimeNames.descriptor("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"));
        render.instructions.insert(objectHook("arrows"));
        require(truth==1,"boolean comparison",truth);require(track==1,"callback token",track);require(guards>=5,"callback guards",guards);require(clip==1,"clipped click",clip);
        String slider=YsmProfile.ROOT.replace('.','/')+p.slider();
        int defaults=0;
        for(MethodNode m:node.methods)if(Type.getReturnType(m.desc).getDescriptor().equals("L"+slider+";")) {
            Type[] args=Type.getArgumentTypes(m.desc);
            require(args.length==4&&args[1].getDescriptor().equals("Ljava/lang/String;"),"slider factory arguments",args.length);
            for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==ARETURN) {
                InsnList l=new InsnList();l.add(new InsnNode(DUP));l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ALOAD,2));
                l.add(call("dev/local/ysmenhancements/fixes/ScaleDefaults","created","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V"));m.instructions.insertBefore(i,l);defaults++;
            }
        }
        require(defaults==1,"scale default hook",defaults);
        panel(node,p,init,render);
    }
    private static void panel(ClassNode node,YsmProfile p,MethodNode init,MethodNode render) {
        // A compile-time constant: inlined, so the transformer never loads the panel class.
        node.fields.add(new FieldNode(ACC_PRIVATE,ButtonPanel.STATE_FIELD,"Ljava/lang/Object;",null,null));
        InsnList rebuilt=new InsnList();rebuilt.add(new VarInsnNode(ALOAD,0));rebuilt.add(call(PANEL,"rebuilt","(Ljava/lang/Object;)V"));init.instructions.insert(rebuilt);
        MethodNode oldRender=rename(node,render);LabelNode originalRender=new LabelNode();
        render.instructions.add(new VarInsnNode(ALOAD,0));render.instructions.add(new VarInsnNode(ALOAD,1));render.instructions.add(new VarInsnNode(ILOAD,2));render.instructions.add(new VarInsnNode(ILOAD,3));render.instructions.add(new VarInsnNode(FLOAD,4));
        render.instructions.add(call(PANEL,"render","(Ljava/lang/Object;Ljava/lang/Object;IIF)Z"));render.instructions.add(new JumpInsnNode(IFEQ,originalRender));render.instructions.add(new InsnNode(RETURN));render.instructions.add(originalRender);render.instructions.add(new FrameNode(F_SAME,0,null,0,null));delegate(node,render,oldRender);
        boolean neo=p.modern();
        panelInput(node,mc(p,"mouseClicked","m_6375_","(DDI)Z"),"(DDI)Z","clicked",false);
        panelInput(node,mc(p,"mouseDragged","m_7979_","(DDIDD)Z"),"(DDIDD)Z","dragged",false);
        panelInput(node,mc(p,"mouseReleased","m_6348_","(DDI)Z"),"(DDI)Z","released",false);
        panelInput(node,mc(p,"mouseScrolled","m_6050_",neo?"(DDDD)Z":"(DDD)Z"),neo?"(DDDD)Z":"(DDD)Z","scrolled",false);
        panelInput(node,mc(p,"charTyped","m_5534_","(CI)Z"),"(CI)Z","character",false);
        panelInput(node,mc(p,"keyPressed","m_7933_","(III)Z"),"(III)Z","key",true);
    }
    private static String mc(YsmProfile p,String named,String srg,String desc) {
        if (!p.fabric()) return p==YsmProfile.NEO ? named : srg;
        String owner = switch(named) {
            case "init", "render" -> "net/minecraft/client/gui/screens/Screen";
            default -> "net/minecraft/client/gui/components/events/GuiEventListener";
        };
        return RuntimeNames.method(owner,named,desc);
    }
    private static void panelInput(ClassNode node,String name,String desc,String hook,boolean fallThrough) {
        MethodNode method=node.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElse(null),old=null;
        if(method==null){method=new MethodNode(ASM9,ACC_PUBLIC,name,desc,null,null);method.maxStack=16;method.maxLocals=1+Arrays.stream(Type.getArgumentTypes(desc)).mapToInt(Type::getSize).sum();node.methods.add(method);}
        else old=rename(node,method);
        LabelNode original=new LabelNode();
        if(!fallThrough){method.instructions.add(new VarInsnNode(ALOAD,0));method.instructions.add(call(PANEL,"active","(Ljava/lang/Object;)Z"));method.instructions.add(new JumpInsnNode(IFEQ,original));}
        method.instructions.add(new VarInsnNode(ALOAD,0));int slot=1;
        for(Type type:Type.getArgumentTypes(desc)){
            if(!(hook.equals("scrolled")&&desc.equals("(DDDD)Z")&&slot==5))method.instructions.add(new VarInsnNode(type.getOpcode(ILOAD),slot));
            slot+=type.getSize();
        }
        String hookDesc=hook.equals("scrolled")?"(Ljava/lang/Object;DDD)Z":"(Ljava/lang/Object;"+desc.substring(1);
        method.instructions.add(call(PANEL,hook,hookDesc));
        if(fallThrough){method.instructions.add(new JumpInsnNode(IFEQ,original));method.instructions.add(new InsnNode(ICONST_1));}
        method.instructions.add(new InsnNode(IRETURN));method.instructions.add(original);method.instructions.add(new FrameNode(F_SAME,0,null,0,null));
        if(old!=null)delegate(node,method,old);
        else{
            method.instructions.add(new VarInsnNode(ALOAD,0));slot=1;
            for(Type type:Type.getArgumentTypes(desc)){method.instructions.add(new VarInsnNode(type.getOpcode(ILOAD),slot));slot+=type.getSize();}
            method.instructions.add(new MethodInsnNode(INVOKESPECIAL,node.superName,name,desc,false));method.instructions.add(new InsnNode(IRETURN));
        }
    }
    private static MethodNode rename(ClassNode node,MethodNode original) {
        MethodNode copy=new MethodNode(ASM9,original.access,"ysm$original$"+node.methods.size(),original.desc,null,original.exceptions.toArray(String[]::new));
        original.accept(copy);copy.name="ysm$original$"+node.methods.size();copy.access=(original.access & ~(ACC_PUBLIC|ACC_PROTECTED))|ACC_PRIVATE;
        node.methods.add(copy);original.instructions=new InsnList();original.tryCatchBlocks=new ArrayList<>();original.localVariables=new ArrayList<>();
        original.maxStack=16;original.maxLocals=1+Arrays.stream(Type.getArgumentTypes(original.desc)).mapToInt(Type::getSize).sum();return copy;
    }
    private static void delegate(ClassNode node,MethodNode method,MethodNode original) {
        method.instructions.add(new VarInsnNode(ALOAD,0));int slot=1;
        for(Type type:Type.getArgumentTypes(method.desc)){method.instructions.add(new VarInsnNode(type.getOpcode(ILOAD),slot));slot+=type.getSize();}
        method.instructions.add(new MethodInsnNode(INVOKESPECIAL,node.name,original.name,original.desc,false));
        method.instructions.add(new InsnNode(Type.getReturnType(method.desc).getOpcode(IRETURN)));
    }
    private static MethodNode find(ClassNode n,String name,String desc) {return n.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new IllegalStateException("Missing YSM method "+name+desc));}
    private static InsnList objectHook(String name){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(call(FIX,name,"(Ljava/lang/Object;)V"));return l;}
    private static MethodInsnNode call(String owner,String name,String desc){return new MethodInsnNode(INVOKESTATIC,owner,name,desc,false);}
    private static void returns(MethodNode m,java.util.function.Supplier<InsnList> hook){for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==RETURN)m.instructions.insertBefore(i,hook.get());}
    private static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
    private static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
    private static void require(boolean ok,String kind,int count){if(!ok)throw new IllegalStateException("Unsupported YSM 2.6.5 layout: "+kind+" count="+count);}
}
