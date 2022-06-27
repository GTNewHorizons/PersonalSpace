package xyz.kubasz.personalspace.gui;

import java.awt.*;

public class WToggleButton extends WButton {
    protected boolean value = true;

    public WToggleButton(
            Rectangle position, String text, boolean dropShadow, int color, boolean value, Runnable onClick) {
        super(position, text, dropShadow, color, Icons.CHECKMARK, onClick);
        setValue(value);
    }

    public void setValue(boolean value) {
        this.value = value;
        this.buttonIcon = value ? Icons.CHECKMARK : Icons.CROSS;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    protected boolean mouseClickedImpl(int x, int y, int button) {
        setValue(!getValue());
        return super.mouseClickedImpl(x, y, button);
    }
}
