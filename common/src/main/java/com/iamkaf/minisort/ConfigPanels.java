package com.iamkaf.minisort;

import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.InfoPanelBuilder;

/** Info panels for the config screens: a header, an optional picture, and a short explanation. */
public final class ConfigPanels {
    private static final ImageOptions PICTURE = ImageOptions.builder()
            .size(256, 144)
            .padding(0)
            .align(ImageOptions.Align.CENTER)
            .captionPosition(ImageOptions.CaptionPosition.NONE)
            .build();

    private ConfigPanels() {
    }

    /** Fills a panel from {@code minisort.config.<name>} and {@code minisort.config.<name>.info}. */
    public static void picture(InfoPanelBuilder info, String name, String picture) {
        info.headerKey(key(name))
                .image(MiniSort.resource("gui/config/" + picture), PICTURE)
                .inlineTextKey(key(name + ".info"));
    }

    /** The first panel a screen shows: a picture, what the screen covers, and how to learn more. */
    public static void screen(InfoPanelBuilder info, String name, String picture) {
        picture(info, name, picture);
        info.inlineTextKey(key("hint"));
    }

    public static String key(String path) {
        return MiniSort.MOD_ID + ".config." + path;
    }
}
