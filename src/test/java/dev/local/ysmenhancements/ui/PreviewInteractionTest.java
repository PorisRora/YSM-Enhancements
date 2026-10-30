package dev.local.ysmenhancements.ui;

import dev.local.ysmenhancements.RegressionTest;
import dev.local.ysmenhancements.compat.Reflect;
import java.util.List;
import org.joml.Vector3f;

public final class PreviewInteractionTest {
    public static final class Screen { public Object ysm$buttonPanelState; }
    public static final class Entity {
        public float yBodyRot=12,yHeadRot=34,yHeadRotO=56;
        private float yaw=78,pitch=23;
        public float getYRot(){return yaw;}public float getXRot(){return pitch;}
        public void setYRot(float value){yaw=value;}public void setXRot(float value){pitch=value;}
    }
    public static final class ForgeEntity {
        public float f_20883_=12,f_20885_=34,f_20886_=56;
        private float yaw=78,pitch=23;
        public float m_146908_(){return yaw;}public float m_146909_(){return pitch;}
        public void m_146922_(float value){yaw=value;}public void m_146926_(float value){pitch=value;}
    }
    public static final class Slider { public double getValue(){return .1+.2;} }
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception {
        Screen screen=new Screen();ButtonPanel.active(screen);Object state=screen.ysm$buttonPanelState;
        var hit=Class.forName("dev.local.ysmenhancements.ui.ButtonPanel$Hit").getDeclaredConstructors()[0];hit.setAccessible(true);
        List<Object> hits=(List<Object>)Reflect.get(state,"hits");int[] clicks={0};ButtonPanel.Rect button=new ButtonPanel.Rect(0,0,20,20);
        hits.add(hit.newInstance("button",button,button,(Runnable)()->clicks[0]++,null,false,true));
        ButtonPanel.clicked(screen,10,10,0);
        RegressionTest.check(clicks[0]==1&&!(Boolean)Reflect.get(state,"keyboardFocus"),"mouse click activates without sticky outline");
        ButtonPanel.key(screen,258,0,0);
        RegressionTest.check((Boolean)Reflect.get(state,"keyboardFocus"),"Tab shows focus for keyboard navigation");
        ButtonPanel.clicked(screen,10,10,0);
        RegressionTest.check(!(Boolean)Reflect.get(state,"keyboardFocus"),"mouse click clears keyboard outline");
        Reflect.set(state,"preview",new ButtonPanel.Rect(0,30,320,240));
        RegressionTest.check(ButtonPanel.clicked(screen,100,100,0),"preview accepts left drag");
        ButtonPanel.dragged(screen,180,100,0,80,0);
        RegressionTest.check(Math.abs(((Number)Reflect.get(state,"turn")).doubleValue()-90)<1e-8,"quarter width drag reaches side");
        ButtonPanel.dragged(screen,260,100,0,80,0);
        RegressionTest.check(Math.abs(((Number)Reflect.get(state,"turn")).doubleValue()-180)<1e-8,"half width drag reaches back");
        Vector3f facing=PreviewRenderer.orientation(180,0).transform(new Vector3f(0,0,1));
        RegressionTest.check(facing.z<-.999f,"renderer quaternion turns the whole model backwards");
        ButtonPanel.dragged(screen,500,100,0,160,5000);
        RegressionTest.check(Math.abs(((Number)Reflect.get(state,"turn")).doubleValue())<1e-8&&((Number)Reflect.get(state,"pitch")).doubleValue()==45,"full turn wraps and tilt is bounded");
        ButtonPanel.released(screen,500,100,1);
        RegressionTest.check((Boolean)Reflect.get(state,"rotating"),"unrelated mouse release does not cancel left drag");
        ButtonPanel.released(screen,500,100,0);
        RegressionTest.check(!ButtonPanel.dragged(screen,550,100,0,50,0),"release ends rotation");
        ControlView view=(ControlView)Reflect.get(state,"controls");
        List<Object> old=List.of(new Object(),new Object(),new Object());
        view.update(old,0);Reflect.set(state,"scroll",280);Reflect.set(state,"maxScroll",500);
        ButtonPanel.rebuilt(screen);
        RegressionTest.check(Reflect.integer(state,"scroll")==280,"rebuild retains requested scroll");
        RegressionTest.check(view.update(List.of(),System.nanoTime()).equals(old)&&view.waiting(),"empty async frame keeps previous form in place");
        RegressionTest.check(view.update(List.of(new Object()),System.nanoTime()).equals(old),"partial async callback does not move visible content");
        List<Object> fresh=List.of(new Object(),new Object(),new Object());
        RegressionTest.check(view.update(fresh,System.nanoTime()).equals(fresh)&&!view.waiting()&&Reflect.integer(state,"scroll")==280,"complete replacement keeps scroll and unlocks controls");
        view.begin(0);RegressionTest.check(view.update(List.of(),3_000_000_000L).isEmpty()&&!view.waiting(),"failed callbacks cannot leave stale controls indefinitely");
        view.clear();RegressionTest.check(view.update(List.of(new Object()),0).size()==1,"different form has no stale snapshot");
        Screen keys=new Screen();ButtonPanel.active(keys);Object ks=keys.ysm$buttonPanelState;
        for(int key:new int[]{258,262,263,264,265,257,335,32})RegressionTest.check(ButtonPanel.key(keys,key,0,0),"navigation key "+key+" never reaches YSM's hidden widgets");
        RegressionTest.check(!ButtonPanel.key(keys,256,0,0),"Escape still closes the screen");
        List<Object> grid=(List<Object>)Reflect.get(ks,"hits");int[] pressed=new int[4];
        Reflect.set(ks,"viewport",new ButtonPanel.Rect(0,0,200,100));Reflect.set(ks,"maxScroll",300);
        ButtonPanel.Rect[] cells={new ButtonPanel.Rect(0,0,90,40),new ButtonPanel.Rect(100,0,90,40),new ButtonPanel.Rect(0,50,90,40),new ButtonPanel.Rect(0,160,90,40)};
        for(int i=0;i<cells.length;i++){int n=i;grid.add(hit.newInstance("cell"+i,cells[i],cells[i],(Runnable)()->pressed[n]++,i==3?new Object():null,false,i!=3));}
        ButtonPanel.key(keys,258,0,0);ButtonPanel.key(keys,262,0,0);
        RegressionTest.check("cell1".equals(Reflect.get(ks,"focusId")),"Right moves to the neighbouring card");
        ButtonPanel.key(keys,264,0,0);
        RegressionTest.check("cell2".equals(Reflect.get(ks,"focusId")),"Down moves to the nearest target below");
        ButtonPanel.key(keys,264,0,0);
        RegressionTest.check("cell3".equals(Reflect.get(ks,"focusId"))&&Reflect.integer(ks,"scroll")==100,"keyboard focus scrolls a clipped control into view");
        ButtonPanel.key(keys,257,0,0);
        RegressionTest.check(pressed[3]==1,"Enter activates the focused control");
        RegressionTest.check(!ButtonPanel.clicked(keys,10,170,0)&&pressed[3]==1,"clipped control ignores the mouse");
        grid.clear();grid.add(hit.newInstance("cell2",cells[2],cells[2],(Runnable)()->{},null,false,true));grid.add(hit.newInstance("cell3",cells[0],cells[0],(Runnable)()->{},null,false,true));
        var resolve=ButtonPanel.class.getDeclaredMethod("resolveFocus",Class.forName("dev.local.ysmenhancements.ui.ButtonPanel$State"));resolve.setAccessible(true);resolve.invoke(null,ks);
        RegressionTest.check(Reflect.integer(ks,"focus")==1,"focus follows its target when the hit list is rebuilt");
        Screen form=new Screen();ButtonPanel.active(form);Object fs=form.ysm$buttonPanelState;Object slider=new Slider();ButtonPanel.Rect bar=new ButtonPanel.Rect(0,0,100,16);
        ((List<Object>)Reflect.get(fs,"hits")).add(hit.newInstance("slider",bar,bar,null,slider,true,true));
        ButtonPanel.key(form,258,0,0);ButtonPanel.key(form,257,0,0);
        RegressionTest.check(Reflect.get(fs,"editing")==slider&&"0.3".equals(Reflect.get(fs,"edit")),"Enter on a slider edits its rounded value");
        ButtonPanel.character(form,'5',0);ButtonPanel.character(form,'x',0);ButtonPanel.character(form,'0',0);
        RegressionTest.check("50".equals(Reflect.get(fs,"edit")),"typing replaces the selected value and ignores non-numbers");
        ButtonPanel.key(form,259,0,0);RegressionTest.check("5".equals(Reflect.get(fs,"edit")),"Backspace removes one character");
        ButtonPanel.key(form,256,0,0);RegressionTest.check(Reflect.get(fs,"editing")==null,"Escape cancels editing");
        RegressionTest.check(ButtonPanel.format(1e-5).equals("0.00001")&&ButtonPanel.format(100).equals("100")&&ButtonPanel.format(1.05).equals("1.05")&&ButtonPanel.format(-0.0).equals("0"),"plain value formatting");
        Reflect.set(fs,"viewport",new ButtonPanel.Rect(0,0,200,100));Reflect.set(fs,"maxScroll",300);Reflect.set(fs,"thumb",25);Reflect.set(fs,"track",new ButtonPanel.Rect(194,0,6,100));
        RegressionTest.check(ButtonPanel.clicked(form,196,99,0)&&Reflect.integer(fs,"scroll")==300,"scrollbar track click jumps to the end");
        ButtonPanel.dragged(form,196,12.5,0,0,-86.5);RegressionTest.check(Reflect.integer(fs,"scroll")==0,"scrollbar drag returns to the top");
        ButtonPanel.released(form,196,12.5,0);RegressionTest.check(!ButtonPanel.dragged(form,196,50,0,0,0),"release ends the scrollbar drag");
        for(Object entity:new Object[]{new Entity(),new ForgeEntity()}) {
            try {
                PreviewRenderer.withNeutralPose(entity,()->{
                    RegressionTest.check(((Number)Reflect.get(entity,"yBodyRot|f_20883_")).floatValue()==180,"preview neutral body pose");
                    RegressionTest.check(((Number)Reflect.call(entity,"getXRot|m_146909_")).floatValue()==0,"preview neutral pitch");
                    throw new IllegalArgumentException("test renderer failure");
                });
                throw new AssertionError("Expected renderer exception");
            }catch(IllegalArgumentException expected){}
            RegressionTest.check(((Number)Reflect.get(entity,"yBodyRot|f_20883_")).floatValue()==12&&((Number)Reflect.get(entity,"yHeadRot|f_20885_")).floatValue()==34&&((Number)Reflect.get(entity,"yHeadRotO|f_20886_")).floatValue()==56,"restore body/head pose even when renderer fails");
            RegressionTest.check(((Number)Reflect.call(entity,"getYRot|m_146908_")).floatValue()==78&&((Number)Reflect.call(entity,"getXRot|m_146909_")).floatValue()==23,"restore actual player camera rotation");
        }
        System.out.println("PASS: mouse vs keyboard focus, full preview rotation, pose restoration, async scroll preservation, keyboard navigation and value editing");
    }
}
