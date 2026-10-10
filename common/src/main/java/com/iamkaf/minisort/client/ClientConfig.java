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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;

public final class ClientConfig {
    public static final ConfigHandle HANDLE;
    private static final ConfigValue<String> SORT_MODE;
    private static final ConfigValue<String> BUTTON_STYLE;
    private static final ConfigValue<List<String>> HIDDEN_MENUS;
    private static final ConfigValue<Boolean> ITEM_ANIMATION;

    static {
        ConfigBuilder builder = Konfig.builder(MiniSort.MOD_ID, "client")
                .scope(ConfigScope.CLIENT)
                .syncMode(SyncMode.NONE)
                .comment("Client-side settings for Minisort container buttons.")
                .info(info -> ConfigPanels.screen(info, "client", "showcase"));

        builder.push("sorting");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "sorting", "sort_creative"));
        // Option values are SortMode names, so sortMode() can read them back directly.
        SORT_MODE = builder.dropdown("sort_mode", SortMode.CREATIVE.name(), options -> options
                        .option(SortMode.CREATIVE.name(), option -> option
                                .labelKey(ConfigPanels.key("sort_mode.creative"))
                                .info(info -> ConfigPanels.picture(info, "sort_mode.creative", "sort_creative")))
                        .option(SortMode.REGISTRY_ID.name(), option -> option
                                .labelKey(ConfigPanels.key("sort_mode.registry_id"))
                                .info(info -> ConfigPanels.picture(info, "sort_mode.registry_id", "sort_registry_id"))))
                .comment("How the sort button orders a container: CREATIVE (the creative inventory's order) or REGISTRY_ID.")
                .info(info -> ConfigPanels.picture(info, "sort_mode", "sort_creative"))
                .clientOnly()
                .build();
        ITEM_ANIMATION = builder.bool("item_animation", true)
                .comment("Whether items glide to their new slots when Minisort sorts, deposits, or retrieves them.")
                .info(info -> ConfigPanels.picture(info, "item_animation", "item_animation"))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("buttons");
        builder.categoryInfo(info -> ConfigPanels.picture(info, "buttons", "buttons"));
        // Option values are ButtonStyle names, so buttonStyle() can read them back directly.
        BUTTON_STYLE = builder.dropdown("button_style", ButtonStyle.OAK.name(), options -> {
                    for (ButtonStyle style : ButtonStyle.values()) {
                        options.option(style.name(), option -> option
                                .labelKey(ConfigPanels.key("button_style." + style.id()))
                                .info(info -> ConfigPanels.picture(info, "button_style." + style.id(), "button_style_" + style.id())));
                    }
                })
                .comment("The look of the buttons: OAK, SPRUCE, BIRCH, DARK_OAK, CHERRY, BAMBOO, CRIMSON, WARPED, or STONE.")
                .info(info -> ConfigPanels.picture(info, "button_style", "button_styles"))
                .clientOnly()
                .build();
        HIDDEN_MENUS = builder.stringList("hidden_menus", List.of())
                .comment("Menu IDs where Minisort stays out: no buttons, middle-click, or sort key. For example, minecraft:generic_9x3.")
                .info(info -> ConfigPanels.picture(info, "buttons.hidden_menus", "hidden_menus"))
                .clientOnly()
                .build();
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

    public static ButtonStyle buttonStyle() {
        return ButtonStyle.valueOf(BUTTON_STYLE.get());
    }

    public static boolean itemAnimation() {
        return ITEM_ANIMATION.get();
    }

    /** Whether the player turned Minisort off for this kind of menu. */
    public static boolean hidden(AbstractContainerMenu menu) {
        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (UnsupportedOperationException noType) {
            // Menus without a registered type, like the player's own, can't be listed.
            return false;
        }
        var id = BuiltInRegistries.MENU.getKey(type);
        return id != null && HIDDEN_MENUS.get().contains(id.toString());
    }
}
