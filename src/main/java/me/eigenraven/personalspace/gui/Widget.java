package me.eigenraven.personalspace.gui;

import com.google.common.collect.Lists;
import java.awt.*;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class Widget {
    public Rectangle position = new Rectangle(0, 0, 1, 1);
    public boolean visible = true, enabled = true;
    public ArrayList<Widget> children = Lists.newArrayList();
    protected boolean dragged = false;

    public final void addChild(Widget w) {
        if (w != null) {
            children.add(w);
        }
    }

    public final void update() {
        this.updateImpl();
        for (Widget w : children) {
            w.update();
        }
    }

    protected void updateImpl() {}

    public final boolean testPoint(int x, int y) {
        return visible && x >= position.x && y >= position.y && x < position.getMaxX() && y < position.getMaxY();
    }

    public final void draw(int mouseX, int mouseY, float partialTicks) {
        GL11.glTranslatef(position.x, position.y, 0);
        if (visible) {
            drawImpl(mouseX, mouseY, partialTicks);
            for (Widget w : children) {
                w.draw(mouseX - position.x, mouseY - position.y, partialTicks);
            }
        }
        GL11.glTranslatef(-position.x, -position.y, 0);
    }

    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {}

    public final void drawForeground(int mouseX, int mouseY, float partialTicks) {
        GL11.glTranslatef(position.x, position.y, 0);
        if (visible) {
            drawForegroundImpl(mouseX, mouseY, partialTicks);
            for (Widget w : children) {
                w.drawForeground(mouseX - position.x, mouseY - position.y, partialTicks);
            }
        }
        GL11.glTranslatef(-position.x, -position.y, 0);
    }

    protected void drawForegroundImpl(int mouseX, int mouseY, float partialTicks) {}

    public final boolean keyTyped(char character, int key) {
        if (this.keyTypedImpl(character, key)) {
            return true;
        }
        for (Widget w : children) {
            if (w.keyTyped(character, key)) {
                return true;
            }
        }
        return false;
    }

    protected boolean keyTypedImpl(char character, int key) {
        return false;
    }

    public final boolean mouseClicked(int x, int y, int button) {
        if (enabled && this.testPoint(x, y) && this.mouseClickedImpl(x, y, button)) {
            dragged = true;
            return true;
        } else if (enabled && !this.testPoint(x, y)) {
            mouseClickedOutsideImpl(x, y, button);
        }
        for (Widget w : children) {
            if (w.mouseClicked(x - position.x, y - position.y, button)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mouseClickedImpl(int x, int y, int button) {
        return false;
    }

    protected void mouseClickedOutsideImpl(int x, int y, int button) {}

    public final boolean mouseMovedOrUp(int x, int y, int button) {
        dragged = false;
        if (enabled && this.testPoint(x, y) && this.mouseMovedOrUpImpl(x, y, button)) {
            return true;
        }
        for (Widget w : children) {
            if (w.mouseMovedOrUp(x - position.x, y - position.y, button)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mouseMovedOrUpImpl(int x, int y, int button) {
        return false;
    }

    public final boolean mouseClickMove(int x, int y, int lastBtn, long timeDragged) {
        if (((enabled && this.testPoint(x, y)) || dragged) && this.mouseClickMoveImpl(x, y, lastBtn, timeDragged)) {
            return true;
        }
        for (Widget w : children) {
            if (w.mouseClickMove(x - position.x, y - position.y, lastBtn, timeDragged)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mouseClickMoveImpl(int x, int y, int lastBtn, long timeDragged) {
        return false;
    }

    public final void clickSound() {
        Minecraft.getMinecraft()
                .getSoundHandler()
                .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public static void drawGradientRect(
            int xLeft, int yTop, int xRight, int yBottom, int topRGBA, int bottomRGBA, int zLevel) {
        float topA = (float) (topRGBA >> 24 & 255) / 255.0F;
        float topR = (float) (topRGBA >> 16 & 255) / 255.0F;
        float topG = (float) (topRGBA >> 8 & 255) / 255.0F;
        float topB = (float) (topRGBA & 255) / 255.0F;
        float bottomA = (float) (bottomRGBA >> 24 & 255) / 255.0F;
        float bottomR = (float) (bottomRGBA >> 16 & 255) / 255.0F;
        float bottomG = (float) (bottomRGBA >> 8 & 255) / 255.0F;
        float bottomB = (float) (bottomRGBA & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(topR, topG, topB, topA);
        tessellator.addVertex(xRight, yTop, zLevel);
        tessellator.addVertex(xLeft, yTop, zLevel);
        tessellator.setColorRGBA_F(bottomR, bottomG, bottomB, bottomA);
        tessellator.addVertex(xLeft, yBottom, zLevel);
        tessellator.addVertex(xRight, yBottom, zLevel);
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public final void drawTooltip(int x, int y, String message) {
        FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRenderer;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        final String[] msgLines = message.split("\n");
        int zLevel = 300;

        if (msgLines.length > 0) {
            int width = 0;
            int drawX;
            int drawY;

            for (drawX = 0; drawX < msgLines.length; ++drawX) {
                drawY = fontRendererObj.getStringWidth(msgLines[drawX]);

                if (drawY > width) {
                    width = drawY;
                }
            }

            drawX = x + 12;
            drawY = y - 12;
            int height = 8;

            if (msgLines.length > 1) {
                height += 2 + (msgLines.length - 1) * 10;
            }

            final int color1 = 0xf0100010;
            drawGradientRect(drawX - 3, drawY - 4, drawX + width + 3, drawY - 3, color1, color1, zLevel);
            drawGradientRect(
                    drawX - 3, drawY + height + 3, drawX + width + 3, drawY + height + 4, color1, color1, zLevel);
            drawGradientRect(drawX - 3, drawY - 3, drawX + width + 3, drawY + height + 3, color1, color1, zLevel);
            drawGradientRect(drawX - 4, drawY - 3, drawX - 3, drawY + height + 3, color1, color1, zLevel);
            drawGradientRect(
                    drawX + width + 3, drawY - 3, drawX + width + 4, drawY + height + 3, color1, color1, zLevel);
            final int color2 = 0x505000ff;
            final int color3 = (color2 & 0xfefefe) >> 1 | color2 & 0xff000000;
            drawGradientRect(drawX - 3, drawY - 3 + 1, drawX - 3 + 1, drawY + height + 3 - 1, color2, color3, zLevel);
            drawGradientRect(
                    drawX + width + 2,
                    drawY - 3 + 1,
                    drawX + width + 3,
                    drawY + height + 3 - 1,
                    color2,
                    color3,
                    zLevel);
            drawGradientRect(drawX - 3, drawY - 3, drawX + width + 3, drawY - 3 + 1, color2, color2, zLevel);
            drawGradientRect(
                    drawX - 3, drawY + height + 2, drawX + width + 3, drawY + height + 3, color3, color3, zLevel);

            for (int i = 0; i < msgLines.length; ++i) {
                String var14 = msgLines[i];

                if (i == 0) {
                    var14 = '\u00a7' + Integer.toHexString(15) + var14;
                } else {
                    var14 = "\u00a77" + var14;
                }

                fontRendererObj.drawStringWithShadow(var14, drawX, drawY, -1);

                if (i == 0) {
                    drawY += 2;
                }

                drawY += 10;
            }
        }
        GL11.glPopAttrib();
    }
}
