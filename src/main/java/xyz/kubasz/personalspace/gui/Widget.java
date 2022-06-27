package xyz.kubasz.personalspace.gui;

import com.google.common.collect.Lists;
import java.awt.*;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

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
}
