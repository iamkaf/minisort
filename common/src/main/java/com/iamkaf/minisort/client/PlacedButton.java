package com.iamkaf.minisort.client;

import net.minecraft.client.gui.components.AbstractButton;

/** A Minisort button, its offset from the container screen's top-left corner, and when it shows. */
public record PlacedButton(AbstractButton button, int x, int y, Shown shown) {
    /** Deposit and Retrieve have a twin in the same spot that does the "everything" version while Shift is held. */
    public enum Shown {
        ALWAYS,
        WITHOUT_SHIFT,
        WITH_SHIFT;

        public boolean visible(boolean shiftDown) {
            return switch (this) {
                case ALWAYS -> true;
                case WITHOUT_SHIFT -> !shiftDown;
                case WITH_SHIFT -> shiftDown;
            };
        }
    }
}
