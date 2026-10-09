package com.iamkaf.minisort.sort;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;

import java.util.Set;

/** Decides which menus are storage that Minisort may sort, deposit into, and retrieve from. Both sides ask it. */
public final class SortMenuPolicy {
    // Below this, a modded menu is more likely a machine than storage.
    private static final int MIN_MODDED_STORAGE_SLOTS = 9;
    // The loaders' item handler slots hold plain storage. Named, not referenced, so common code needs no loader.
    private static final Set<String> LOADER_STORAGE_SLOTS = Set.of(
            "net.neoforged.neoforge.items.SlotItemHandler",
            "net.neoforged.neoforge.transfer.item.ResourceHandlerSlot",
            "net.minecraftforge.items.SlotItemHandler"
    );

    private SortMenuPolicy() {
    }

    public static boolean supportsStorageActions(AbstractContainerMenu menu) {
        Class<?> menuClass = menu.getClass();
        if (menuClass == ChestMenu.class
                || menuClass == ShulkerBoxMenu.class
                || menuClass == DispenserMenu.class
                || menuClass == HopperMenu.class) {
            return true;
        }
        // Every other vanilla menu crafts, smelts, trades, or equips.
        if (menuClass.getName().startsWith("net.minecraft.")) {
            return false;
        }
        return isModdedStorage(menu);
    }

    /**
     * A modded menu is storage when every slot outside the player's inventory is a plain storage slot. Machines,
     * crafting grids, and virtual network slots use slot subclasses, so they stay out.
     */
    private static boolean isModdedStorage(AbstractContainerMenu menu) {
        int storage = 0;
        boolean playerInventory = false;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                playerInventory = true;
            } else if (isPlainStorageSlot(slot)) {
                storage++;
            } else {
                return false;
            }
        }
        return playerInventory && storage >= MIN_MODDED_STORAGE_SLOTS;
    }

    private static boolean isPlainStorageSlot(Slot slot) {
        Class<?> slotClass = slot.getClass();
        return slotClass == Slot.class || slotClass == ShulkerBoxSlot.class
                || LOADER_STORAGE_SLOTS.contains(slotClass.getName());
    }
}
