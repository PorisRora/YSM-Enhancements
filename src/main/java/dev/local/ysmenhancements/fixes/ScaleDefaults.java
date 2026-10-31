package dev.local.ysmenhancements.fixes;

import dev.local.ysmenhancements.compat.Reflect;
import dev.local.ysmenhancements.compat.YsmProfile;
import dev.local.ysmenhancements.config.Feature;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

/** Reads existing scale ASTs only. Never evaluates scripts or writes model variables. */
public final class ScaleDefaults {
    private static final Map<Object,Map<String,Double>> CACHE=new WeakHashMap<>();
    private static final Pattern BINDING=Pattern.compile("(?:v|variable)\\.roaming\\.[a-z0-9_]+",Pattern.CASE_INSENSITIVE);
    private static final int LIMIT=100_000;
    private ScaleDefaults() {}

    public record Layout(String parsed,String value,String ast,String number,String ternary,String binary,
                         String access,String variable,String scoped,String pool,String bones) {
        public static Layout of(YsmProfile p) {
            if (p==YsmProfile.FABRIC_1201) return new Layout(
                "oo0oooO0OoOOo0o00Oo0oOO0","oOOOo0oOo00OooOO000OOoo0","OOoo0OoO0o0O0oOOoO00ooOO",
                "o0oooOoooOo0O0oOoOOo00OO","o00oOOOo000O00Oo00O0Oooo","O0Oo00oo0Oo00OOOo0o0o0oO",
                "ooooooO0Oo000OO0oOO00Oo0","OOoOoO0Oooo0OoO00oOoO0oo","ooOoOoOooooO0OOO0o00oOOO",
                "OO0o0OooOOooO00000o0oO0o","o0OO00OoOo0oOoOOo0OOOO00");
            if (p==YsmProfile.FABRIC_1211) return new Layout(
                "O00ooO000ooOOOo0O0000Ooo","oOo0oO0O0ooOO0o0O0OOoo0O","OooOO0oO0O0OO00OoO000OoO",
                "oooOOo0oOoooo0oo0oooO0Oo","O00O00oooOooO0O0O0000ooo","O000000oO0OoOOoooOOoOooO",
                "OoO00OOoo0O0oOooo0OooooO","o0o0OOooo0ooo0OooOoO0O00","o000OoO0o00ooo0oOo00oO00",
                "ooo0OooOOOO0o0OO0Oo0oooO","Oo0OO0o0Ooo0OO0o0oOo00oo");
            return p==YsmProfile.NEO
                ?new Layout("O0o00OOOOOooooo000o0oo00","o0oOo00o0oOOOOOo00o0O0oo","O00OO0O000oO0ooOOO0o0OOO",
                    "o00o000oooOoOO0oO0O0o0OO","ooo0OOOo0oO0o0oOOoOOo0Oo","o0O0oo0o0ooOOO0ooooO0o0o",
                    "Oo00OOO00000ooOo0oOO0oO0","O0OO00oo0ooo0Oo0O0OO0OOo","o0OOOooo000O0O0Oo0O0Oo00",
                    "oO0oO0ooO000OOooOoo00OO0","oOOO00ooO0oOOoOOo0OoOOOo")
                :new Layout("o0o0OoOoO0Oo0oOoOOOooOOo","O0o0OOO00000oO00O00oOOo0","oOOOOoo0O0Oo0O00oo0oooO0",
                    "OoO00Oo0OoO0o000oOOO0Ooo","O00ooOoo0Oooo0O00oo0OoO0","oo00OOOoo00000Oo0Oooo0Oo",
                    "o0o00ooOO0oOo0oO000oOOOO","oOo0O0o0Oooo0o0oO0oOOO0o","ooo0oooo0OoO0oO0OOooO00o",
                    "oOO0OO0oO0oooo00OooooO0O","oooooooOOoOOoO00OooOo00O");
        }
    }

    /** Injected after the original slider factory, before the widget is published. */
    public static void created(Object widget,Object screen,String raw) {
        if(!Feature.SCALE_DEFAULTS.enabled()||!zero(raw))return;
        double min=((Number)Reflect.get(widget,"minValue")).doubleValue(),max=((Number)Reflect.get(widget,"maxValue")).doubleValue();
        if(!outsideZero(min,max))return; // Zero may be an intentional, selectable value.
        try {
            YsmProfile p=YsmProfile.of(screen);
            String binding=canonical((String)Reflect.get(widget,p.sliderBinding()));
            if(binding==null)return;
            Object data=Reflect.get(screen,p.data());
            Map<String,Double> defaults;
            synchronized(CACHE){defaults=CACHE.computeIfAbsent(data,key->readModel(key,p));}
            Double initial=defaults.get(binding);
            if(initial!=null)display(widget,initial,min,max);
        } catch(RuntimeException|LinkageError error) {
            // This optional view fix must never prevent opening a model's settings.
            System.getLogger("ysm_enhancements").log(System.Logger.Level.WARNING,"[YSM patch] Scale default unavailable; retaining original slider",error);
        }
    }

    static boolean zero(String raw) {
        if(raw==null||raw.equals("null"))return true;
        try{return Double.parseDouble(raw)==0;}catch(NumberFormatException e){return false;}
    }
    static boolean outsideZero(double min,double max){return Double.isFinite(min)&&Double.isFinite(max)&&min!=max&&(Math.min(min,max)>0||Math.max(min,max)<0);}
    static String canonical(String text) {
        if(text==null)return null;String s=text.strip().toLowerCase(Locale.ROOT);
        return BINDING.matcher(s).matches()?"v."+s.substring(s.indexOf('.')+1):null;
    }
    static boolean display(Object widget,double initial,double min,double max) {
        if(!Double.isFinite(initial)||initial<Math.min(min,max)||initial>Math.max(min,max))return false;
        double fraction=(initial-min)/(max-min);
        double snapped=((Number)Reflect.call(widget,"snapToNearest",fraction)).doubleValue();
        double value=min+snapped*(max-min);
        if(Math.abs(value-initial)>Math.max(1,Math.abs(initial))*1e-6)return false;
        // Calling setValue/applyValue here would modify and synchronize the model.
        Reflect.set(widget,"value|f_93577_",snapped);
        Reflect.call(widget,"updateMessage|m_5695_");
        return true;
    }

    public static Map<String,Double> readModel(Object data,YsmProfile p) {
        Object geometry=Reflect.call(data,p.a);
        Analysis result=new Analysis(p);Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(String mapField:new String[]{p.configMethod,p.playMethod}) {
            for(Object animation:((Map<?,?>)Reflect.get(geometry,mapField)).values()) {
                for(Object bone:(List<?>)Reflect.get(animation,result.layout.bones)) {
                    result.keyframes(Reflect.get(bone,p.folderMethod),seen,0);
                }
            }
        }
        return result.finish();
    }

    /** Also used by isolated tests with the real YSM parser and AST classes. */
    public static Map<String,Double> analyzeExpressions(Iterable<?> expressions,YsmProfile p) {
        Analysis result=new Analysis(p);
        for(Object value:expressions)result.expression(value);
        return result.finish();
    }

    private static final class Analysis {
        final YsmProfile p;final Layout layout;final Class<?> valueType,astType;
        final Map<String,Double> defaults=new HashMap<>();final Set<String> blocked=new HashSet<>();int visited;
        Analysis(YsmProfile p){this.p=p;layout=Layout.of(p);valueType=Reflect.type(YsmProfile.ROOT+layout.value);astType=Reflect.type(YsmProfile.ROOT+layout.ast);}
        boolean kind(Object o,String type){return o!=null&&o.getClass().getName().equals(YsmProfile.ROOT+type);}
        Object first(Object o){return Reflect.get(o,p.a);}Object second(Object o){return Reflect.get(o,p.b);}Object third(Object o){return Reflect.get(o,p.configMethod);}
        void budget(int depth){if(++visited>LIMIT||depth>64)throw new IllegalStateException("Scale expression analysis limit");}
        Map<String,Double> finish(){blocked.forEach(defaults::remove);return Map.copyOf(defaults);}
        void keyframes(Object o,Set<Object> seen,int depth) {
            if(o==null||!seen.add(o))return;budget(depth);
            if(valueType.isInstance(o)){expression(o);return;}
            if(o instanceof Iterable<?> list){for(Object item:list)keyframes(item,seen,depth+1);return;}
            if(o instanceof Object[] array){for(Object item:array)keyframes(item,seen,depth+1);return;}
            if(o.getClass().isEnum()||!o.getClass().getName().startsWith(YsmProfile.ROOT))return;
            for(Field f:fields(o))if(!f.getType().isPrimitive())keyframes(read(f,o),seen,depth+1);
        }
        void expression(Object o) {
            if(!kind(o,layout.parsed)) {
                // Constant scale values carry no variable; other value wrappers are unknown.
                String constant = switch(p) {
                    case NEO -> "o000OOo0ooo00o0OOOOOOO0O";
                    case FORGE -> "oOoOoOOooOOO0OO0oooO00oO";
                    case FABRIC_1201 -> "O0oOOOo0oo0o00OOOO0oO0OO";
                    case FABRIC_1211 -> "ooO0Oo00o0O0oOoO0OOo00O0";
                };
                if(!o.getClass().getSimpleName().equals(constant))
                    throw new IllegalStateException("Unsupported scale value wrapper");
                return;
            }
            List<?> nodes=(List<?>)first(o);
            for(Object node:nodes)visit(node,nodes.size()==1,0);
        }
        void visit(Object node,boolean safe,int depth) {
            if(node==null)return;budget(depth);
            String variable=variable(node);
            if(variable!=null){blocked.add(variable);return;}
            if(safe&&kind(node,layout.ternary)) {
                Object cond=first(node);
                if(kind(cond,layout.binary)) {
                    int op=operator(cond);
                    Object left=second(cond),right=third(cond);
                    String v=variable(left);Double zero=number(right);
                    if(v==null){v=variable(right);zero=number(left);}
                    if(v!=null&&zero!=null&&zero==0&&(op==14||op==15)) {
                        Object constant=op==14?second(node):third(node),identity=op==14?third(node):second(node);
                        Double initial=number(constant);
                        if(v.equals(variable(identity))&&initial!=null&&Double.isFinite(initial)) {
                            Double old=defaults.putIfAbsent(v,initial);
                            if(old!=null&&Double.compare(old,initial)!=0)blocked.add(v);
                            return;
                        }
                    }
                }
            }
            if(kind(node,layout.binary)) {
                int op=operator(node);boolean arithmetic=safe&&op>=6&&op<=9;
                visit(second(node),arithmetic,depth+1);visit(third(node),arithmetic,depth+1);return;
            }
            // Never infer from conditionally executed statements, function calls or assignments.
            for(Field f:fields(node))if(!f.getType().isPrimitive())astChildren(read(f,node),depth+1);
        }
        void astChildren(Object o,int depth) {
            if(o==null)return;budget(depth);
            if(astType.isInstance(o))visit(o,false,depth);
            else if(o instanceof Iterable<?> items)for(Object item:items)astChildren(item,depth+1);
            else if(o instanceof Object[] items)for(Object item:items)astChildren(item,depth+1);
            else if(o.getClass().getName().startsWith(YsmProfile.ROOT)) {
                // Function arguments have a small wrapper around List<Expression>.
                // Do not traverse the function target's runtime/global object graph.
                for(Field f:fields(o))if(f.getGenericType().getTypeName().contains(YsmProfile.ROOT+layout.ast))
                    astChildren(read(f,o),depth+1);
            }
        }
        int operator(Object node){return ((Number)Reflect.call(first(node),p.b)).intValue();}
        Double number(Object node){return kind(node,layout.number)?((Number)Reflect.call(node,p.a)).doubleValue():null;}
        String variable(Object node) {
            if(!kind(node,layout.access))return null;
            Object root=first(node);
            if(!kind(root,layout.variable))return null;
            Object scoped=first(root);
            if(!kind(scoped,layout.scoped+"$"+p.a))return null;
            Object pool=Reflect.type(YsmProfile.ROOT+layout.pool);
            String parent=(String)Reflect.call(pool,p.a,Reflect.integer(scoped,p.a));
            if(!parent.equalsIgnoreCase("roaming"))return null;
            String name=(String)Reflect.call(pool,p.a,Reflect.integer(node,p.b));
            return canonical("v.roaming."+name);
        }
    }
    private static List<Field> fields(Object object) {
        List<Field> out=new ArrayList<>();
        for(Class<?> c=object.getClass();c!=null&&c.getName().startsWith(YsmProfile.ROOT);c=c.getSuperclass())
            for(Field f:c.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);out.add(f);}
        return out;
    }
    private static Object read(Field f,Object o){try{return f.get(o);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
}
