package com.iamkaf.minisort.client;

import com.iamkaf.minisort.MiniSort;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
//? if >=1.21.9 {
import net.minecraft.client.input.InputWithModifiers;
//?} else {
/*import net.minecraft.client.gui.screens.Screen;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.renderer.RenderPipelines;
//?}
import net.minecraft.network.chat.Component;

/**
 * A small Minisort button. Its icon plays a short animation once when the pointer arrives, then rests. The action
 * learns whether Shift was held for the click or key press that pressed it.
 */
public final class ShiftClickButton extends ImageButton {
    public static final int SIZE = 12;
    // Sprites are button/<style>/<glyph> at rest and button/<style>/<glyph>_hover_<n> while hovered; hover frame 0
    // is the hovered rest.
    private static final int HOVER_FRAMES = 4;
    private static final long FRAME_MILLIS = 70;

    private final String glyph;
    private final Action action;
    private long hoverStart = -1;

    public ShiftClickButton(int x, int y, String glyph, Action action, Component message) {
        // ImageButton wants sprites, but drawing below picks them from the player's style each frame.
        super(x, y, SIZE, SIZE, new WidgetSprites(MiniSort.resource("button/oak/" + glyph),
                MiniSort.resource("button/oak/" + glyph + "_hover_0")), ignored -> {
        }, message);
        this.glyph = glyph;
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

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MiniSort.resource(sprite()), getX(), getY(), width, height);
    }
    //?} else if >=1.21.11 {
    /*@Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MiniSort.resource(sprite()), getX(), getY(), width, height);
    }
    *///?} else {
    /*@Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(MiniSort.resource(sprite()), getX(), getY(), width, height);
    }
    *///?}

    /** Hovering or focusing the button starts its animation; it plays once, then holds the hovered rest frame. */
    private String sprite() {
        String base = "button/" + ClientConfig.buttonStyle().id() + "/" + glyph;
        if (!isHoveredOrFocused()) {
            hoverStart = -1;
            return base;
        }
        long now = System.nanoTime() / 1_000_000L;
        if (hoverStart < 0) {
            hoverStart = now;
        }
        long frame = 1 + (now - hoverStart) / FRAME_MILLIS;
        return base + "_hover_" + (frame < HOVER_FRAMES ? frame : 0);
    }

    @FunctionalInterface
    public interface Action {
        void press(boolean shiftDown);
    }
}
