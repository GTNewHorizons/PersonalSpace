package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.List;

import me.eigenraven.personalspace.world.DimensionConfig;

public class WCycleButton extends WButton {

    final List<ButtonState> stateList;
    int currentIndex = 0;
    int length = 0;

    public WCycleButton(Rectangle position, String text, boolean dropShadow, int color, List<ButtonState> states,
            int initialIndex, Runnable onClick) {
        super(position, text, dropShadow, color, Icons.CHECKMARK, onClick);
        stateList = states;
        length = stateList.size();
        currentIndex = initialIndex;
        this.buttonIcon = stateList.get(currentIndex).icon;
    }

    public DimensionConfig.DaylightCycle getState() {
        return stateList.get(currentIndex).cycle;
    }

    void next() {
        if (++currentIndex >= length) {
            currentIndex = 0;
        }
        this.buttonIcon = stateList.get(currentIndex).icon;
    }

    @Override
    protected boolean mouseClickedImpl(int x, int y, int button) {
        next();
        return super.mouseClickedImpl(x, y, button);
    }

    public static class ButtonState {

        public final DimensionConfig.DaylightCycle cycle;
        public final Icons icon;

        ButtonState(DimensionConfig.DaylightCycle cycle, Icons icon) {
            this.cycle = cycle;
            this.icon = icon;
        }
    }
}
