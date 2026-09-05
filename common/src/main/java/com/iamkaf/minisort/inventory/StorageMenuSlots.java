package com.iamkaf.minisort.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StorageMenuSlots(List<IndexedSlot> containerSlots, List<IndexedSlot> playerSlots) {
    public StorageMenuSlots {
        containerSlots = List.copyOf(containerSlots);
        playerSlots = List.copyOf(playerSlots);
    }

    public static Optional<StorageMenuSlots> resolve(ServerPlayer player, AbstractContainerMenu menu) {
        Inventory playerInventory = player.getInventory();
        Container target = null;
        List<IndexedSlot> containerSlots = new ArrayList<>();
        List<IndexedSlot> playerSlots = new ArrayList<>();

        for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
            Slot slot = menu.slots.get(menuIndex);
            if (slot.container == playerInventory) {
                playerSlots.add(new IndexedSlot(menuIndex, slot));
                continue;
            }

            if (target == null) {
                target = slot.container;
            } else if (target != slot.container) {
                return Optional.empty();
            }
            containerSlots.add(new IndexedSlot(menuIndex, slot));
        }

        if (containerSlots.isEmpty() || playerSlots.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new StorageMenuSlots(containerSlots, playerSlots));
    }

    public record IndexedSlot(int menuIndex, Slot slot) {
    }
}
