package com.iamkaf.minisort.client;

import com.iamkaf.amber.api.registry.v1.KeybindHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class MiniSortClient {
    /** Sorts like middle-click, for players without a middle mouse button or on a controller. */
    public static final KeyMapping SORT_KEY = new KeyMapping("key.minisort.sort", InputConstants.KEY_R,
            //? if >=1.21.9 {
            KeyMapping.Category.INVENTORY
            //?} else {
            /*KeyMapping.CATEGORY_INVENTORY
            *///?}
    );

    private MiniSortClient() {
    }

    public static void init() {
        ClientConfig.init();
        KeybindHelper.register(SORT_KEY);
    }
}
