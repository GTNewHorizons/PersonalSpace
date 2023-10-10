package me.eigenraven.personalspace.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public enum Icons {

    // 9-patch rectangles
    GUI_BG(0, 0, 24, 24),
    BUTTON_NORMAL(24, 0, 15, 15),
    BUTTON_HIGHLIGHT(40, 0, 15, 15),
    BUTTON_OFF(56, 0, 15, 15),
    BUTTON_PRESSED_NORMAL(72, 0, 15, 15),
    BUTTON_PRESSED_HIGHLIGHT(88, 0, 15, 15),
    // large icons (16x16)
    SLOT(0, 24, 17, 17),
    CHECKMARK(32, 16, 16, 16),
    CROSS(48, 16, 16, 16),
    LOCK(64, 16, 16, 16),
    PLUS(80, 16, 16, 16),
    MINUS(96, 16, 16, 16),
    // small icons (8x8)
    SMALL_CROSS(112, 16, 8, 8),
    SMALL_UP(120, 16, 8, 8),
    SMALL_DOWN(128, 16, 8, 8),
    SMALL_X(136, 16, 8, 8),
    // more large icons (16x16)
    STAR(32, 32, 16, 16),
    STAR_BG(48, 32, 16, 16),
    PENCIL(64, 32, 16, 16),
    MOON(80, 32, 16, 16),
    SUN(96, 32, 16, 16),
    SUN_MOON(112, 32, 16, 16),
    ALL(0, 0, 256, 256);

    public static final double TEXTURE_DIM = 256;
    public static final String TEXTURE_PATH = "personalspace:textures/widgets.png";
    public static final ResourceLocation TEXTURE_LOC = new ResourceLocation(TEXTURE_PATH);

    public final int x, y, w, h;
    public final double u0, v0, u1, v1;

    Icons(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        u0 = x / TEXTURE_DIM;
        v0 = y / TEXTURE_DIM;
        u1 = (x + w) / TEXTURE_DIM;
        v1 = (y + h) / TEXTURE_DIM;
    }

    public static void bindTexture() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(Icons.TEXTURE_LOC);
    }

    private static void tSquare(double x, double y, double w, double h, double u0, double v0, double u1, double v1) {
        Tessellator t = Tessellator.instance;
        t.addVertexWithUV(x, y, 0, u0, v0);
        t.addVertexWithUV(x, y + h, 0, u0, v1);
        t.addVertexWithUV(x + w, y + h, 0, u1, v1);
        t.addVertexWithUV(x + w, y, 0, u1, v0);
    }

    public void drawAt(int xOff, int yOff) {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        tSquare(xOff, yOff, w, h, u0, v0, u1, v1);
        t.draw();
    }

    private static double B(double a, double b, double x) {
        return x * (b - a) + a;
    }

    public void draw9Patch(int xOff, int yOff, int w9, int h9) {
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        double w = w9, h = h9;
        double w3 = this.w / 3D;
        double h3 = this.h / 3D;
        // top
        tSquare(xOff, yOff, w3, h3, B(u0, u1, 0 / 3D), B(v0, v1, 0 / 3D), B(u0, u1, 1 / 3D), B(v0, v1, 1 / 3D));
        tSquare(
                xOff + w3,
                yOff,
                w - 2D * w3,
                h3,
                B(u0, u1, 1 / 3D),
                B(v0, v1, 0 / 3D),
                B(u0, u1, 2 / 3D),
                B(v0, v1, 1 / 3D));
        tSquare(
                xOff + w - w3,
                yOff,
                w3,
                h3,
                B(u0, u1, 2 / 3D),
                B(v0, v1, 0 / 3D),
                B(u0, u1, 3 / 3D),
                B(v0, v1, 1 / 3D));
        // middle
        tSquare(
                xOff,
                yOff + h3,
                w3,
                h - 2D * h3,
                B(u0, u1, 0 / 3D),
                B(v0, v1, 1 / 3D),
                B(u0, u1, 1 / 3D),
                B(v0, v1, 2 / 3D));
        tSquare(
                xOff + w3,
                yOff + h3,
                w - 2D * w3,
                h - 2D * h3,
                B(u0, u1, 1 / 3D),
                B(v0, v1, 1 / 3D),
                B(u0, u1, 2 / 3D),
                B(v0, v1, 2 / 3D));
        tSquare(
                xOff + w - w3,
                yOff + h3,
                w3,
                h - 2D * h3,
                B(u0, u1, 2 / 3D),
                B(v0, v1, 1 / 3D),
                B(u0, u1, 3 / 3D),
                B(v0, v1, 2 / 3D));
        // bottom
        tSquare(
                xOff,
                yOff + h - h3,
                w3,
                h3,
                B(u0, u1, 0 / 3D),
                B(v0, v1, 2 / 3D),
                B(u0, u1, 1 / 3D),
                B(v0, v1, 3 / 3D));
        tSquare(
                xOff + w3,
                yOff + h - h3,
                w - 2D * w3,
                h3,
                B(u0, u1, 1 / 3D),
                B(v0, v1, 2 / 3D),
                B(u0, u1, 2 / 3D),
                B(v0, v1, 3 / 3D));
        tSquare(
                xOff + w - w3,
                yOff + h - h3,
                w3,
                h3,
                B(u0, u1, 2 / 3D),
                B(v0, v1, 2 / 3D),
                B(u0, u1, 3 / 3D),
                B(v0, v1, 3 / 3D));
        t.draw();
    }

    private static final ItemStack dummyStack = new ItemStack(Blocks.fire);

    public static void drawItem(int xOff, int yOff, ItemStack is, String text, RenderItem itemRender) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderHelper.enableGUIStandardItemLighting();

        GL11.glTranslatef(1.0f, 1.0f, 0.0f);

        if (is != null && is.getItem() != null && is.stackSize > 0) {
            itemRender.renderItemAndEffectIntoGUI(fr, tm, is, xOff, yOff);
            itemRender.renderItemOverlayIntoGUI(fr, tm, is, xOff, yOff, text);
        } else {
            itemRender.renderItemOverlayIntoGUI(fr, tm, dummyStack, xOff, yOff, text);
        }

        RenderHelper.disableStandardItemLighting();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
