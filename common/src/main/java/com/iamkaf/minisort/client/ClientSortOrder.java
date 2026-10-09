package com.iamkaf.minisort.client;

import com.iamkaf.minisort.sort.CreativeOrder;
import com.iamkaf.minisort.sort.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Orders the items in an open menu by the player's sort mode, for the server to sort by. */
public final class ClientSortOrder {
    private static @Nullable Map<Item, Integer> creativeRanks;

    private ClientSortOrder() {
    }

    public static List<Item> of(AbstractContainerMenu menu, SortMode mode) {
        Set<Item> items = new LinkedHashSet<>();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        List<Item> order = new ArrayList<>(items);
        switch (mode) {
            case CREATIVE -> {
                Map<Item, Integer> ranks = creativeRanks();
                order.sort(Comparator.comparingInt(item -> ranks.getOrDefault(item, Integer.MAX_VALUE)));
            }
            case REGISTRY_ID -> order.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        return order;
    }

    private static Map<Item, Integer> creativeRanks() {
        Map<Item, Integer> ranks = creativeRanks;
        if (rebuildTabs() || ranks == null) {
            List<Item> shown = new ArrayList<>();
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                    tab.getDisplayItems().forEach(stack -> shown.add(stack.getItem()));
                }
            }
            List<Item> order = CreativeOrder.complete(BuiltInRegistries.ITEM.stream().toList(), shown);
            ranks = new HashMap<>(order.size());
            for (int i = 0; i < order.size(); i++) {
                ranks.put(order.get(i), i);
            }
            creativeRanks = ranks;
        }
        return ranks;
    }

    /**
     * Builds the creative tabs as opening the creative inventory does, including its search refresh, so that screen
     * finds them current. Vanilla only builds them there, so a survival player may never have.
     */
    private static boolean rebuildTabs() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        HolderLookup.Provider lookup = player.level().registryAccess();
        boolean operator = player.canUseGameMasterBlocks() && minecraft.options.operatorItemsTab().get();
        if (!CreativeModeTabs.tryRebuildTabContents(player.connection.enabledFeatures(), operator, lookup)) {
            return false;
        }
        List<ItemStack> search = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        player.connection.searchTrees().updateCreativeTooltips(lookup, search);
        player.connection.searchTrees().updateCreativeTags(search);
        return true;
    }
}
