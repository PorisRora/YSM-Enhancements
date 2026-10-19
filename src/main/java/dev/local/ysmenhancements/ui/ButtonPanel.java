package dev.local.ysmenhancements.ui;

import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.config.Feature;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** Draws a new view of the live YSM menu. Author widgets retain all variable/action callbacks. */
public final class ButtonPanel {
    public static final String STATE_FIELD="ysm$buttonPanelState";
    private static final int INK=0xFFEAEAEA, MUTED=0xFFAEAEAE, ACCENT=0xFFCCCCCC;
    public record Rect(int x,int y,int w,int h) {
        boolean contains(double px,double py){return px>=x&&py>=y&&px<x+w&&py<y+h;}
    }
    private record Original(int x,int y,int w,int h) {}
    private record Hit(Rect rect,Runnable click,Object widget,boolean slider) {}
    private static final class State {
        final Map<Object,Original> originals=new IdentityHashMap<>();
        final List<Hit> hits=new ArrayList<>();
        final ControlView controls=new ControlView();
        Rect outer,preview,viewport; int page,scroll,maxScroll,focus=-1;
        double zoom=1,turn,pitch; boolean rotating,failed,keyboardFocus; Hit dragging;
        Object selected,editing; String edit="";
    }
    private static State state(Object screen) {
        Object value=Reflect.get(screen,STATE_FIELD);
        if(value==null){value=new State();Reflect.set(screen,STATE_FIELD,value);}return (State)value;
    }
    public static boolean active(Object screen){return Feature.BUTTON_PANEL.enabled()&&!state(screen).failed;}
    public static void rebuilt(Object screen){State s=state(screen);s.controls.begin(System.nanoTime());s.hits.clear();s.dragging=null;s.rotating=false;s.editing=null;s.focus=-1;s.keyboardFocus=false;}
    private static void fail(State state,Throwable error){
        state.failed=true;
        System.getLogger("ysm_enhancements").log(System.Logger.Level.ERROR,"[YSM patch] Button panel unavailable; restored the original wheel for this screen",error);
    }
    public static boolean render(Object screen,Object graphics,int mouseX,int mouseY,float partialTick) {
        if(!active(screen))return false;
        State s=state(screen);
        try {draw(screen,graphics,mouseX,mouseY,s);return true;}
        catch(RuntimeException|LinkageError error){fail(s,error);return false;}
    }
    private static void draw(Object screen,Object graphics,int mx,int my,State s) {
        YsmProfile p=YsmProfile.of(screen);Draw d=new Draw(graphics);
        int sw=Reflect.integer(screen,"width|f_96543_"),sh=Reflect.integer(screen,"height|f_96544_");
        int w=Math.max(230,sw-28),h=Math.max(160,sh-24),x=(sw-w)/2,y=(sh-h)/2;
        s.outer=new Rect(x,y,w,h);int split=x+(int)(w*.48);
        s.preview=new Rect(x+6,y+34,split-x-12,h-70);
        s.viewport=new Rect(split+12,y+64,x+w-split-26,h-104);
        s.hits.clear();d.round(s.outer,0xDC202020);d.fill(x+1,y+32,x+w-1,y+33,0x409E9E9E);
        d.label("模型配置",new Rect(x+15,y+7,w-60,20),INK);
        button(d,s,new Rect(x+w-30,y+7,22,20),"×",mx,my,()->Reflect.call(screen,"onClose|m_7379_"));
        d.fill(split,y+43,split+1,y+h-12,0x30808080);
        preview(screen,graphics,p,s,mx,my);
        int by=y+h-29,cx=x+(split-x)/2;
        button(d,s,new Rect(cx-55,by,24,19),"−",mx,my,()->s.zoom=clamp(s.zoom/1.18,.12,5));
        button(d,s,new Rect(cx-25,by,50,19),Math.round(s.zoom*100)+"%",mx,my,()->{s.zoom=1;s.turn=0;s.pitch=0;});
        button(d,s,new Rect(cx+31,by,24,19),"+",mx,my,()->s.zoom=clamp(s.zoom*1.18,.12,5));
        int toolbarY=y+41;
        Object entity=Reflect.call(Reflect.get(screen,p.animatable),p.entityMethod);
        if(Reflect.type("net.minecraft.world.entity.player.Player").isInstance(entity)){
            Object lock=Reflect.type(YsmProfile.ROOT+p.lock);boolean locked=(Boolean)Reflect.call(lock,p.b);
            button(d,s,new Rect(x+12,toolbarY,69,18),locked?"动作锁定：开":"动作锁定：关",mx,my,()->Reflect.call(lock,p.a));
        }else{
            for(Object widget:(List<?>)Reflect.call(screen,"children|m_6702_")){
                if(((Number)Reflect.call(widget,"getX|m_252754_")).intValue()==Reflect.integer(screen,p.x)-20){
                    button(d,s,new Rect(x+12,toolbarY,69,18),"停止动作",mx,my,()->Reflect.call(widget,"onPress|m_5691_"));break;
                }
            }
        }
        Object selected=Reflect.get(screen,p.selected);
        if(selected!=s.selected){s.selected=selected;s.scroll=0;s.editing=null;s.focus=-1;s.controls.clear();s.originals.clear();}
        button(d,s,new Rect(split+12,toolbarY,33,18),"返回",mx,my,()->back(screen,p,s));
        String title=selected==null?pathTitle(screen,p):String.valueOf(Reflect.call(selected,p.b));
        d.label(title,new Rect(split+53,toolbarY,x+w-split-68,18),INK);
        if(selected==null)menu(screen,p,d,s,mx,my);else controls(screen,p,d,s,mx,my);
        if(s.keyboardFocus&&s.focus>=0&&s.focus<s.hits.size())d.outline(s.hits.get(s.focus).rect,ACCENT);
    }
    private static String pathTitle(Object screen,YsmProfile p) {
        String path=String.valueOf(Reflect.call(Reflect.get(screen,p.path),"getLeft"));
        return path.isBlank()?"根目录":localize(screen,p,"properties.extra_animation.#"+path,path);
    }
    private static String localize(Object screen,YsmProfile p,String key,String fallback) {
        String resolver=p==YsmProfile.NEO?"OoO0o00Oo0O00oOoO0OOO0oo":"OOOOooO0O0o0OOoOOo00oo00";
        Object data=Reflect.get(screen,"ooOoO0OOO0oOo0Oo00oo000o|o0OooO00ooo0OO000O0OoOoO");
        return (String)Reflect.call(Reflect.type(YsmProfile.ROOT+resolver),p.a,data,key,fallback);
    }
    private static void menu(Object screen,YsmProfile p,Draw d,State s,int mx,int my) {
        Object menu=Reflect.get(screen,p.menu);Map<?,?> configs=(Map<?,?>)Reflect.get(screen,p.configs);
        int total=((Number)Reflect.call(menu,"size")).intValue();
        Rect v=s.viewport;int cols=v.w>=180?2:1,rows=Math.max(1,Math.min(4,(v.h-26)/47));
        int per=cols*rows,pages=Math.max(1,(total+per-1)/per);s.page=Math.max(0,Math.min(s.page,pages-1));
        int cw=(v.w-8*(cols-1))/cols,ch=Math.max(34,Math.min(46,(v.h-26)/rows-6));
        for(int j=0;j<per&&s.page*per+j<total;j++){
            int index=s.page*per+j;String key=(String)Reflect.call(menu,p.a,index),value=(String)Reflect.call(menu,p.b,index);
            String configId=value.startsWith("#")?value.substring(1):null;
            Object config=configId==null?null:configs.get(configId);
            String name=config!=null?String.valueOf(Reflect.call(config,p.b)):value;
            if(name.isBlank())name=key;
            name=localize(screen,p,"properties.extra_animation."+key,name);
            Rect r=new Rect(v.x+(j%cols)*(cw+8),v.y+(j/cols)*(ch+7),cw,ch);
            boolean hover=r.contains(mx,my);d.round(r,hover?0xCC4C4C4C:0x993B3B3B);
            boolean nested=config!=null||key.startsWith("#");
            if(nested)d.round(new Rect(r.x+2,r.y+7,2,r.h-14),0xAACCCCCC);
            if(config!=null){
                Rect main=new Rect(r.x,r.y,r.w-27,r.h);d.label(name,new Rect(r.x+10,r.y,r.w-55,r.h),INK);
                d.center("›",new Rect(r.x+r.w-40,r.y,10,r.h),ACCENT);
                s.hits.add(new Hit(main,()->Reflect.call(screen,p.configMethod,configId),null,false));
                button(d,s,new Rect(r.x+r.w-25,r.y+5,21,r.h-10),key.startsWith("#")?">":"▶",mx,my,()->activate(screen,p,index,key));
            }else{
                d.label(name,new Rect(r.x+10,r.y,r.w-(nested?34:24),r.h),INK);
                if(nested)d.center(key.equals("#return")?"‹":"›",new Rect(r.x+r.w-18,r.y,10,r.h),ACCENT);
                s.hits.add(new Hit(r,()->activate(screen,p,index,key),null,false));
            }
        }
        int by=v.y+v.h-20;
        if(s.page>0)button(d,s,new Rect(v.x,by,30,18),"<",mx,my,()->s.page--);
        d.center((s.page+1)+" / "+pages,new Rect(v.x+32,by,v.w-64,18),MUTED);
        if(s.page+1<pages)button(d,s,new Rect(v.x+v.w-30,by,30,18),">",mx,my,()->s.page++);
    }
    private static void activate(Object screen,YsmProfile p,int index,String key){
        Reflect.set(screen,p.selectedIndex,index);
        if(key.equals("#return"))Reflect.call(screen,p.configMethod);
        else if(key.startsWith("#"))Reflect.call(screen,p.folderMethod,key);
        else Reflect.call(screen,p.playMethod,key);
    }
    private static void back(Object screen,YsmProfile p,State s){
        if(Reflect.get(screen,p.selected)!=null){Reflect.set(screen,p.selected,null);Reflect.set(screen,p.scroll,0);Reflect.set(screen,p.max,0);s.scroll=0;Reflect.call(screen,"init|m_7856_");}
        else Reflect.call(screen,p.configMethod);
    }
    private static void controls(Object screen,YsmProfile p,Draw d,State s,int mx,int my){
        Class<?> marker=Reflect.type(YsmProfile.ROOT+p.marker);
        Class<?> slider=Reflect.type(YsmProfile.ROOT+(p==YsmProfile.NEO?"o0OO0ooOOo0Ooo00OO0Oo0O0":"oo0ooOoOOOO0OOoO0OOoOOo0"));
        Class<?> checkbox=Reflect.type(YsmProfile.ROOT+(p==YsmProfile.NEO?"O0oo0oOOOoooO0o0oooOo000":"oOO0oo0OoOo0O0OOo0oooO0o"));
        List<Object> current=new ArrayList<>();
        for(Object widget:(List<?>)Reflect.get(screen,"renderables|f_169369_"))if(marker.isInstance(widget)&&(Boolean)Reflect.get(widget,"visible|f_93624_"))current.add(widget);
        List<Object> displayed=s.controls.update(current,System.nanoTime());
        s.originals.keySet().retainAll(displayed);
        TreeMap<Integer,List<Object>> rows=new TreeMap<>();
        for(Object widget:displayed){
            Original original=s.originals.computeIfAbsent(widget,key->new Original(
                    ((Number)Reflect.call(key,"getX|m_252754_")).intValue(),((Number)Reflect.call(key,"getY|m_252907_")).intValue(),
                    ((Number)Reflect.call(key,"getWidth|m_5711_")).intValue(),((Number)Reflect.call(key,"getHeight|m_93694_")).intValue()));
            rows.computeIfAbsent(original.y,k->new ArrayList<>()).add(widget);
        }
        int contentHeight=0;for(List<Object> row:rows.values())contentHeight+=rowHeight(row.get(0),slider,checkbox)+6;
        Rect v=s.viewport;s.maxScroll=Math.max(0,contentHeight-v.h);int shownScroll=scrollOffset(s);
        d.clip(v);
        try {
            int y=v.y-shownScroll;
            for(List<Object> row:rows.values()){
                row.sort(Comparator.comparingInt(w->s.originals.get(w).x));int height=rowHeight(row.get(0),slider,checkbox);
                int cell=(v.w-6*(row.size()-1)-5)/row.size();
                for(int i=0;i<row.size();i++){
                    Object widget=row.get(i);Rect r=new Rect(v.x+i*(cell+6),y,cell,height);
                    if(r.y+r.h>v.y&&r.y<v.y+v.h){
                        if(slider.isInstance(widget))drawSlider(widget,r,d,s,mx,my);
                        else if(checkbox.isInstance(widget))drawCheckbox(widget,p,r,d,s,mx,my,widget.getClass()==checkbox);
                        else d.label(Reflect.text(Reflect.call(widget,"getMessage|m_6035_")),new Rect(r.x+3,r.y,r.w-6,r.h),MUTED);
                    }
                }
                y+=height+6;
            }
        } finally {d.unclip();}
        if(s.maxScroll>0){int thumb=Math.max(12,v.h*v.h/Math.max(v.h,contentHeight));int top=v.y+(int)((v.h-thumb)*(shownScroll/(double)s.maxScroll));d.round(new Rect(v.x+v.w-2,top,2,thumb),0x88BDBDBD);}
    }
    private static int rowHeight(Object widget,Class<?> slider,Class<?> checkbox){return slider.isInstance(widget)?38:checkbox.isInstance(widget)?25:19;}
    private static void drawSlider(Object widget,Rect r,Draw d,State s,int mx,int my){
        double min=((Number)Reflect.get(widget,"minValue")).doubleValue(),max=((Number)Reflect.get(widget,"maxValue")).doubleValue(),value=((Number)Reflect.call(widget,"getValue")).doubleValue();
        d.round(r,0x77363636);String label=Reflect.text(Reflect.get(widget,"prefix"));
        d.label(label,new Rect(r.x+8,r.y+3,Math.max(15,r.w-67),17),INK);
        String text=s.editing==widget?s.edit+"_":(String)Reflect.call(widget,"getValueString");
        Rect number=new Rect(r.x+r.w-53,r.y+3,48,17);d.round(number,s.editing==widget?0xDD5C5C5C:0x99303030);d.center(text,number,ACCENT);
        Rect bar=new Rect(r.x+9,r.y+26,r.w-18,4);d.round(bar,0xFF555555);
        int fill=(int)(bar.w*clamp(max==min?0:(value-min)/(max-min),0,1));if(fill>0)d.round(new Rect(bar.x,bar.y,fill,4),ACCENT);
        d.round(new Rect(bar.x+fill-2,bar.y-2,5,8),0xFFE5E5E5);
        if(!s.controls.waiting()&&interactive(widget)&&fullyVisible(number,s.viewport))s.hits.add(new Hit(number,()->{s.editing=widget;s.edit=Double.toString(value);},widget,false));
        Rect hit=new Rect(r.x+5,r.y+20,r.w-10,16);
        if(!s.controls.waiting()&&interactive(widget)&&fullyVisible(hit,s.viewport))s.hits.add(new Hit(hit,null,widget,true));
    }
    private static void drawCheckbox(Object widget,YsmProfile p,Rect r,Draw d,State s,int mx,int my,boolean choice){
        boolean on=(Boolean)Reflect.call(widget,p==YsmProfile.NEO?p.a:"m_94620_");
        d.round(r,r.contains(mx,my)?0xAA4C4C4C:0x77363636);
        String label=checkboxLabel(widget);
        // In both supported YSM 2.6.5 builds, selection options use the concrete
        // checkbox class; independent booleans use the wheel's anonymous subclass.
        // Keep the option style stable when a group wraps or the GUI is resized.
        if(!choice){
            d.label(label,new Rect(r.x+8,r.y,r.w-65,r.h),INK);
            Rect toggle=new Rect(r.x+r.w-54,r.y+5,47,15);
            d.round(toggle,on?0xFFAAAAAA:0xFF4D4D4D);
            d.center(on?"已勾选":"未勾选",toggle,on?0xFF191919:INK);
        }else{
            d.round(new Rect(r.x+7,r.y+8,8,8),on?ACCENT:0xFF555555);
            d.label(label,new Rect(r.x+22,r.y,r.w-27,r.h),on?ACCENT:INK);
        }
        if(!s.controls.waiting()&&interactive(widget)&&fullyVisible(r,s.viewport))s.hits.add(new Hit(r,()->{
            Original o=s.originals.get(widget);Reflect.call(widget,"mouseClicked|m_6375_",o.x+o.w/2.0,o.y+o.h/2.0,0);
        },widget,false));
    }
    private static String checkboxLabel(Object widget){
        for(Class<?> c=widget.getClass();c!=null&&c.getName().startsWith(YsmProfile.ROOT);c=c.getSuperclass())for(Field f:c.getDeclaredFields()){
            if(!Modifier.isStatic(f.getModifiers())&&f.getType().getName().equals("net.minecraft.network.chat.Component"))try{f.setAccessible(true);return Reflect.text(f.get(widget));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        return Reflect.text(Reflect.call(widget,"getMessage|m_6035_"));
    }
    private static boolean interactive(Object widget){return (Boolean)Reflect.get(widget,"active|f_93623_")&&(Boolean)Reflect.get(widget,"visible|f_93624_");}
    private static boolean fullyVisible(Rect r,Rect clip){return r.y>=clip.y&&r.y+r.h<=clip.y+clip.h;}
    private static void preview(Object screen,Object graphics,YsmProfile p,State s,int mx,int my){
        Object entity=Reflect.call(Reflect.get(screen,p.animatable),p.entityMethod);
        if(!Reflect.type("net.minecraft.world.entity.LivingEntity").isInstance(entity))return;
        Rect r=s.preview;double height=((Number)Reflect.call(entity,"getBbHeight|m_20206_")).doubleValue();
        double width=((Number)Reflect.call(entity,"getBbWidth|m_20205_")).doubleValue();
        int scale=(int)(Math.min((r.h-14)/Math.max(1.8,height),(r.w-20)/Math.max(1.2,width*1.4))*.88*s.zoom);
        Reflect.call(graphics,"enableScissor|m_280588_",r.x,r.y,r.x+r.w,r.y+r.h);
        try {
            PreviewRenderer.render(graphics,entity,r,Math.max(1,scale),s.turn,s.pitch,p==YsmProfile.NEO);
        } finally {Reflect.call(graphics,"disableScissor|m_280618_");}
    }
    private static void button(Draw d,State s,Rect rect,String text,int mx,int my,Runnable action){
        d.round(rect,rect.contains(mx,my)?0xCC5C5C5C:0xAA484848);d.center(text,rect,INK);s.hits.add(new Hit(rect,action,null,false));
    }
    public static boolean clicked(Object screen,double x,double y,int button){
        State s=state(screen);s.keyboardFocus=false;if(button!=0)return false;
        s.editing=null;s.rotating=false;s.dragging=null;
        for(int i=0;i<s.hits.size();i++){
            Hit hit=s.hits.get(i);if(!hit.rect.contains(x,y))continue;s.focus=i;
            if(hit.slider){s.dragging=hit;sliderMouse(s,hit,x);}else hit.click.run();return true;
        }
        if(s.preview!=null&&s.preview.contains(x,y)){s.rotating=true;return true;}return false;
    }
    private static void sliderMouse(State s,Hit hit,double x){
        Original o=s.originals.get(hit.widget);double fraction=clamp((x-hit.rect.x-4)/Math.max(1,hit.rect.w-8),0,1);
        double px=o.x+4+fraction*Math.max(1,o.w-8);
        Reflect.call(hit.widget,"onClick|m_5716_",px,o.y+o.h/2.0);
    }
    public static boolean dragged(Object screen,double x,double y,int button,double dx,double dy){
        State s=state(screen);if(button!=0)return false;if(s.dragging!=null){sliderMouse(s,s.dragging,x);return true;}if(s.rotating){s.turn=Math.IEEEremainder(s.turn+dx*360/Math.max(160,s.preview.w),360);s.pitch=clamp(s.pitch+dy*90/Math.max(100,s.preview.h),-45,45);return true;}return false;
    }
    public static boolean released(Object screen,double x,double y,int button){State s=state(screen);if(button!=0)return false;boolean result=s.dragging!=null||s.rotating;s.dragging=null;s.rotating=false;return result;}
    public static boolean scrolled(Object screen,double x,double y,double amount){
        State s=state(screen);if(s.preview!=null&&s.preview.contains(x,y)){s.zoom=clamp(s.zoom*Math.pow(1.12,amount),.12,5);return true;}
        if(s.viewport!=null&&s.viewport.contains(x,y)){
            if(s.selected!=null)s.scroll=(int)clamp(scrollOffset(s)-amount*28,0,s.maxScroll);else s.page=Math.max(0,s.page+(amount<0?1:amount>0?-1:0));return true;
        }return false;
    }
    public static boolean key(Object screen,int key,int scan,int modifiers){
        if(!active(screen))return false;State s=state(screen);
        if(s.editing!=null){
            if(key==256){s.editing=null;return true;}
            if(key==257||key==335){try{double value=Double.parseDouble(s.edit);if(Double.isFinite(value))setSliderValue(s.editing,value,YsmProfile.of(screen)==YsmProfile.NEO);s.editing=null;}catch(NumberFormatException ignored){}return true;}
            if(key==259&&!s.edit.isEmpty())s.edit=s.edit.substring(0,s.edit.length()-1);
            if(key==65&&(modifiers&2)!=0)s.edit="";
            return true;
        }
        if(key==258&&!s.hits.isEmpty()){s.keyboardFocus=true;s.focus=s.focus<0?((modifiers&1)!=0?s.hits.size()-1:0):Math.floorMod(s.focus+((modifiers&1)!=0?-1:1),s.hits.size());return true;}
        if(s.focus>=0&&s.focus<s.hits.size()){
            Hit h=s.hits.get(s.focus);
            if((key==257||key==32)&&h.click!=null){h.click.run();return true;}
            if(h.slider&&(key==262||key==263)){keySlider(h.widget,key,scan,modifiers,YsmProfile.of(screen)==YsmProfile.NEO);return true;}
        }
        return false;
    }
    public static boolean character(Object screen,char ch,int modifiers){State s=state(screen);if(s.editing==null)return false;if("0123456789.+-eE".indexOf(ch)>=0&&s.edit.length()<32)s.edit+=ch;return true;}
    public static void setSliderValue(Object widget,double value,boolean neo){
        double old=((Number)Reflect.call(widget,"getValue")).doubleValue();
        double min=((Number)Reflect.get(widget,"minValue")).doubleValue(),max=((Number)Reflect.get(widget,"maxValue")).doubleValue();
        if(min==max)return;
        Reflect.call(widget,"setValue|m_93611_",value);
        if(!neo&&Double.compare(old,((Number)Reflect.call(widget,"getValue")).doubleValue())!=0)Reflect.call(widget,"m_5697_");
    }
    public static void keySlider(Object widget,int key,int scan,int modifiers,boolean neo){
        double old=((Number)Reflect.call(widget,"getValue")).doubleValue();
        Reflect.call(widget,"keyPressed|m_7933_",key,scan,modifiers);
        // Forge's stepped-key branch calls its display-only setter; its continuous branch applies already.
        if(!neo&&((Number)Reflect.get(widget,"stepSize")).doubleValue()>0&&Double.compare(old,((Number)Reflect.call(widget,"getValue")).doubleValue())!=0)Reflect.call(widget,"m_5697_");
    }
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static int scrollOffset(State s){return Math.max(0,Math.min(s.scroll,s.maxScroll));}
    private static final class Draw {
        final Object g,font;
        Draw(Object graphics){g=graphics;font=Reflect.get(Reflect.minecraft(),"font|f_91062_");}
        void fill(int x,int y,int right,int bottom,int color){if(right>x&&bottom>y)Reflect.call(g,"fill|m_280509_",x,y,right,bottom,color);}
        void round(Rect r,int color){int n=Math.min(3,Math.min(r.w,r.h)/2);fill(r.x,r.y+n,r.x+r.w,r.y+r.h-n,color);fill(r.x+n,r.y,r.x+r.w-n,r.y+n,color);fill(r.x+n,r.y+r.h-n,r.x+r.w-n,r.y+r.h,color);if(n>1){fill(r.x+1,r.y+1,r.x+n,r.y+n,color);fill(r.x+r.w-n,r.y+1,r.x+r.w-1,r.y+n,color);fill(r.x+1,r.y+r.h-n,r.x+n,r.y+r.h-1,color);fill(r.x+r.w-n,r.y+r.h-n,r.x+r.w-1,r.y+r.h-1,color);}}
        int width(String text){return ((Number)Reflect.call(font,"width|m_92895_",text)).intValue();}
        String fit(String text,int available){
            if(available<=0)return "";if(width(text)<=available)return text;
            if(width("…")>available)return "";int end=text.length();
            while(end>0&&width(text.substring(0,end)+"…")>available)end=text.offsetByCodePoints(end,-1);
            return text.substring(0,end)+"…";
        }
        void label(String text,Rect rect,int color){aligned(text,rect,color,false);}
        void center(String text,Rect rect,int color){aligned(text,rect,color,true);}
        void aligned(String text,Rect rect,int color,boolean centered){
            String shown=fit(text,rect.w);if(shown.isEmpty())return;
            int height=Reflect.integer(font,"lineHeight|f_92710_");
            // Match AbstractWidget's vertical alignment, including its baseline offset.
            int y=rect.y+(rect.h-height)/2+1;
            int x=centered?rect.x+(rect.w-width(shown))/2:rect.x;
            Reflect.call(g,"drawString|m_280056_",font,shown,x,y,color,false);
        }
        void outline(Rect r,int color){fill(r.x,r.y,r.x+r.w,r.y+1,color);fill(r.x,r.y+r.h-1,r.x+r.w,r.y+r.h,color);fill(r.x,r.y,r.x+1,r.y+r.h,color);fill(r.x+r.w-1,r.y,r.x+r.w,r.y+r.h,color);}
        void clip(Rect r){Reflect.call(g,"enableScissor|m_280588_",r.x,r.y,r.x+r.w,r.y+r.h);}
        void unclip(){Reflect.call(g,"disableScissor|m_280618_");}
    }
}
