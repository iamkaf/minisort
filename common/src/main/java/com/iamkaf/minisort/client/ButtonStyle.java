package com.iamkaf.minisort.client;

import java.util.Locale;

/** The look of Minisort's buttons. Each player picks one in the client config. */
public enum ButtonStyle {
    OAK,
    SPRUCE,
    BIRCH,
    DARK_OAK,
    CHERRY,
    BAMBOO,
    CRIMSON,
    WARPED,
    STONE;

    /** The style's sprite folder under {@code textures/gui/sprites/button}, and its key in lang and pictures. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
