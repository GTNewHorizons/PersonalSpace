package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;

public class WRectangle extends Widget {

    public Icons icon9 = null;
    public float r = 1, g = 1, b = 1, a = 1;

    WRectangle(Rectangle position, Icons icon9) {
        this.position = position;
        this.icon9 = icon9;
    }

    public void setColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 1;
    }

    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        if (icon9 != null) {
            Icons.bindTexture();
            icon9.draw9Patch(0, 0, position.width, position.height);
        }
    }
}
