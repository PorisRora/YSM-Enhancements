package net.minecraft;

/** Vanilla key-state stand-in; modifierAllowed models the provider's isDown filter. */
public class class_304 {
    public boolean pressed, modifierAllowed = true, mouse;
    public int key = 76, scan = -1;
    public class_304(String name,class_3675.class_307 type,int key,String category) { this.key=key; }
    public void method_23481(boolean down) { pressed=down; }
    public boolean method_1434() { return pressed && modifierAllowed; }
    public boolean method_1417(int code,int scancode) { return !mouse && (scan>=0 ? scan==scancode : key==code); }
    public boolean method_1433(int button) { return mouse && key==button; }
    public int method_1430(class_304 other) { return 0; }
}
