package com.iamkaf.minisort.refill;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.minisort.ConfigPanels;
import com.iamkaf.minisort.MiniSort;

public final class RefillConfig {
    public static final ConfigHandle HANDLE;
    public static final ConfigValue<Boolean> REFILL_BLOCKS;
    public static final ConfigValue<Boolean> REFILL_TOOLS;
    public static final ConfigValue<Boolean> REFILL_CONSUMABLES;
    public static final ConfigValue<Boolean> REFILL_GENERIC_USE_ITEMS;
    public static final ConfigValue<Boolean> SEARCH_HOTBAR_FIRST;

    static {
        ConfigBuilder builder = Konfig.builder(MiniSort.MOD_ID, "common")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN)
                .comment("Server-authoritative refill settings.")
                .info(info -> ConfigPanels.screen(info, "common", "refill_blocks"));

        builder.push("refill");
        builder.categoryComment("Choose which emptied held-item categories can be refilled.");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "refill", "refill_blocks"));
        REFILL_BLOCKS = builder.bool("refill_blocks", true)
                .comment("Refill blocks after the held stack is placed.")
                .info(info -> ConfigPanels.picture(info, "refill_blocks", "refill_blocks"))
                .sync(true)
                .build();
        REFILL_TOOLS = builder.bool("refill_tools", true)
                .comment("Refill a broken held tool with a component-compatible copy.")
                .info(info -> ConfigPanels.picture(info, "refill_tools", "refill_tools"))
                .sync(true)
                .build();
        REFILL_CONSUMABLES = builder.bool("refill_consumables", true)
                .comment("Refill food and other items after a held-use action consumes the stack.")
                .info(info -> ConfigPanels.picture(info, "refill_consumables", "refill_consumables"))
                .sync(true)
                .build();
        REFILL_GENERIC_USE_ITEMS = builder.bool("refill_generic_use_items", true)
                .comment("Refill other right-click items, such as ender pearls, bone meal, and spawn eggs.")
                .info(info -> ConfigPanels.picture(info, "refill_generic_use_items", "refill_generic_use_items"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("inventory");
        builder.categoryComment("Control where replacement stacks are found.");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "inventory", "search_hotbar_first"));
        SEARCH_HOTBAR_FIRST = builder.bool("search_hotbar_first", true)
                .comment("Search other hotbar slots before the main inventory.")
                .info(info -> ConfigPanels.picture(info, "search_hotbar_first", "search_hotbar_first"))
                .sync(true)
                .build();
        builder.pop();

        HANDLE = builder.build();
    }

    private RefillConfig() {
    }

    public static void init() {
    }
}
