package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;

public class WToggleButton extends WButton {

    protected boolean value = true;

    public Icons yesIcon = Icons.CHECKMARK;
    public Icons noIcon = Icons.CROSS;

    public WToggleButton(Rectangle position, String text, boolean dropShadow, int color, boolean value,
            Runnable onClick) {
        super(position, text, dropShadow, color, Icons.CHECKMARK, onClick);
        setValue(value);
    }

    public void setValue(boolean value) {
        this.value = value;
        this.buttonIcon = value ? yesIcon : noIcon;
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
