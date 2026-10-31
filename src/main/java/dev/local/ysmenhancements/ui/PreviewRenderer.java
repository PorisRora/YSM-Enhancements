package dev.local.ysmenhancements.ui;

import dev.local.ysmenhancements.compat.Reflect;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rotates the whole rendered model, independent of its head/body animation expressions. */
public final class PreviewRenderer {
    private PreviewRenderer() {}
    static Quaternionf orientation(double yaw,double pitch) {
        return new Quaternionf().rotationX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw));
    }
    public static void render(Object graphics,Object entity,ButtonPanel.Rect rect,int scale,double yaw,double pitch,boolean neo) {
        Quaternionf camera=orientation(yaw,pitch);
        Quaternionf pose=new Quaternionf().rotationZ((float)Math.PI).mul(camera);
        Object inventory=Reflect.type("net.minecraft.client.gui.screens.inventory.InventoryScreen");
        // Inventory rendering uses z=50, which is too shallow for an enlarged,
        // freely rotating model over GUI quads at z=0. Isolate depth inside the
        // caller's preview scissor; retain depth testing between model surfaces.
        Object renderSystem=Reflect.type("com.mojang.blaze3d.systems.RenderSystem");
        Object gl=Reflect.type("org.lwjgl.opengl.GL11");
        withIsolatedDepth(new DepthAccess(){
            public void flush(){Reflect.call(graphics,"flush|m_280262_");}
            public boolean mask(){return (Boolean)Reflect.call(gl,"glGetBoolean",0x0B72);}
            public double clearValue(){return ((Number)Reflect.call(gl,"glGetDouble",0x0B73)).doubleValue();}
            public void mask(boolean value){Reflect.call(renderSystem,"depthMask",value);}
            public void clearValue(double value){Reflect.call(renderSystem,"clearDepth",value);}
            public void clear(){Reflect.call(renderSystem,"clear",0x0100,false);}
        },()->withNeutralPose(entity,()->{
            if(neo){
                float height=((Number)Reflect.call(entity,"getBbHeight")).floatValue();
                float entityScale=((Number)Reflect.call(entity,"getScale")).floatValue();
                Reflect.call(inventory,"renderEntityInInventory",graphics,rect.x()+rect.w()/2f,rect.y()+rect.h()/2f,
                        scale/Math.max(.001f,entityScale),new Vector3f(0,height/2,0),pose,camera,entity);
            }else Reflect.call(inventory,"renderEntityInInventory|m_280432_",graphics,rect.x()+rect.w()/2,rect.y()+rect.h()-4,scale,pose,camera,entity);
        }));
    }
    /** Screen y of the model's feet: both renderer paths pivot the model there. */
    public static int feet(Object entity,ButtonPanel.Rect rect,int scale,boolean neo) {
        if(!neo)return rect.y()+rect.h()-4;
        float height=((Number)Reflect.call(entity,"getBbHeight")).floatValue(),entityScale=((Number)Reflect.call(entity,"getScale")).floatValue();
        return Math.round(rect.y()+rect.h()/2f+height/2*scale/Math.max(.001f,entityScale));
    }
    interface DepthAccess {
        void flush();boolean mask();double clearValue();void mask(boolean value);void clearValue(double value);void clear();
    }
    static void withIsolatedDepth(DepthAccess depth,Runnable draw) {
        depth.flush();boolean mask=depth.mask();double clear=depth.clearValue();
        try {
            depth.mask(true);depth.clearValue(1);depth.clear();draw.run();
        } finally {
            // Do not let the 3D preview occlude controls drawn later in the frame.
            try {
                depth.flush();depth.mask(true);depth.clearValue(1);depth.clear();
            } finally {
                depth.clearValue(clear);depth.mask(mask);
            }
        }
    }
    static void withNeutralPose(Object entity,Runnable draw) {
        Object body=Reflect.get(entity,"yBodyRot|f_20883_"),head=Reflect.get(entity,"yHeadRot|f_20885_"),oldHead=Reflect.get(entity,"yHeadRotO|f_20886_");
        Object yaw=Reflect.call(entity,"getYRot|m_146908_"),pitch=Reflect.call(entity,"getXRot|m_146909_");
        try {
            Reflect.set(entity,"yBodyRot|f_20883_",180f);Reflect.set(entity,"yHeadRot|f_20885_",180f);Reflect.set(entity,"yHeadRotO|f_20886_",180f);
            Reflect.call(entity,"setYRot|m_146922_",180f);Reflect.call(entity,"setXRot|m_146926_",0f);
            draw.run();
        }finally {
            Reflect.set(entity,"yBodyRot|f_20883_",body);Reflect.set(entity,"yHeadRot|f_20885_",head);Reflect.set(entity,"yHeadRotO|f_20886_",oldHead);
            Reflect.call(entity,"setYRot|m_146922_",yaw);Reflect.call(entity,"setXRot|m_146926_",pitch);
        }
    }
}
