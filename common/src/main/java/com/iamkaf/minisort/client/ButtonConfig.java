package com.iamkaf.minisort.client;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.minisort.MiniSort;

public final class ButtonConfig {
    public static final ConfigHandle HANDLE;
    public static final ConfigValue<Integer> SORT_X;
    public static final ConfigValue<Integer> SORT_Y;
    public static final ConfigValue<Integer> DEPOSIT_X;
    public static final ConfigValue<Integer> DEPOSIT_Y;
    public static final ConfigValue<Integer> RETRIEVE_X;
    public static final ConfigValue<Integer> RETRIEVE_Y;

    static {
        ConfigBuilder builder = Konfig.builder(MiniSort.MOD_ID, "client")
                .scope(ConfigScope.CLIENT)
                .syncMode(SyncMode.NONE)
                .comment("Client-side positions for MiniSort container buttons.");

        builder.push("buttons");
        builder.categoryComment("Positions are measured from the top-left corner of the container screen.");
        SORT_X = position(builder, "sort_x", 178, "Horizontal position of the sort button.");
        SORT_Y = position(builder, "sort_y", 4, "Vertical position of the sort button.");
        DEPOSIT_X = position(builder, "deposit_x", 178, "Horizontal position of the deposit matching button.");
        DEPOSIT_Y = position(builder, "deposit_y", 24, "Vertical position of the deposit matching button.");
        RETRIEVE_X = position(builder, "retrieve_x", 178, "Horizontal position of the retrieve matching button.");
        RETRIEVE_Y = position(builder, "retrieve_y", 44, "Vertical position of the retrieve matching button.");
        builder.pop();

        HANDLE = builder.build();
    }

    private ButtonConfig() {
    }

    public static void init() {
    }

    private static ConfigValue<Integer> position(ConfigBuilder builder, String key, int defaultValue, String comment) {
        return builder.intRange(key, defaultValue, -4096, 4096)
                .comment(comment)
                .clientOnly()
                .build();
    }
}
