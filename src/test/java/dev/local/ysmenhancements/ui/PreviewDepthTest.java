package dev.local.ysmenhancements.ui;

import dev.local.ysmenhancements.RegressionTest;
import java.nio.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

/** Real OpenGL depth/scissor regression in a hidden, disposable GLFW window. */
public final class PreviewDepthTest {
    static final PreviewRenderer.DepthAccess DEPTH=new PreviewRenderer.DepthAccess(){
        public void flush(){glFlush();}public boolean mask(){return glGetBoolean(GL_DEPTH_WRITEMASK);}
        public double clearValue(){return glGetDouble(GL_DEPTH_CLEAR_VALUE);}
        public void mask(boolean value){glDepthMask(value);}public void clearValue(double value){glClearDepth(value);}
        public void clear(){glClear(GL_DEPTH_BUFFER_BIT);}
    };
    public static void main(String[] args) {
        if(!glfwInit())throw new AssertionError("GLFW unavailable");long window=0;
        try {
            glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_DEPTH_BITS,24);
            window=glfwCreateWindow(64,64,"YSM preview depth regression",0,0);
            if(window==0)throw new AssertionError("Hidden GL context unavailable");
            glfwMakeContextCurrent(window);GL.createCapabilities();
            glViewport(0,0,64,64);glEnable(GL_DEPTH_TEST);glDepthFunc(GL_LEQUAL);
            reset();quad(-1,1,0,0,0,1);quad(-.7f,.7f,.5f,0,1,0);
            RegressionTest.check(pixel(24,32)[2]>200,"reproduce: rear surface is hidden by panel background at z=0");
            reset();quad(-1,1,0,0,0,1);glEnable(GL_SCISSOR_TEST);glScissor(8,8,48,48);
            glDepthMask(false);glClearDepth(.625);
            PreviewRenderer.withIsolatedDepth(DEPTH,()->{
                quad(.1f,.7f,-.5f,1,0,0); // closer part first
                quad(-.7f,.7f,.5f,0,1,0); // rear part must still be depth-tested against it
            });
            RegressionTest.check(pixel(24,32)[1]>200,"rear surface now visible");
            RegressionTest.check(pixel(44,32)[0]>200,"model self-occlusion preserved");
            RegressionTest.check(pixel(3,32)[2]>200&&Math.abs(depth(3,32)-.5)<.001,"outside preview retains color and depth");
            RegressionTest.check(Math.abs(depth(24,32)-1)<.001,"preview depth released for later GUI controls");
            RegressionTest.check(!glGetBoolean(GL_DEPTH_WRITEMASK)&&Math.abs(glGetDouble(GL_DEPTH_CLEAR_VALUE)-.625)<.001,"depth mask and clear value restored");
            RegressionTest.check(glIsEnabled(GL_DEPTH_TEST)&&glIsEnabled(GL_SCISSOR_TEST),"depth test and clipping remain enabled");
            try{PreviewRenderer.withIsolatedDepth(DEPTH,()->{throw new IllegalArgumentException("render failure");});throw new AssertionError();}
            catch(IllegalArgumentException expected){}
            RegressionTest.check(!glGetBoolean(GL_DEPTH_WRITEMASK)&&Math.abs(glGetDouble(GL_DEPTH_CLEAR_VALUE)-.625)<.001,"exception restores GL state");
            RegressionTest.check(glGetError()==GL_NO_ERROR,"no OpenGL errors");
            System.out.println("PASS: real OpenGL background-depth reproduction, isolated preview, self-occlusion, scissor and state restoration ("+glGetString(GL_RENDERER)+")");
        } finally {if(window!=0)glfwDestroyWindow(window);glfwTerminate();}
    }
    static void reset(){glDisable(GL_SCISSOR_TEST);glDepthMask(true);glClearDepth(1);glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);}
    static void quad(float left,float right,float z,float r,float g,float b){glColor3f(r,g,b);glBegin(GL_QUADS);glVertex3f(left,-.8f,z);glVertex3f(right,-.8f,z);glVertex3f(right,.8f,z);glVertex3f(left,.8f,z);glEnd();}
    static int[] pixel(int x,int y){ByteBuffer out=ByteBuffer.allocateDirect(4);glReadPixels(x,y,1,1,GL_RGBA,GL_UNSIGNED_BYTE,out);return new int[]{out.get(0)&255,out.get(1)&255,out.get(2)&255};}
    static float depth(int x,int y){FloatBuffer out=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();glReadPixels(x,y,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,out);return out.get(0);}
}
