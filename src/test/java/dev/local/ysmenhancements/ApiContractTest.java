package dev.local.ysmenhancements;

import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.fixes.ScaleDefaults;
import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks reflection contracts against the actual jars without initializing game classes. */
public final class ApiContractTest {
    private final List<ZipFile> jars=new ArrayList<>();
    private final Map<String,ClassNode> nodes=new HashMap<>();
    private int checks;
    public static void main(String[] args) throws Exception {
        ApiContractTest audit=new ApiContractTest();
        try { for(String path:args)audit.jars.add(new ZipFile(path));audit.run(); }
        finally {for(ZipFile jar:audit.jars)jar.close();}
    }
    private ClassNode node(String owner) throws IOException {
        owner=owner.replace('.','/');if(nodes.containsKey(owner))return nodes.get(owner);
        byte[] bytes=null;
        for(ZipFile jar:jars){var entry=jar.getEntry(owner+".class");if(entry!=null){try(var in=jar.getInputStream(entry)){bytes=in.readAllBytes();}break;}}
        if(bytes==null)try(var in=ApiContractTest.class.getClassLoader().getResourceAsStream(owner+".class")){if(in!=null)bytes=in.readAllBytes();}
        ClassNode n=null;if(bytes!=null){n=new ClassNode();new ClassReader(bytes).accept(n,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);}
        nodes.put(owner,n);return n;
    }
    private MethodNode lookup(String owner,String names,String parameters,Set<String> seen) throws IOException {
        if(owner==null||!seen.add(owner))return null;ClassNode n=node(owner);if(n==null)return null;
        for(String name:names.split("\\|"))for(MethodNode m:n.methods)if(m.name.equals(name)&&m.desc.startsWith(parameters))return m;
        MethodNode result=lookup(n.superName,names,parameters,seen);if(result!=null)return result;
        for(String parent:n.interfaces){result=lookup(parent,names,parameters,seen);if(result!=null)return result;}return null;
    }
    private MethodNode method(String owner,String names,String parameters) throws IOException {
        MethodNode found=lookup(owner,names,parameters,new HashSet<>());
        if(found==null)throw new AssertionError("Missing method "+owner+"."+names+parameters);
        checks++;return found;
    }
    private FieldNode field(String owner,String names) throws IOException {
        for(String name:names.split("\\|"))for(ClassNode n=node(owner);n!=null;n=n.superName==null?null:node(n.superName))
            for(FieldNode f:n.fields)if(f.name.equals(name)){checks++;return f;}
        throw new AssertionError("Missing field "+owner+"."+names);
    }
    private static String owner(FieldNode field){return Type.getType(field.desc).getInternalName();}
    private void run() throws Exception {
        String root=YsmProfile.ROOT.replace('.','/');YsmProfile p=node(root+YsmProfile.NEO.wheel)!=null?YsmProfile.NEO:YsmProfile.FORGE;boolean neo=p==YsmProfile.NEO;
        String wheel=root+p.wheel,widget="net/minecraft/client/gui/components/AbstractWidget",screen="net/minecraft/client/gui/screens/Screen";
        for(String name:new String[]{p.x,p.y,p.scroll,p.max,p.up,p.down,p.selectedIndex})field(wheel,name);
        for(String name:new String[]{"width|f_96543_","height|f_96544_","renderables|f_169369_"})field(wheel,name);
        method(wheel,"init|m_7856_","()");method(wheel,"onClose|m_7379_","()");method(wheel,"children|m_6702_","()");
        for(String name:new String[]{p.configMethod,p.playMethod,p.folderMethod})method(wheel,name,"(Ljava/lang/String;)");
        method(wheel,p.configMethod,"()");
        String menu=owner(field(wheel,p.menu)),config=owner(field(wheel,p.selected)),anim=owner(field(wheel,p.animatable));
        field(wheel,p.configs);field(wheel,p.path);field(wheel,"ooOoO0OOO0oOo0Oo00oo000o|o0OooO00ooo0OO000O0OoOoO");
        method(menu,"size","()");method(menu,p.a,"(I)");method(menu,p.b,"(I)");method(config,p.b,"()");method(anim,p.entityMethod,"()");
        method(root+p.lock,p.a,"()");method(root+p.lock,p.b,"()");method(root+p.keys,p.a,"()");
        String resolver=root+(neo?"OoO0o00Oo0O00oOoO0OOO0oo":"OOOOooO0O0o0OOoOOo00oo00");
        String data=field(wheel,"ooOoO0OOO0oOo0Oo00oo000o|o0OooO00ooo0OO000O0OoOoO").desc;
        method(resolver,p.a,"("+data+"Ljava/lang/String;Ljava/lang/String;)");
        String geometry=Type.getReturnType(method(Type.getType(data).getInternalName(),p.a,"()").desc).getInternalName();
        String animation=root+(neo?"OOOOo0o0oOooO0oO0O00oOoO":"Oo0OOo00Oo00oO00Ooo0O00O");
        String bone=root+(neo?"Oo00OOooo0Ooo000ooooOOOo":"o0OOOOooooOO00oOoO0o00OO");
        for(String name:new String[]{p.configMethod,p.playMethod})
            RegressionTest.check(field(geometry,name).signature.contains(animation),"geometry map contains actual animation type");
        RegressionTest.check(field(animation,ScaleDefaults.Layout.of(p).bones()).signature.contains(bone),"animation bone list contract");
        FieldNode scale=field(bone,p.folderMethod);
        RegressionTest.check(scale.signature.startsWith("Ljava/util/List<"),"bone scale keyframe list contract");
        String slider=root+(neo?"o0OO0ooOOo0Ooo00OO0Oo0O0":"oo0ooOoOOOO0OOoO0OOoOOo0");
        String checkbox=root+(neo?"O0oo0oOOOoooO0o0oooOo000":"oOO0oo0OoOo0O0OOo0oooO0o");
        for(String name:new String[]{"minValue","maxValue","prefix","stepSize","value|f_93577_",p.playMethod})field(slider,name);
        method(slider,"snapToNearest","(D)");method(slider,"updateMessage|m_5695_","()");
        method(slider,"getValue","()");method(slider,"getValueString","()");method(slider,"setValue|m_93611_","(D)");
        method(slider,neo?"applyValue":"m_5697_","()");method(slider,"onClick|m_5716_","(DD)");method(slider,"keyPressed|m_7933_","(III)");
        method(checkbox,neo?p.a:"m_94620_","()");method(checkbox,"mouseClicked|m_6375_","(DDI)");
        for(String name:new String[]{"active|f_93623_","visible|f_93624_"})field(widget,name);
        for(String name:new String[]{"getX|m_252754_","getY|m_252907_","getWidth|m_5711_","getHeight|m_93694_","getMessage|m_6035_"})method(widget,name,"()");
        String g="Lnet/minecraft/client/gui/GuiGraphics;",font="Lnet/minecraft/client/gui/Font;",entity="Lnet/minecraft/world/entity/LivingEntity;";
        String graphics="net/minecraft/client/gui/GuiGraphics";
        method(graphics,"fill|m_280509_","(IIIII)");method(graphics,"drawString|m_280056_","("+font+"Ljava/lang/String;IIIZ)");
        method(graphics,"enableScissor|m_280588_","(IIII)");method(graphics,"disableScissor|m_280618_","()");
        method(graphics,"flush|m_280262_","()");
        method("net/minecraft/client/Minecraft","getSoundManager|m_91106_","()");method("net/minecraft/client/sounds/SoundManager","play|m_120367_","(Lnet/minecraft/client/resources/sounds/SoundInstance;)");
        method("net/minecraft/client/resources/sounds/SimpleSoundInstance","forUI|m_263171_","(Lnet/minecraft/core/Holder;F)");field("net/minecraft/sounds/SoundEvents","UI_BUTTON_CLICK|f_12490_");
        String pair="org/apache/commons/lang3/tuple/MutablePair";method(pair,"getLeft","()");method(pair,"getRight","()");method(pair,"setValue","(Ljava/lang/Object;)");
        RegressionTest.check(node(wheel).fields.stream().filter(f->(f.access&Opcodes.ACC_STATIC)!=0&&f.desc.equals("Ljava/util/LinkedList;")).count()==1,"wheel keeps one static folder trail");
        method("com/mojang/blaze3d/systems/RenderSystem","depthMask","(Z)");
        method("com/mojang/blaze3d/systems/RenderSystem","clearDepth","(D)");
        method("com/mojang/blaze3d/systems/RenderSystem","clear","(IZ)");
        method("org/lwjgl/opengl/GL11","glGetBoolean","(I)");method("org/lwjgl/opengl/GL11","glGetDouble","(I)");
        field("net/minecraft/client/gui/Font","lineHeight|f_92710_");
        method("net/minecraft/client/gui/Font","width|m_92895_","(Ljava/lang/String;)");
        method("net/minecraft/world/entity/Entity","getBbWidth|m_20205_","()");method("net/minecraft/world/entity/Entity","getBbHeight|m_20206_","()");
        method("net/minecraft/client/gui/screens/inventory/InventoryScreen",neo?"renderEntityInInventory":"m_280432_","("+g+(neo?"FFFLorg/joml/Vector3f;":"III")+"Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;"+entity+")");
        String living="net/minecraft/world/entity/LivingEntity";
        for(String name:new String[]{"yBodyRot|f_20883_","yHeadRot|f_20885_","yHeadRotO|f_20886_"})field(living,name);
        for(String name:new String[]{"getYRot|m_146908_","getXRot|m_146909_"})method(living,name,"()");
        for(String name:new String[]{"setYRot|m_146922_","setXRot|m_146926_"})method(living,name,"(F)");
        if(neo)method(living,"getScale","()");
        for(String[] input:new String[][]{{"mouseDragged|m_7979_","(DDIDD)"},{"mouseReleased|m_6348_","(DDI)"},{"mouseScrolled|m_6050_",neo?"(DDDD)":"(DDD)"},{"charTyped|m_5534_","(CI)"},{"keyPressed|m_7933_","(III)"}})method(screen,input[0],input[1]);
        String search=owner(field(root+p.models,p.search));for(String name:new String[]{"cursorPos|f_94101_","highlightPos|f_94102_","displayPos|f_94100_"})field(search,name);method(search,"getValue|m_94155_","()");
        String key="net/minecraft/client/KeyMapping";method(key,"matches|m_90832_","(II)");method(key,"matchesMouse|m_90830_","(I)");method(key,"getKeyModifier","()");method(key,"getKeyConflictContext","()");
        String event="L"+p.loaderRoot().replace('.','/')+"/client/event/InputEvent$Key;";
        for(String handler:p.keyHandlers)method(root+handler,p.a,"("+event+")");
        System.out.println("PASS: "+checks+" actual "+p+" reflection member/signature contracts");
    }
}
