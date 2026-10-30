package dev.local.ysmenhancements.ui;

import dev.local.ysmenhancements.compat.*;
import dev.local.ysmenhancements.config.Feature;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/** Draws a new view of the live YSM menu. Author widgets retain all variable/action callbacks. */
public final class ButtonPanel {
    public static final String STATE_FIELD="ysm$buttonPanelState";
    // Neutral dark surfaces and a single accent for state; translucency keeps the world faintly visible.
    private static final int TEXT=0xFFF1F3F5, SUB=0xFFA2A9B3, FAINT=0xFF5E656E, ACCENT=0xFF62A8EA, ON_ACCENT=0xFF0B1621, DANGER=0xFFD9534F,
            PANEL=0xEB15171A, LINE=0x1AFFFFFF, SURFACE=0xC2262A30, RAISED=0xE0303640, SELECTED=0xD0324355, WELL=0x12FFFFFF, TRACK=0xFF3A3F47, GAP=6;
    private static final double MIN_ZOOM=.12, MAX_ZOOM=5;
    // Exact first entry of the shown page per YSM menu. YSM stores only pages of eight, and it replaces
    // the root's trail entry each time the root opens, so the menu object is the stable key.
    private static final Map<Object,Integer> FIRST_ENTRY=new IdentityHashMap<>();
    private static final Map<Class<?>,Field> LABELS=new HashMap<>();
    private static Class<?> folderTrailOwner;private static Field folderTrail;private static boolean silent;
    public record Rect(int x,int y,int w,int h) {
        boolean contains(double px,double py){return px>=x&&py>=y&&px<x+w&&py<y+h;}
    }
    private record Original(int x,int y,int w,int h) {}
    private record Entry(String key,String configId,String name) {}
    private record Fitted(String text,int width) {}
    /** Ids keep focus across rebuilt hit lists; pointer=false marks clipped targets that only the keyboard reaches. */
    private record Hit(Object id,Rect rect,Rect area,Runnable click,Object widget,boolean slider,boolean pointer) {}
    private static final class State {
        final Map<Object,Original> originals=new IdentityHashMap<>();
        final List<Hit> hits=new ArrayList<>();
        final ControlView controls=new ControlView();
        final Map<String,Fitted> fitted=new HashMap<>();
        final Map<Object,Glow> glow=new HashMap<>();
        final List<Entry> entries=new ArrayList<>();
        Rect preview,viewport,track; int page=-1,pages=1,per=1,scroll,maxScroll,thumb,focus=-1,contentStart;
        double zoom=1,turn,pitch,wheel; boolean rotating,failed,keyboardFocus,scrolling,selectAll; Hit dragging;
        Object selected,editing,focusId,menu,crumbFor=this; String edit="",crumb; long last,frame; float dt;
    }
    private static final class Glow { float t; long frame; }
    private static State state(Object screen) {
        Object value=Reflect.get(screen,STATE_FIELD);
        if(value==null){value=new State();Reflect.set(screen,STATE_FIELD,value);}return (State)value;
    }
    public static boolean active(Object screen){return Feature.BUTTON_PANEL.enabled()&&!state(screen).failed;}
    // Focus ids are positional, so keyboard users keep their place while YSM rebuilds the same form.
    public static void rebuilt(Object screen){State s=state(screen);s.controls.begin(System.nanoTime());s.hits.clear();s.focus=-1;s.dragging=null;s.rotating=false;s.scrolling=false;s.editing=null;}
    private static void fail(State state,Throwable error){
        state.failed=true;state.hits.clear();state.dragging=null;state.rotating=false;state.scrolling=false;state.editing=null;
        System.getLogger("ysm_enhancements").log(System.Logger.Level.ERROR,"[YSM patch] Button panel unavailable; restored the original wheel for this screen",error);
    }
    public static boolean render(Object screen,Object graphics,int mouseX,int mouseY,float partialTick) {
        if(!active(screen))return false;
        State s=state(screen);
        try {draw(screen,graphics,mouseX,mouseY,s);return true;}
        catch(RuntimeException|LinkageError error){fail(s,error);return false;}
    }
    private static void draw(Object screen,Object graphics,int mx,int my,State s) {
        YsmProfile p=YsmProfile.of(screen);Draw d=new Draw(graphics,s);
        long now=System.nanoTime();s.dt=s.last==0?0:Math.min(.1f,(now-s.last)/1e9f);s.last=now;s.frame++;
        int sw=Reflect.integer(screen,"width|f_96543_"),sh=Reflect.integer(screen,"height|f_96544_");
        // A dialog rather than a full-screen sheet on small GUI scales.
        int w=Math.min(820,Math.max(230,sw-28)),h=Math.min(480,Math.max(160,sh-24)),x=(sw-w)/2,y=(sh-h)/2;
        Rect outer=new Rect(x,y,w,h);int split=x+(int)(w*.48);
        // Three aligned rows: toolbar, content (preview well | list) and bottom controls (zoom | pager).
        s.preview=new Rect(x+10,y+64,split-x-20,h-99);
        s.viewport=new Rect(split+12,y+64,x+w-split-26,h-74);
        s.hits.clear();s.track=null;
        for(int i=3;i>0;i--)d.shape(new Rect(x-i,y-i+1,w+2*i,h+2*i),6+i,(4-i)*0x0E000000);
        d.shape(outer,6,PANEL);d.ring(outer,6,LINE);d.fill(x+1,y+32,x+w-1,y+33,LINE);
        d.shape(new Rect(x+13,y+12,3,10),1,ACCENT);d.label("模型配置",new Rect(x+21,y+7,w-60,20),TEXT);
        ghost(d,s,"close",new Rect(x+w-29,y+7,20,19),"×",mx,my,()->Reflect.call(screen,"onClose|m_7379_"),0xE0B8473F);
        Object entity=Reflect.call(Reflect.get(screen,p.animatable),p.entityMethod);int scale=previewScale(entity,s);
        stage(d,s.preview,scale>0?PreviewRenderer.feet(entity,s.preview,scale,p==YsmProfile.NEO):Integer.MIN_VALUE,scale);
        if(scale>0)preview(graphics,entity,p,s,scale);
        int cx=s.preview.x+s.preview.w/2;
        zoom(d,s,new Rect(cx-50,y+h-29,100,19),mx,my);
        int toolbarY=y+41;
        if(Reflect.type("net.minecraft.world.entity.player.Player").isInstance(entity)){
            Object lock=Reflect.type(YsmProfile.ROOT+p.lock);boolean locked=(Boolean)Reflect.call(lock,p.b);
            Rect chip=new Rect(x+10,toolbarY,80,18);float over=ease(s,"lock",chip.contains(mx,my)),on=ease(s,"lock:on",locked);
            d.shape(chip,5,mix(SURFACE,RAISED,over));d.label("动作锁定",new Rect(chip.x+9,chip.y,40,chip.h),mix(SUB,TEXT,Math.max(over,on)));
            drawSwitch(d,new Rect(chip.x+chip.w-29,chip.y+4,22,10),on);
            s.hits.add(new Hit("lock",chip,chip,()->Reflect.call(lock,p.a),null,false,true));
        }else{
            for(Object widget:(List<?>)Reflect.call(screen,"children|m_6702_")){
                if(((Number)Reflect.call(widget,"getX|m_252754_")).intValue()==Reflect.integer(screen,p.x)-20){
                    button(d,s,"stop",new Rect(x+10,toolbarY,80,18),"停止动作",mx,my,()->Reflect.call(widget,"onPress|m_5691_"));break;
                }
            }
        }
        Object selected=Reflect.get(screen,p.selected);
        if(selected!=s.selected){s.selected=selected;s.scroll=0;s.editing=null;s.controls.clear();s.originals.clear();}
        List<?> folders=folders(screen);
        // At the root YSM's return closes the whole screen, which × already does.
        boolean root=selected==null&&(folders!=null?folders.size()<=1:String.valueOf(Reflect.call(Reflect.get(screen,p.path),"getLeft")).isBlank());
        ghost(d,s,"back",new Rect(split+12,toolbarY,40,18),"‹ 返回",mx,my,root?null:()->back(screen,p,s),RAISED);
        d.trail(breadcrumb(screen,p,s,folders,selected),new Rect(split+60,toolbarY,x+w-split-74,18));
        s.contentStart=s.hits.size();
        if(selected==null)menu(screen,p,d,s,mx,my);else controls(screen,p,d,s,mx,my);
        resolveFocus(s);
        if(s.keyboardFocus&&s.focus>=0&&s.hits.get(s.focus).pointer){Rect f=s.hits.get(s.focus).rect;d.ring(new Rect(f.x-1,f.y-1,f.w+2,f.h+2),5,ACCENT);}
        if(s.preview.contains(mx,my)&&!s.rotating&&s.dragging==null&&hitAt(s,mx,my)==null)hint(d,s,cx,s.preview.y+s.preview.h-21);
    }
    /** A lifted well, so dark models keep their outline, with a contact shadow under the feet while they are in view. */
    private static void stage(Draw d,Rect r,int feet,int scale){
        d.shape(r,6,WELL);d.fill(r.x+4,r.y,r.x+r.w-4,r.y+1,0x0AFFFFFF);
        if(feet>r.y+4&&feet<r.y+r.h-3)for(int i=0;i<3;i++){
            int w=scale*(95-29*i)/100,h=Math.max(2,scale/9-3*i);d.shape(new Rect(r.x+r.w/2-w/2,feet-h/2,w,h),h/2,0x1E000000);
        }
    }
    private static void zoom(Draw d,State s,Rect r,int mx,int my){
        d.shape(r,5,SURFACE);
        ghost(d,s,"zoom-out",new Rect(r.x,r.y,26,r.h),"−",mx,my,s.zoom>MIN_ZOOM?()->s.zoom=clamp(s.zoom/1.18,MIN_ZOOM,MAX_ZOOM):null,RAISED);
        ghost(d,s,"zoom",new Rect(r.x+26,r.y,r.w-52,r.h),Math.round(s.zoom*100)+"%",mx,my,()->{s.zoom=1;s.turn=0;s.pitch=0;},RAISED);
        ghost(d,s,"zoom-in",new Rect(r.x+r.w-26,r.y,26,r.h),"+",mx,my,s.zoom<MAX_ZOOM?()->s.zoom=clamp(s.zoom*1.18,MIN_ZOOM,MAX_ZOOM):null,RAISED);
        d.fill(r.x+26,r.y+5,r.x+27,r.y+r.h-5,LINE);d.fill(r.x+r.w-27,r.y+5,r.x+r.w-26,r.y+r.h-5,LINE);
    }
    private static String breadcrumb(Object screen,YsmProfile p,State s,List<?> folders,Object selected) {
        if(s.crumb!=null&&s.crumbFor==selected)return s.crumb;
        StringJoiner names=new StringJoiner(" › ");
        if(folders==null)names.add(folderTitle(screen,p,String.valueOf(Reflect.call(Reflect.get(screen,p.path),"getLeft"))));
        else for(Object folder:folders)names.add(folderTitle(screen,p,String.valueOf(Reflect.call(folder,"getLeft"))));
        if(selected!=null)names.add(String.valueOf(Reflect.call(selected,p.b)));
        s.crumbFor=selected;return s.crumb=names.toString();
    }
    private static String folderTitle(Object screen,YsmProfile p,String path) {
        return path.isBlank()?"根目录":localize(screen,p,"properties.extra_animation.#"+path,path);
    }
    /** YSM's folder trail is the wheel's only static LinkedList of (folder, page) pairs; null if absent or ambiguous. */
    private static List<?> folders(Object screen) {
        Class<?> type=screen.getClass();
        if(type!=folderTrailOwner){
            folderTrailOwner=type;folderTrail=null;int found=0;
            for(Field f:type.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&f.getType()==LinkedList.class){folderTrail=f;found++;}
            if(found!=1)folderTrail=null;else folderTrail.setAccessible(true);
        }
        try {return folderTrail!=null&&folderTrail.get(null) instanceof List<?> list&&!list.isEmpty()?list:null;}
        catch(IllegalAccessException e){return null;}
    }
    private static String localize(Object screen,YsmProfile p,String key,String fallback) {
        String resolver=p==YsmProfile.NEO?"OoO0o00Oo0O00oOoO0OOO0oo":"OOOOooO0O0o0OOoOOo00oo00";
        Object data=Reflect.get(screen,"ooOoO0OOO0oOo0Oo00oo000o|o0OooO00ooo0OO000O0OoOoO");
        return (String)Reflect.call(Reflect.type(YsmProfile.ROOT+resolver),p.a,data,key,fallback);
    }
    private static void menu(Object screen,YsmProfile p,Draw d,State s,int mx,int my) {
        Object menu=Reflect.get(screen,p.menu);
        int total=((Number)Reflect.call(menu,"size")).intValue();
        if(s.menu!=menu||s.entries.size()!=total)entries(screen,p,s,menu,total);
        Rect v=s.viewport;int cols=Math.max(1,Math.min(3,(v.w+GAP)/(130+GAP))),cw=(v.w-GAP*(cols-1))/cols;
        int room=v.h,rows=Math.max(1,(total+cols-1)/cols);
        // Use the whole column; give up the bottom row to the pager only when the entries overflow.
        if(rows>(room+GAP)/(34+GAP)){room=v.h-25;rows=Math.max(1,(room+GAP)/(34+GAP));}
        int per=cols*rows,pages=Math.max(1,(total+per-1)/per),ch=Math.min(44,(room+GAP)/rows-GAP);
        if(s.page<0){
            Integer first=FIRST_ENTRY.get(menu);int ysmPage=((Number)Reflect.call(Reflect.get(screen,p.path),"getRight")).intValue();
            s.page=(first!=null&&first/8==ysmPage?first:ysmPage*8)/per;
        }
        s.page=Math.max(0,Math.min(s.page,pages-1));s.per=per;s.pages=pages;
        for(int j=0;j<per&&s.page*per+j<total;j++){
            int index=s.page*per+j;Entry e=s.entries.get(index);
            Rect r=new Rect(v.x+(j%cols)*(cw+GAP),v.y+(j/cols)*(ch+GAP),cw,ch),main=e.configId!=null?new Rect(r.x,r.y,r.w-27,r.h):r;
            boolean nested=e.configId!=null||e.key.startsWith("#");float over=ease(s,"card:"+index,main.contains(mx,my));
            d.shape(r,5,mix(SURFACE,RAISED,over));d.fill(r.x+3,r.y,r.x+r.w-3,r.y+1,0x0FFFFFFF);
            // Entries that open a page carry a side bar, which takes the accent on hover.
            if(nested)d.shape(new Rect(r.x+4,r.y+8,2,r.h-16),1,mix(0x40FFFFFF,ACCENT,over));
            if(e.configId!=null){
                d.label(e.name,new Rect(r.x+13,r.y,r.w-58,r.h),TEXT);
                d.center("›",new Rect(r.x+r.w-42,r.y,10,r.h),mix(FAINT,SUB,over));
                s.hits.add(new Hit("card:"+index,main,main,()->Reflect.call(screen,p.configMethod,e.configId),null,false,true));
                play(d,s,"play:"+index,new Rect(r.x+r.w-27,r.y+6,21,r.h-12),e.key.startsWith("#")?"›":"▶",mx,my,()->activate(screen,p,index,e.key));
            }else{
                d.label(e.name,new Rect(r.x+13,r.y,r.w-(nested?37:26),r.h),TEXT);
                if(nested)d.center(e.key.equals("#return")?"‹":"›",new Rect(r.x+r.w-19,r.y,10,r.h),mix(FAINT,SUB,over));
                s.hits.add(new Hit("card:"+index,r,r,()->activate(screen,p,index,e.key),null,false,true));
            }
        }
        if(total==0)d.center("没有可显示的内容",new Rect(v.x,v.y,v.w,ch),SUB);
        if(pages>1){
            int py=v.y+v.h-19;
            ghost(d,s,"prev",new Rect(v.x,py,28,19),"‹",mx,my,s.page>0?()->turnPage(screen,s,-1):null,RAISED);
            ghost(d,s,"next",new Rect(v.x+v.w-28,py,28,19),"›",mx,my,s.page+1<pages?()->turnPage(screen,s,1):null,RAISED);
            if(pages*9+10<v.w-70){
                int dx=v.x+(v.w-pages*9-1)/2;
                for(int i=0;i<pages;i++){boolean on=i==s.page;d.shape(new Rect(dx,py+8,on?10:4,4),2,on?ACCENT:FAINT);dx+=on?15:9;}
            }else d.center((s.page+1)+" / "+pages,new Rect(v.x+32,py,v.w-64,19),SUB);
        }
    }
    /** The menu of an open wheel never changes, so names and translations are resolved once. */
    private static void entries(Object screen,YsmProfile p,State s,Object menu,int total) {
        Map<?,?> configs=(Map<?,?>)Reflect.get(screen,p.configs);s.menu=menu;s.entries.clear();
        for(int i=0;i<total;i++){
            String key=(String)Reflect.call(menu,p.a,i),value=(String)Reflect.call(menu,p.b,i);
            String configId=value.startsWith("#")&&configs.get(value.substring(1))!=null?value.substring(1):null;
            String name=configId!=null?String.valueOf(Reflect.call(configs.get(configId),p.b)):value;
            s.entries.add(new Entry(key,configId,localize(screen,p,"properties.extra_animation."+key,name.isBlank()?key:name)));
        }
    }
    private static void turnPage(Object screen,State s,int delta) {
        int page=Math.max(0,Math.min(s.page+delta,s.pages-1));if(page==s.page)return;
        s.page=page;int first=page*s.per;
        if(FIRST_ENTRY.size()>64)FIRST_ENTRY.clear();
        // Keep YSM's own page in step: returning from a subfolder reopens this page, as the original wheel does.
        FIRST_ENTRY.put(s.menu,first);Reflect.call(Reflect.get(screen,YsmProfile.of(screen).path),"setValue",first/8);
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
        long now=System.nanoTime();List<Object> displayed=s.controls.update(current,now);
        s.originals.keySet().retainAll(displayed);
        TreeMap<Integer,List<Object>> byY=new TreeMap<>();
        for(Object widget:displayed){
            Original original=s.originals.computeIfAbsent(widget,key->new Original(
                    ((Number)Reflect.call(key,"getX|m_252754_")).intValue(),((Number)Reflect.call(key,"getY|m_252907_")).intValue(),
                    ((Number)Reflect.call(key,"getWidth|m_5711_")).intValue(),((Number)Reflect.call(key,"getHeight|m_93694_")).intValue()));
            byY.computeIfAbsent(original.y,k->new ArrayList<>()).add(widget);
        }
        List<List<Object>> rows=new ArrayList<>(byY.values());int[] tops=new int[rows.size()],heights=new int[rows.size()];int content=0;
        for(int i=0;i<rows.size();i++){
            List<Object> row=rows.get(i);row.sort(Comparator.comparingInt(w->s.originals.get(w).x));
            int height=0;for(Object widget:row)height=Math.max(height,rowHeight(widget,slider,checkbox));
            if(i>0&&height==19)content+=5; // Room above a section label groups the controls below it.
            tops[i]=content;heights[i]=height;content+=height+GAP;
        }
        content=Math.max(0,content-GAP);
        Rect v=s.viewport;s.maxScroll=Math.max(0,content-v.h);int shown=scrollOffset(s);
        if(displayed.isEmpty())d.center(now-s.controls.started()<2_000_000_000L?"正在读取参数"+"...".substring(0,1+(int)(now/350_000_000L%3)):"没有可显示的内容",new Rect(v.x,v.y,v.w,30),SUB);
        d.clip(v);
        try {
            for(int i=0;i<rows.size();i++){
                List<Object> row=rows.get(i);int cell=(v.w-GAP*(row.size()-1)-7)/row.size();
                for(int c=0;c<row.size();c++){
                    // Clipped controls still get keyboard-only targets, so Tab and the arrows can scroll to them.
                    Object widget=row.get(c);Original o=s.originals.get(widget);String id=o.x+","+o.y;
                    Rect r=new Rect(v.x+c*(cell+GAP),v.y+tops[i]-shown,cell,heights[i]);boolean drawn=r.y+r.h>v.y&&r.y<v.y+v.h;
                    if(slider.isInstance(widget))drawSlider(widget,id,r,drawn,d,s,mx,my);
                    else if(checkbox.isInstance(widget))drawCheckbox(widget,id,p,r,drawn,d,s,mx,my,widget.getClass()==checkbox);
                    else if(drawn){int end=d.label(Reflect.text(Reflect.call(widget,"getMessage|m_6035_")),new Rect(r.x+2,r.y,r.w-4,r.h),SUB);d.fill(end+6,r.y+r.h/2,r.x+r.w-2,r.y+r.h/2+1,LINE);}
                }
            }
        } finally {d.unclip();}
        if(s.maxScroll>0){
            s.thumb=Math.max(12,v.h*v.h/Math.max(v.h,content));s.track=new Rect(v.x+v.w-6,v.y,6,v.h);
            int top=v.y+(int)((v.h-s.thumb)*(shown/(double)s.maxScroll));float over=ease(s,"scrollbar",s.scrolling||s.track.contains(mx,my));
            d.shape(new Rect(v.x+v.w-3,top,3,s.thumb),1,s.scrolling?ACCENT:mix(0x66A2A9B3,0xCCA2A9B3,over));
        }
    }
    private static int rowHeight(Object widget,Class<?> slider,Class<?> checkbox){return slider.isInstance(widget)?38:checkbox.isInstance(widget)?25:19;}
    private static void drawSlider(Object widget,String id,Rect r,boolean drawn,Draw d,State s,int mx,int my){
        Rect number=new Rect(r.x+r.w-56,r.y+4,48,15),hit=new Rect(r.x+5,r.y+20,r.w-10,16);
        if(drawn){
            double min=((Number)Reflect.get(widget,"minValue")).doubleValue(),max=((Number)Reflect.get(widget,"maxValue")).doubleValue(),value=((Number)Reflect.call(widget,"getValue")).doubleValue();
            boolean pointer=s.viewport.contains(mx,my),editing=s.editing==widget;
            float over=ease(s,"slider:"+id,s.dragging!=null&&s.dragging.widget==widget||pointer&&hit.contains(mx,my)),chip=ease(s,"value:"+id,editing||pointer&&number.contains(mx,my));
            d.shape(r,5,mix(SURFACE,RAISED,over*.5f));
            d.label(Reflect.text(Reflect.get(widget,"prefix")),new Rect(r.x+10,r.y+3,Math.max(15,r.w-72),17),TEXT);
            d.shape(number,4,mix(0x40000000,0x80000000,chip));
            if(editing){
                d.ring(number,4,s.edit.isEmpty()||parse(s.edit)!=null?ACCENT:DANGER);
                if(s.selectAll&&!s.edit.isEmpty()){int tw=Math.min(number.w-4,d.width(s.edit));d.fill(number.x+(number.w-tw)/2-1,number.y+3,number.x+(number.w+tw)/2+1,number.y+number.h-3,0x8062A8EA);}
            }
            d.center(editing?s.edit+(s.selectAll?"":"_"):(String)Reflect.call(widget,"getValueString"),number,TEXT);
            Rect bar=new Rect(r.x+10,r.y+27,r.w-20,4);d.shape(bar,2,TRACK);
            int fill=(int)Math.round(bar.w*clamp(max==min?0:(value-min)/(max-min),0,1));if(fill>0)d.shape(new Rect(bar.x,bar.y,fill,4),2,ACCENT);
            int k=8+Math.round(2*over);Rect knob=new Rect(bar.x+fill-k/2,bar.y+2-k/2,k,k);
            if(over>0)d.shape(new Rect(knob.x-2,knob.y-2,k+4,k+4),(k+4)/2,mix(0x0062A8EA,0x5562A8EA,over));
            d.shape(knob,k/2,TEXT);
        }
        if(!s.controls.waiting()&&interactive(widget)){
            s.hits.add(new Hit("value:"+id,number,r,()->edit(s,widget),widget,false,fullyVisible(number,s.viewport)));
            s.hits.add(new Hit("slider:"+id,hit,r,null,widget,true,fullyVisible(hit,s.viewport)));
        }
    }
    /** Starts with the whole value selected, so typing replaces it. */
    private static void edit(State s,Object widget){s.editing=widget;s.edit=format(((Number)Reflect.call(widget,"getValue")).doubleValue());s.selectAll=true;}
    /** Plain editable text: no binary-fraction noise such as 0.30000000000000004 and no exponent. */
    static String format(double value){return Double.isFinite(value)?BigDecimal.valueOf(value).round(new MathContext(10)).stripTrailingZeros().toPlainString():Double.toString(value);}
    private static Double parse(String text){
        try{double value=Double.parseDouble(text.strip());return Double.isFinite(value)?value:null;}catch(NumberFormatException e){return null;}
    }
    private static void drawCheckbox(Object widget,String id,YsmProfile p,Rect r,boolean drawn,Draw d,State s,int mx,int my,boolean choice){
        if(drawn){
            boolean checked=(Boolean)Reflect.call(widget,p==YsmProfile.NEO?p.a:"m_94620_");
            float over=ease(s,"check:"+id,s.viewport.contains(mx,my)&&r.contains(mx,my)),on=ease(s,"on:"+id,checked);
            String label=checkboxLabel(widget);
            // In both supported YSM 2.6.5 builds, selection options use the concrete
            // checkbox class; independent booleans use the wheel's anonymous subclass.
            // Keep the option style stable when a group wraps or the GUI is resized.
            if(!choice){
                d.shape(r,5,mix(SURFACE,RAISED,over*.5f));
                d.label(label,new Rect(r.x+10,r.y,r.w-50,r.h),TEXT);
                drawSwitch(d,new Rect(r.x+r.w-34,r.y+(r.h-12)/2,24,12),on);
            }else{
                // The chosen option takes the accent, so a group reads at a glance.
                d.shape(r,5,mix(mix(SURFACE,RAISED,over*.5f),SELECTED,on));
                if(on>0)d.ring(r,5,mix(0x0062A8EA,0xAA62A8EA,on));
                Rect dot=new Rect(r.x+8,r.y+(r.h-8)/2,8,8);d.ring(dot,4,mix(FAINT,ACCENT,on));
                if(on>0)d.shape(new Rect(dot.x+2,dot.y+2,4,4),2,mix(0x0062A8EA,ACCENT,on));
                d.label(label,new Rect(r.x+22,r.y,r.w-28,r.h),mix(SUB,TEXT,Math.max(on,over)));
            }
        }
        if(!s.controls.waiting()&&interactive(widget))s.hits.add(new Hit("check:"+id,r,r,()->{
            Original o=s.originals.get(widget);Reflect.call(widget,"mouseClicked|m_6375_",o.x+o.w/2.0,o.y+o.h/2.0,0);
        },widget,false,fullyVisible(r,s.viewport)));
    }
    private static void drawSwitch(Draw d,Rect track,float on){
        int size=track.h-4;d.shape(track,track.h/2,mix(TRACK,ACCENT,on));
        d.shape(new Rect(track.x+2+Math.round((track.w-4-size)*on),track.y+2,size,size),size/2,mix(0xFFB4BBC4,0xFFFFFFFF,on));
    }
    private static String checkboxLabel(Object widget){
        Class<?> type=widget.getClass();
        if(!LABELS.containsKey(type)){
            Field found=null;
            for(Class<?> c=type;found==null&&c!=null&&c.getName().startsWith(YsmProfile.ROOT);c=c.getSuperclass())for(Field f:c.getDeclaredFields())
                if(found==null&&!Modifier.isStatic(f.getModifiers())&&f.getType().getName().equals("net.minecraft.network.chat.Component")){f.setAccessible(true);found=f;}
            LABELS.put(type,found);
        }
        Field label=LABELS.get(type);
        try{return Reflect.text(label!=null?label.get(widget):Reflect.call(widget,"getMessage|m_6035_"));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private static boolean interactive(Object widget){return (Boolean)Reflect.get(widget,"active|f_93623_")&&(Boolean)Reflect.get(widget,"visible|f_93624_");}
    private static boolean fullyVisible(Rect r,Rect clip){return r.y>=clip.y&&r.y+r.h<=clip.y+clip.h;}
    /** Preview pixels per block, or 0 when the entity cannot be drawn. */
    private static int previewScale(Object entity,State s){
        if(!Reflect.type("net.minecraft.world.entity.LivingEntity").isInstance(entity))return 0;
        Rect r=s.preview;double height=((Number)Reflect.call(entity,"getBbHeight|m_20206_")).doubleValue();
        double width=((Number)Reflect.call(entity,"getBbWidth|m_20205_")).doubleValue();
        return Math.max(1,(int)(Math.min((r.h-14)/Math.max(1.8,height),(r.w-20)/Math.max(1.2,width*1.4))*.88*s.zoom));
    }
    private static void preview(Object graphics,Object entity,YsmProfile p,State s,int scale){
        Rect r=s.preview;
        Reflect.call(graphics,"enableScissor|m_280588_",r.x,r.y,r.x+r.w,r.y+r.h);
        try {
            PreviewRenderer.render(graphics,entity,r,scale,s.turn,s.pitch,p==YsmProfile.NEO);
        } finally {Reflect.call(graphics,"disableScissor|m_280618_");}
    }
    private static void hint(Draw d,State s,int cx,int y){
        String text="拖动旋转 · 滚轮缩放";int w=Math.min(s.preview.w-12,d.width(text)+16);
        Rect pill=new Rect(cx-w/2,y,w,14);d.shape(pill,7,0xCC0E1013);d.center(text,pill,SUB);
    }
    /** Filled button; a null action draws it disabled and leaves no target. */
    private static void button(Draw d,State s,Object id,Rect rect,String text,int mx,int my,Runnable action){
        float over=action==null?0:ease(s,id,rect.contains(mx,my));
        d.shape(rect,5,action==null?0x40262A30:mix(SURFACE,RAISED,over));d.center(text,rect,action==null?FAINT:TEXT);
        if(action!=null)s.hits.add(new Hit(id,rect,rect,action,null,false,true));
    }
    /** Transparent until hovered, then fades in the given colour; a null action draws it disabled. */
    private static void ghost(Draw d,State s,Object id,Rect rect,String text,int mx,int my,Runnable action,int hover){
        float over=action==null?0:ease(s,id,rect.contains(mx,my));
        if(over>0)d.shape(rect,5,mix(hover&0xFFFFFF,hover,over));
        d.center(text,rect,action==null?FAINT:mix(SUB,TEXT,over));
        if(action!=null)s.hits.add(new Hit(id,rect,rect,action,null,false,true));
    }
    private static void play(Draw d,State s,Object id,Rect rect,String glyph,int mx,int my,Runnable action){
        float over=ease(s,id,rect.contains(mx,my));
        d.shape(rect,4,mix(0x30000000,ACCENT,over));d.center(glyph,rect,mix(ACCENT,ON_ACCENT,over));
        s.hits.add(new Hit(id,rect,rect,action,null,false,true));
    }
    /** Eases toward a hover or on state over about 0.12 s and returns 0..1 for colour blending. */
    private static float ease(State s,Object id,boolean on){
        Glow g=s.glow.get(id);float target=on?1:0;
        if(g==null){if(s.glow.size()>512)s.glow.clear();s.glow.put(id,g=new Glow());g.t=target;}
        // A target missing from the previous frame (another page, or disabled) starts settled instead of resuming.
        else if(g.frame<s.frame-1)g.t=target;
        else if(g.frame==s.frame-1&&g.t!=target)g.t=on?Math.min(1,g.t+s.dt*8):Math.max(0,g.t-s.dt*8);
        g.frame=s.frame;return g.t*g.t*(3-2*g.t);
    }
    private static int mix(int from,int to,float t){
        if(t<=0)return from;if(t>=1)return to;int out=0;
        for(int shift=0;shift<32;shift+=8){int a=from>>>shift&255,b=to>>>shift&255;out|=Math.round(a+(b-a)*t)<<shift;}
        return out;
    }
    private static Hit hitAt(State s,double x,double y){for(Hit hit:s.hits)if(hit.pointer&&hit.rect.contains(x,y))return hit;return null;}
    private static void resolveFocus(State s){
        s.focus=-1;
        for(int i=0;s.focusId!=null&&i<s.hits.size();i++)if(s.focusId.equals(s.hits.get(i).id)){s.focus=i;return;}
    }
    public static boolean clicked(Object screen,double x,double y,int button){
        State s=state(screen);
        try {
            s.keyboardFocus=false;if(button!=0)return false;
            s.editing=null;s.rotating=false;s.dragging=null;s.scrolling=false;
            if(s.track!=null&&s.track.contains(x,y)){s.scrolling=true;scrollTo(s,y);return true;}
            Hit hit=hitAt(s,x,y);
            if(hit!=null){s.focusId=hit.id;s.focus=s.hits.indexOf(hit);if(hit.slider){s.dragging=hit;sliderMouse(s,hit,x);}else run(hit);return true;}
            if(s.preview!=null&&s.preview.contains(x,y)){s.rotating=true;return true;}return false;
        } catch(RuntimeException|LinkageError error){fail(s,error);return true;}
    }
    private static void scrollTo(State s,double y){Rect v=s.viewport;s.scroll=(int)Math.round(clamp((y-v.y-s.thumb/2.0)/Math.max(1,v.h-s.thumb),0,1)*s.maxScroll);}
    private static void sliderMouse(State s,Hit hit,double x){
        Original o=s.originals.get(hit.widget);if(o==null){s.dragging=null;return;}
        double fraction=clamp((x-hit.rect.x-4)/Math.max(1,hit.rect.w-8),0,1);
        double px=o.x+4+fraction*Math.max(1,o.w-8);
        Reflect.call(hit.widget,"onClick|m_5716_",px,o.y+o.h/2.0);
    }
    public static boolean dragged(Object screen,double x,double y,int button,double dx,double dy){
        State s=state(screen);if(button!=0)return false;
        try {
            if(s.scrolling){scrollTo(s,y);return true;}
            if(s.dragging!=null){sliderMouse(s,s.dragging,x);return true;}
            if(s.rotating){s.turn=Math.IEEEremainder(s.turn+dx*360/Math.max(160,s.preview.w),360);s.pitch=clamp(s.pitch+dy*90/Math.max(100,s.preview.h),-45,45);return true;}
            return false;
        } catch(RuntimeException|LinkageError error){fail(s,error);return true;}
    }
    public static boolean released(Object screen,double x,double y,int button){
        State s=state(screen);if(button!=0)return false;
        boolean result=s.dragging!=null||s.rotating||s.scrolling;
        if(s.dragging!=null)clickSound();
        s.dragging=null;s.rotating=false;s.scrolling=false;return result;
    }
    public static boolean scrolled(Object screen,double x,double y,double amount){
        State s=state(screen);
        try {
            if(s.preview!=null&&s.preview.contains(x,y)){s.zoom=clamp(s.zoom*Math.pow(1.12,amount),MIN_ZOOM,MAX_ZOOM);return true;}
            if(s.viewport==null||!s.viewport.contains(x,y))return false;
            if(s.selected!=null){s.scroll=(int)clamp(scrollOffset(s)-amount*28,0,s.maxScroll);return true;}
            // Touchpads report fractions of a notch; turn one page per accumulated notch.
            s.wheel=Math.signum(amount)==Math.signum(s.wheel)?s.wheel+amount:amount;
            if(Math.abs(s.wheel)>=1&&s.page>=0){turnPage(screen,s,s.wheel<0?1:-1);s.wheel=0;}
            return true;
        } catch(RuntimeException|LinkageError error){fail(s,error);return true;}
    }
    public static boolean key(Object screen,int key,int scan,int modifiers){
        if(!active(screen))return false;State s=state(screen);
        try {
            if(s.editing!=null)return editKey(screen,s,key,modifiers);
            Hit focused=s.focus>=0&&s.focus<s.hits.size()?s.hits.get(s.focus):null;
            // Consume navigation keys: vanilla would otherwise focus and operate YSM's hidden original widgets.
            if(key==258){move(s,(modifiers&1)!=0?-1:1);return true;}
            if(key==262||key==263){
                if(focused!=null&&focused.slider)keySlider(focused.widget,key,scan,modifiers,YsmProfile.of(screen)==YsmProfile.NEO);else step(s,key==262?1:-1,0);
                return true;
            }
            if(key==264||key==265){step(s,0,key==264?1:-1);return true;}
            if(key==257||key==335||key==32){if(focused!=null)press(s,focused);return true;}
            if(key==266||key==267){page(screen,s,key==267?1:-1);return true;}
            return false;
        } catch(RuntimeException|LinkageError error){fail(s,error);return true;}
    }
    private static boolean editKey(Object screen,State s,int key,int modifiers){
        if(key==256){s.editing=null;return true;}
        if(key==257||key==335){Double value=parse(s.edit);if(value!=null){setSliderValue(s.editing,value,YsmProfile.of(screen)==YsmProfile.NEO);s.editing=null;}return true;}
        if(key==259){s.edit=s.selectAll||s.edit.isEmpty()?"":s.edit.substring(0,s.edit.length()-1);s.selectAll=false;}
        else if(key==65&&(modifiers&2)!=0)s.selectAll=true;
        else if(key>=262&&key<=269)s.selectAll=false;
        return true;
    }
    private static void press(State s,Hit hit){s.keyboardFocus=true;if(hit.slider)edit(s,hit.widget);else run(hit);}
    /** Panel-drawn targets click like vanilla buttons; author widgets play their own sound. */
    private static void run(Hit hit){if(hit.widget==null)clickSound();hit.click.run();}
    private static void move(State s,int direction){
        int n=s.hits.size();if(n==0)return;
        focusOn(s,s.focus>=0&&s.focus<n?Math.floorMod(s.focus+direction,n):direction>0?Math.min(s.contentStart,n-1):n-1);
    }
    /** Arrow keys pick the nearest target past the focused edge in that direction, preferring targets in line with it. */
    private static void step(State s,int dx,int dy){
        if(s.focus<0||s.focus>=s.hits.size()){move(s,1);return;}
        Rect a=s.hits.get(s.focus).rect;int best=-1,score=Integer.MAX_VALUE;
        for(int i=0;i<s.hits.size();i++){
            // Centres alone are not enough: a full-width card below a card with a play button has its centre further right.
            Rect b=s.hits.get(i).rect;int along=dx>0?b.x-a.x-a.w:dx<0?a.x-b.x-b.w:dy>0?b.y-a.y-a.h:a.y-b.y-b.h;
            int across=dx!=0?gap(a.y,a.y+a.h,b.y,b.y+b.h):gap(a.x,a.x+a.w,b.x,b.x+b.w),cost=Math.max(0,along)+across*4;
            if(i!=s.focus&&along>=-2&&cost<score){score=cost;best=i;}
        }
        if(best>=0)focusOn(s,best);else s.keyboardFocus=true;
    }
    private static int gap(int start,int end,int otherStart,int otherEnd){return Math.max(0,Math.max(start,otherStart)-Math.min(end,otherEnd));}
    private static void focusOn(State s,int index){Hit hit=s.hits.get(index);s.keyboardFocus=true;s.focus=index;s.focusId=hit.id;reveal(s,hit);}
    /** Scrolls the parameter list so a keyboard-focused control is fully visible. */
    private static void reveal(State s,Hit hit){
        Rect v=s.viewport,area=hit.area;if(hit.widget==null||v==null)return;
        int shown=scrollOffset(s);
        if(area.y<v.y)s.scroll=Math.max(0,shown-(v.y-area.y));
        else if(area.y+area.h>v.y+v.h)s.scroll=Math.min(s.maxScroll,shown+area.y+area.h-v.y-v.h);
    }
    private static void page(Object screen,State s,int direction){
        if(s.selected==null){if(s.page>=0)turnPage(screen,s,direction);}
        else if(s.viewport!=null)s.scroll=(int)clamp(scrollOffset(s)+direction*Math.max(20,s.viewport.h-24),0,s.maxScroll);
    }
    public static boolean character(Object screen,char ch,int modifiers){
        State s=state(screen);if(s.editing==null)return false;
        if("0123456789.+-eE".indexOf(ch)>=0){if(s.selectAll){s.edit="";s.selectAll=false;}if(s.edit.length()<32)s.edit+=ch;}
        return true;
    }
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
    /** Vanilla buttons click when pressed and sliders when released; panel-drawn targets do the same. */
    private static void clickSound(){
        if(silent)return;
        try {
            Object click=Reflect.get(Reflect.type("net.minecraft.sounds.SoundEvents"),"UI_BUTTON_CLICK|f_12490_");
            Object sound=Reflect.call(Reflect.type("net.minecraft.client.resources.sounds.SimpleSoundInstance"),"forUI|m_263171_",click,1f);
            Reflect.call(Reflect.call(Reflect.minecraft(),"getSoundManager|m_91106_"),"play|m_120367_",sound);
        } catch(RuntimeException|LinkageError error) {
            silent=true;System.getLogger("ysm_enhancements").log(System.Logger.Level.WARNING,"[YSM patch] Button panel click sound unavailable: "+error);
        }
    }
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static int scrollOffset(State s){return Math.max(0,Math.min(s.scroll,s.maxScroll));}
    private static final class Draw {
        final Object g,font;final State s;final Method fill,text,width;final int line;
        Draw(Object graphics,State state){
            g=graphics;s=state;font=Reflect.get(Reflect.minecraft(),"font|f_91062_");line=Reflect.integer(font,"lineHeight|f_92710_");
            // A frame issues hundreds of fills and strings; resolve them once instead of on every call.
            fill=Reflect.method(g,"fill|m_280509_",0,0,0,0,0);text=Reflect.method(g,"drawString|m_280056_",font,"",0,0,0,false);width=Reflect.method(font,"width|m_92895_","");
        }
        void fill(int x,int y,int right,int bottom,int color){if(right>x&&bottom>y)Reflect.invoke(fill,g,x,y,right,bottom,color);}
        /** Pixel inset of a rounded corner's row; radius 5 gives the 3-1-1 corner of the 1.0 panel. */
        static int inset(int radius,int row){double d=radius-row-.5;return radius-(int)Math.round(Math.sqrt(radius*radius-d*d));}
        static int flat(int radius){int rows=radius;while(rows>0&&inset(radius,rows-1)==0)rows--;return rows;}
        void shape(Rect r,int radius,int color){
            radius=Math.min(radius,Math.min(r.w,r.h)/2);int flat=flat(radius);
            for(int i=0;i<flat;){
                int inset=inset(radius,i),j=i+1;while(j<flat&&inset(radius,j)==inset)j++;
                fill(r.x+inset,r.y+i,r.x+r.w-inset,r.y+j,color);fill(r.x+inset,r.y+r.h-j,r.x+r.w-inset,r.y+r.h-i,color);i=j;
            }
            fill(r.x,r.y+flat,r.x+r.w,r.y+r.h-flat,color);
        }
        /** One-pixel outline that follows {@link #shape}'s corners. */
        void ring(Rect r,int radius,int color){
            radius=Math.min(radius,Math.min(r.w,r.h)/2);int flat=flat(radius),top=flat>0?inset(radius,0):0;
            fill(r.x+top,r.y,r.x+r.w-top,r.y+1,color);fill(r.x+top,r.y+r.h-1,r.x+r.w-top,r.y+r.h,color);
            for(int i=1;i<flat;i++){
                int in=inset(radius,i),to=Math.max(in+1,inset(radius,i-1));
                fill(r.x+in,r.y+i,r.x+to,r.y+i+1,color);fill(r.x+r.w-to,r.y+i,r.x+r.w-in,r.y+i+1,color);
                fill(r.x+in,r.y+r.h-1-i,r.x+to,r.y+r.h-i,color);fill(r.x+r.w-to,r.y+r.h-1-i,r.x+r.w-in,r.y+r.h-i,color);
            }
            fill(r.x,r.y+Math.max(1,flat),r.x+1,r.y+r.h-Math.max(1,flat),color);fill(r.x+r.w-1,r.y+Math.max(1,flat),r.x+r.w,r.y+r.h-Math.max(1,flat),color);
        }
        int width(String value){return ((Number)Reflect.invoke(width,font,value)).intValue();}
        /** Shortens on code points with an ellipsis; cached per screen because labels rarely change between frames. */
        Fitted fit(String value,int available){
            String key=available+"\n"+value;Fitted cached=s.fitted.get(key);if(cached!=null)return cached;
            String shown="";
            if(available>0&&width(value)<=available)shown=value;
            else if(available>0&&width("…")<=available){
                int end=value.length();
                while(end>0&&width(value.substring(0,end)+"…")>available)end=value.offsetByCodePoints(end,-1);
                shown=value.substring(0,end)+"…";
            }
            if(s.fitted.size()>512)s.fitted.clear();
            cached=new Fitted(shown,shown.isEmpty()?0:width(shown));s.fitted.put(key,cached);return cached;
        }
        int label(String value,Rect rect,int color){return aligned(value,rect,color,false);}
        void center(String value,Rect rect,int color){aligned(value,rect,color,true);}
        /** Folder trail: leading segments drop first, and only the current page is drawn in full text colour. */
        void trail(String value,Rect rect){
            for(int cut=value.indexOf(" › ");cut>=0&&width(value)>rect.w;cut=value.indexOf(" › ",2))value="…"+value.substring(cut);
            String[] parts=value.split(" › ",-1);int x=rect.x;
            for(int i=0;i<parts.length;i++){
                if(i>0)x=aligned("›",new Rect(x+4,rect.y,rect.x+rect.w-x-4,rect.h),FAINT,false)+4;
                x=aligned(parts[i],new Rect(x,rect.y,rect.x+rect.w-x,rect.h),i==parts.length-1?TEXT:SUB,false);
            }
        }
        int aligned(String value,Rect rect,int color,boolean centered){
            Fitted shown=fit(value,rect.w);if(shown.text.isEmpty())return rect.x;
            // Match AbstractWidget's vertical alignment, including its baseline offset.
            int y=rect.y+(rect.h-line)/2+1,x=centered?rect.x+(rect.w-shown.width)/2:rect.x;
            Reflect.invoke(text,g,font,shown.text,x,y,color,false);return x+shown.width;
        }
        void clip(Rect r){Reflect.call(g,"enableScissor|m_280588_",r.x,r.y,r.x+r.w,r.y+r.h);}
        void unclip(){Reflect.call(g,"disableScissor|m_280618_");}
    }
}
