package me.eigenraven.personalspace.gui;

import java.awt.*;
import java.util.function.DoubleConsumer;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;

public class WSlider extends Widget {

    @Nonnull
    String text = "";

    public boolean dropShadow = true;
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public int color = DEFAULT_COLOR;
    public Icons buttonIcon = null;
    public double minValue = 0;
    public double maxValue = 1;
    public double rawValue = 0.5;
    public double step = 0.1;
    public DoubleConsumer onChange = null;

    public WSlider() {}

    public WSlider(Rectangle position, String text, double min, double max, double val, double step, boolean dropShadow,
            int color, Icons buttonIcon, DoubleConsumer onChange) {
        this.position = position;
        this.dropShadow = dropShadow;
        this.color = color;
        this.onChange = onChange;
        this.buttonIcon = buttonIcon;
        this.minValue = min;
        this.maxValue = max;
        this.rawValue = val;
        this.step = step;
        setText(text);
    }

    public double getValue() {
        return (step > 1.0e-6) ? (Math.round(rawValue / step) * step) : rawValue;
    }

    public int getValueInt() {
        return (int) (getValue());
    }

    public double value01() {
        return MathHelper.clamp_double((rawValue - minValue) / Math.max(maxValue - minValue, 1.0e-6), 0, 1);
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
        Icons.BUTTON_OFF.draw9Patch(0, 0, position.width, position.height);
        icon.draw9Patch((int) (value01() * (position.width - 15)), 0, 15, position.height);
        int textSpace = position.width - 6;
        if (buttonIcon != null) {
            textSpace -= buttonIcon.w;
            buttonIcon.drawAt(4, position.height / 2 - buttonIcon.h / 2 - 1);
        }
        String text = this.text;
        if (!text.isEmpty()) {
            if (text.contains("%")) {
                text = String.format(text, rawValue);
            }
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
    protected boolean mouseClickedImpl(int x, int y, int button) {
        clickSound();
        return mouseMovedOrUpImpl(x, y, button);
    }

    @Override
    protected boolean mouseMovedOrUpImpl(int x, int y, int button) {
        double newVal01 = MathHelper.clamp_double((double) (x - position.x - 3) / (double) (position.width - 6), 0, 1);
        double newVal = minValue + (newVal01 * (maxValue - minValue));
        double oldVal = rawValue;
        rawValue = newVal;
        rawValue = getValue();
        if (Math.abs(newVal - oldVal) > 1.0e-6 && onChange != null) {
            onChange.accept(rawValue);
        }
        return true;
    }

    @Override
    protected boolean mouseClickMoveImpl(int x, int y, int lastBtn, long timeDragged) {
        return dragged ? mouseMovedOrUpImpl(x, y, lastBtn) : false;
    }
}
