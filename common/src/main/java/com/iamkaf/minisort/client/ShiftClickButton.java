package com.iamkaf.minisort.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
//? if >=1.21.9 {
import net.minecraft.client.input.InputWithModifiers;
//?} else {
/*import net.minecraft.client.gui.screens.Screen;
*///?}
import net.minecraft.network.chat.Component;

/** An image button that tells its action whether Shift was held for the click or key press that pressed it. */
public final class ShiftClickButton extends ImageButton {
    private final Action action;

    public ShiftClickButton(int x, int y, WidgetSprites sprites, Action action, Component message) {
        super(x, y, 18, 18, sprites, ignored -> {
        }, message);
        this.action = action;
    }

    //? if >=1.21.9 {
    @Override
    public void onPress(InputWithModifiers input) {
        action.press(input.hasShiftDown());
    }
    //?} else {
    /*@Override
    public void onPress() {
        action.press(Screen.hasShiftDown());
    }
    *///?}

    @FunctionalInterface
    public interface Action {
        void press(boolean shiftDown);
    }
}
