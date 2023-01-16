package me.eigenraven.personalspace.gui;

import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

public class WLabel extends Widget {
    @Nonnull
    String text = "";

    public boolean dropShadow = true;
    public int color = 0x222222;

    public WLabel(int x, int y, String text, boolean dropShadow) {
        setText(text);
        this.dropShadow = dropShadow;
        this.position.x = x;
        this.position.y = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        this.text = text;
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        this.position.width = fr.getStringWidth(text);
        this.position.height = fr.FONT_HEIGHT;
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        if (!text.isEmpty()) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            fr.drawString(this.text, 0, 0, color, dropShadow);
            GL11.glColor4f(1, 1, 1, 1);
        }
    }
}
