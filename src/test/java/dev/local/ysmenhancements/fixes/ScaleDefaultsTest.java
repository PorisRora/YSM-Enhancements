package dev.local.ysmenhancements.fixes;

import com.google.gson.*;
import dev.local.ysmenhancements.RegressionTest;
import dev.local.ysmenhancements.compat.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.ZipFile;

public final class ScaleDefaultsTest {
    private final YsmProfile p;private final Object parser;
    ScaleDefaultsTest(YsmProfile profile)throws Exception {
        p=profile;var layout=ScaleDefaults.Layout.of(p);
        String binding=switch(p) {
            case NEO -> "O0OoOO0ooo0oOooo0oooO0o0";
            case FORGE -> "OooOoO0O0O0o00Oo000o00Oo";
            case FABRIC_1201 -> "O0oO0OOOoOoo000oo0000OOo";
            case FABRIC_1211 -> "O00o0O0O0oOOooooo0OOoOo0";
        };
        String engine=switch(p) {
            case NEO -> "oO0o0O0oo00O00Oo0O00Oooo";
            case FORGE -> "o00oO0OOoOOooOoOOoO0Oooo";
            case FABRIC_1201 -> "oOooO0ooO0000OO0OoOO0O00";
            case FABRIC_1211 -> "ooO0o0ooO0OooO0OOOo00o0O";
        };
        Object scope=Reflect.type(YsmProfile.ROOT+layout.scoped()).getConstructor().newInstance();
        Class<?> bindingType=Reflect.type(YsmProfile.ROOT+binding);
        Object resolver=Proxy.newProxyInstance(bindingType.getClassLoader(),new Class<?>[]{bindingType},(obj,method,args)->{
            if(method.getName().equals("getProperty")&&Set.of("v","variable").contains(args[0]))return scope;
            return null;
        });
        parser=Reflect.call(Reflect.type(YsmProfile.ROOT+engine),p.a,resolver);
    }
    private Object parse(String source)throws Exception {
        Object ast=Reflect.call(parser,p.a,source);
        return Reflect.type(YsmProfile.ROOT+ScaleDefaults.Layout.of(p).parsed()).getConstructor(List.class,boolean.class).newInstance(ast,false);
    }
    private Map<String,Double> analyze(String... sources)throws Exception {
        List<Object> values=new ArrayList<>();for(String s:sources)values.add(parse(s));
        return ScaleDefaults.analyzeExpressions(values,p);
    }
    public static final class Slider {
        public double value;int writes,updates;
        public double snapToNearest(double fraction){return Math.round((.2+fraction*2.8)/.05)*.05/2.8-.2/2.8;}
        public void updateMessage(){updates++;}public void applyValue(){writes++;}
    }
    public static void main(String[] args)throws Exception {
        var test=new ScaleDefaultsTest(YsmProfile.valueOf(args[0].toUpperCase(Locale.ROOT)));
        String v="v.roaming.size";
        RegressionTest.check(test.analyze(v+"==0?1:"+v).get(v)==1,"actual AST recognizes zero fallback");
        RegressionTest.check(test.analyze("variable.roaming.Size != 0 ? variable.roaming.Size : 2").get(v)==2,"alias, case and reversed branch preserve author constant");
        RegressionTest.check(test.analyze("0 == "+v+" ? 1 : "+v).get(v)==1,"reversed equality");
        RegressionTest.check(test.analyze("("+v+"==0?1:"+v+")+(v.roaming.offset==0?0:v.roaming.offset)").get(v)==1,"model's additive axis offsets remain independent");
        RegressionTest.check(!test.analyze(v+"==0?1:"+v,v+"==0?2:"+v).containsKey(v),"conflicting defaults skipped");
        RegressionTest.check(!test.analyze(v+"==0?1:"+v,v).containsKey(v),"unprotected scale usage skipped");
        RegressionTest.check(test.analyze("v.roaming.flag ? ("+v+"==0?1:"+v+") : 2").isEmpty(),"conditional action does not declare a universal default");
        RegressionTest.check(test.analyze(v+"==0?1:2").isEmpty(),"boolean visibility is not a scale default");
        RegressionTest.check(test.analyze(v+"=1;"+v).isEmpty(),"assignment is inspected without executing it");
        RegressionTest.check(ScaleDefaults.zero("0")&&ScaleDefaults.zero("null")&&!ScaleDefaults.zero("0.25")&&!ScaleDefaults.zero("broken"),"only initial zero values eligible");
        RegressionTest.check(!ScaleDefaults.outsideZero(0,3)&&ScaleDefaults.outsideZero(.2,3),"intentional selectable zero left untouched");
        Slider slider=new Slider();RegressionTest.check(ScaleDefaults.display(slider,1,.2,3)&&slider.writes==0&&slider.updates==1,"view initialization never applies or synchronizes a model value");
        RegressionTest.check(Math.abs((.2+slider.value*2.8)-1)<1e-6,"initial handle corresponds to one instead of the minimum");
        RegressionTest.check(!ScaleDefaults.display(slider,4,.2,3)&&!ScaleDefaults.display(slider,1.012,.2,3),"out-of-range or unrepresentable defaults skipped");
        if(args.length>1)try(ZipFile zip=new ZipFile(new java.io.File(args[1]),java.nio.charset.Charset.forName("IBM437"))) {
            List<Object> expressions=new ArrayList<>();
            for(var entries=zip.entries();entries.hasMoreElements();) {
                var entry=entries.nextElement();if(!entry.getName().endsWith(".animation.json"))continue;
                JsonObject document;
                try(var reader=new java.io.InputStreamReader(zip.getInputStream(entry),java.nio.charset.StandardCharsets.UTF_8)){document=JsonParser.parseReader(reader).getAsJsonObject();}
                for(JsonElement anim:document.getAsJsonObject("animations").asMap().values()) {
                    JsonObject bones=anim.getAsJsonObject().getAsJsonObject("bones");if(bones==null)continue;
                    for(JsonElement bone:bones.asMap().values())test.collect(bone.getAsJsonObject().get("scale"),expressions);
                }
            }
            Map<String,Double> found=ScaleDefaults.analyzeExpressions(expressions,test.p);
            for(String key:List.of("v.roaming.player_size","v.roaming.head","v.roaming.tair_size"))
                RegressionTest.check(found.get(key)!=null&&found.get(key)==1,"real Rooboko scale default: "+key);
            RegressionTest.check(!found.containsKey("v.roaming.hat")&&!found.containsKey("v.roaming.elytra"),"visibility expressions not reinterpreted");
            System.out.println("PASS: "+expressions.size()+" real model scale expressions; player_size/head/tair_size default to 1; no visibility override");
        }
        System.out.println("PASS: "+test.p+" actual YSM AST scale defaults, conservative exclusions and non-mutating display initialization");
    }
    private void collect(JsonElement value,List<Object> out)throws Exception {
        if(value==null)return;
        if(value.isJsonArray()){for(JsonElement item:value.getAsJsonArray())collect(item,out);}
        else if(value.isJsonObject()){for(var entry:value.getAsJsonObject().entrySet())if(!entry.getKey().equals("lerp_mode"))collect(entry.getValue(),out);}
        else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()&&value.getAsString().toLowerCase(Locale.ROOT).contains(".roaming."))out.add(parse(value.getAsString()));
    }
}
