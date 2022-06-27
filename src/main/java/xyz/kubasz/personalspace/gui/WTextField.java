package xyz.kubasz.personalspace.gui;

import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.opengl.GL11;

public class WTextField extends Widget {
    public GuiTextField textField;

    public WTextField(Rectangle position, String text) {
        this.position = position;
        textField = new GuiTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, position.width, position.height);
        textField.setMaxStringLength(4096);
        textField.setText(text);
    }

    @Override
    protected void updateImpl() {
        textField.updateCursorCounter();
        textField.width = position.width;
        textField.height = position.height;
        textField.setEnabled(this.enabled);
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        textField.drawTextBox();
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    protected boolean keyTypedImpl(char character, int key) {
        return textField.textboxKeyTyped(character, key);
    }

    @Override
    protected boolean mouseClickedImpl(int x, int y, int button) {
        textField.mouseClicked(x - position.x, y - position.y, button);
        return true;
    }

    @Override
    protected void mouseClickedOutsideImpl(int x, int y, int button) {
        mouseClickedImpl(x, y, button);
    }
}
