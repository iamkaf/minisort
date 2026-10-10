package com.iamkaf.minisort.sort;

import com.iamkaf.amber.api.platform.v1.Platform;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
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

    // Mochila before 6.2.1 left the open backpack's slot unlocked, so sorting the inventory or depositing everything
    // while a backpack was open could duplicate items or put the backpack inside itself. Fabric and NeoForge refuse
    // those versions at launch. Forge has no incompatibility metadata, so the server refuses Minisort's actions in
    // their backpack menus instead; the client only sees a vanilla chest menu there, so its buttons still show.
    private static final boolean UNSAFE_MOCHILA = ModVersions.olderThan(Platform.getModVersion("mochila"), 6, 2, 1);

    private SortMenuPolicy() {
    }

    /** Whether the server must not touch anything while this menu is open, not even the player's inventory. */
    public static boolean refuses(AbstractContainerMenu menu) {
        return UNSAFE_MOCHILA && menu.getClass().getName().startsWith("com.iamkaf.mochila.");
    }

    public static boolean supportsStorageActions(AbstractContainerMenu menu) {
        if (refuses(menu)) {
            return false;
        }
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
     * A modded menu is storage when it has enough plain storage slots beside the player's inventory. Special slots,
     * like Echo Chest's bottle slot or an upgrade slot, may sit beside them; Minisort leaves those alone. A crafting,
     * smelting, or trading result slot marks a machine, and virtual network slots aren't plain, so those stay out.
     */
    private static boolean isModdedStorage(AbstractContainerMenu menu) {
        int storage = 0;
        boolean playerInventory = false;
        for (Slot slot : menu.slots) {
            if (slot instanceof ResultSlot || slot instanceof FurnaceResultSlot || slot instanceof MerchantResultSlot) {
                return false;
            }
            if (slot.container instanceof Inventory) {
                playerInventory = true;
            } else if (isStorageSlot(slot)) {
                storage++;
            }
        }
        return playerInventory && storage >= MIN_MODDED_STORAGE_SLOTS;
    }

    /** A slot that only holds items, with no rules of its own. The only container slots Minisort moves items in. */
    public static boolean isStorageSlot(Slot slot) {
        Class<?> slotClass = slot.getClass();
        return slotClass == Slot.class || slotClass == ShulkerBoxSlot.class
                || LOADER_STORAGE_SLOTS.contains(slotClass.getName());
    }
}
