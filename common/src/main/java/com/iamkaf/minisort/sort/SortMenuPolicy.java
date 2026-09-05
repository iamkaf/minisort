package com.iamkaf.minisort.sort;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

public final class SortMenuPolicy {
    private SortMenuPolicy() {
    }

    public static boolean supportsStorageActions(AbstractContainerMenu menu) {
        Class<?> menuClass = menu.getClass();
        return menuClass == ChestMenu.class
                || menuClass == ShulkerBoxMenu.class
                || menuClass == DispenserMenu.class
                || menuClass == HopperMenu.class;
    }
}
