package me.eigenraven.personalspace.gui;

import java.awt.*;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class WButton extends Widget {

    @Nonnull
    String text = "";

    public boolean dropShadow = true;
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public int color = DEFAULT_COLOR;
    public Icons buttonIcon = null;
    public ItemStack itemStack = null;
    public String itemStackText = "";
    public Runnable onClick = null;
    public int lastButton = 0;
    public String tooltip = null;

    private static RenderItem renderItem;

    public WButton() {}

    public WButton(Rectangle position, String text, boolean dropShadow, int color, Icons buttonIcon, Runnable onClick) {
        this.position = position;
        this.dropShadow = dropShadow;
        this.color = color;
        this.onClick = onClick;
        this.buttonIcon = buttonIcon;
        setText(text);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        this.text = text;
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        Icons.bindTexture();
        Icons icon = enabled ? Icons.BUTTON_NORMAL : Icons.BUTTON_OFF;
        if (enabled && testPoint(mouseX, mouseY)) {
            icon = Icons.BUTTON_HIGHLIGHT;
        }
        icon.draw9Patch(0, 0, position.width, position.height);
        int textSpace = position.width;
        if (buttonIcon != null) {
            textSpace -= buttonIcon.w + 6;
            buttonIcon.drawAt(1, position.height / 2 - buttonIcon.h / 2);
        }
        if (itemStack != null || (itemStackText != null && !itemStackText.isEmpty())) {
            if (renderItem == null) {
                renderItem = new RenderItem();
            }
            textSpace -= 18;
            Icons.drawItem(1, position.height / 2 - 9, itemStack, itemStackText, renderItem);
        }
        if (!text.isEmpty()) {
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            String drawText = fr.trimStringToWidth(text, textSpace - 6);
            int textW = fr.getStringWidth(drawText);
            fr.drawString(
                    drawText,
                    position.width - textSpace / 2 - textW / 2,
                    position.height / 2 - fr.FONT_HEIGHT / 2,
                    color,
                    dropShadow);
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    @Override
    protected void drawForegroundImpl(int mouseX, int mouseY, float partialTicks) {
        if (tooltip != null && !tooltip.isEmpty() && this.testPoint(mouseX, mouseY)) {
            this.drawTooltip(mouseX - position.x, mouseY - position.y, tooltip);
        }
    }

    @Override
    protected boolean mouseClickedImpl(int x, int y, int button) {
        lastButton = button;
        if (onClick != null) {
            onClick.run();
        }
        clickSound();
        return true;
    }
}
