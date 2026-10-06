package com.iamkaf.minisort.client;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.minisort.ConfigPanels;
import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.sort.SortMode;

public final class ClientConfig {
    public static final ConfigHandle HANDLE;
    public static final ConfigValue<Integer> SORT_X;
    public static final ConfigValue<Integer> SORT_Y;
    public static final ConfigValue<Integer> DEPOSIT_X;
    public static final ConfigValue<Integer> DEPOSIT_Y;
    public static final ConfigValue<Integer> RETRIEVE_X;
    public static final ConfigValue<Integer> RETRIEVE_Y;
    private static final ConfigValue<String> SORT_MODE;

    static {
        ConfigBuilder builder = Konfig.builder(MiniSort.MOD_ID, "client")
                .scope(ConfigScope.CLIENT)
                .syncMode(SyncMode.NONE)
                .comment("Client-side settings for Minisort container buttons.")
                .info(info -> ConfigPanels.picture(info, "client", "buttons"));

        builder.push("sorting");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "sorting", "sort_categories"));
        // Option values are SortMode names, so sortMode() can read them back directly.
        SORT_MODE = builder.dropdown("sort_mode", SortMode.REGISTRY_ID.name(), options -> options
                        .option(SortMode.REGISTRY_ID.name(), option -> option
                                .labelKey(ConfigPanels.key("sort_mode.registry_id"))
                                .info(info -> ConfigPanels.picture(info, "sort_mode.registry_id", "sort_registry_id")))
                        .option(SortMode.CATEGORIES.name(), option -> option
                                .labelKey(ConfigPanels.key("sort_mode.categories"))
                                .info(info -> ConfigPanels.picture(info, "sort_mode.categories", "sort_categories"))))
                .comment("How the sort button orders a container: REGISTRY_ID or CATEGORIES (experimental).")
                .info(info -> ConfigPanels.picture(info, "sort_mode", "sort_categories"))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("buttons");
        builder.categoryComment("Positions are measured from the top-left corner of the container screen.");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "buttons", "buttons"));
        SORT_X = position(builder, "sort_x", 178, "Horizontal position of the sort button.");
        SORT_Y = position(builder, "sort_y", 4, "Vertical position of the sort button.");
        DEPOSIT_X = position(builder, "deposit_x", 178, "Horizontal position of the deposit matching button.");
        DEPOSIT_Y = position(builder, "deposit_y", 24, "Vertical position of the deposit matching button.");
        RETRIEVE_X = position(builder, "retrieve_x", 178, "Horizontal position of the retrieve matching button.");
        RETRIEVE_Y = position(builder, "retrieve_y", 44, "Vertical position of the retrieve matching button.");
        builder.pop();

        HANDLE = builder.build();
    }

    private ClientConfig() {
    }

    public static void init() {
    }

    public static SortMode sortMode() {
        return SortMode.valueOf(SORT_MODE.get());
    }

    private static ConfigValue<Integer> position(ConfigBuilder builder, String key, int defaultValue, String comment) {
        return builder.intRange(key, defaultValue, -4096, 4096)
                .comment(comment)
                .info(info -> ConfigPanels.text(info, "buttons.position"))
                .clientOnly()
                .build();
    }
}
