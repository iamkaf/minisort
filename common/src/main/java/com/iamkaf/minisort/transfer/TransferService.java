package com.iamkaf.minisort.transfer;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.inventory.StorageMenuSlots;
import com.iamkaf.minisort.inventory.StorageMenuSlots.IndexedSlot;
import com.iamkaf.minisort.network.TransferContainerPayload;
import com.iamkaf.minisort.sort.SortMenuPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TransferService {
    private static final int HOTBAR_END = 9;

    private TransferService() {
    }

    public static TransferResult transfer(ServerPlayer player, TransferContainerPayload payload) {
        if (player.isSpectator()) {
            return TransferResult.SPECTATOR;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId()) {
            return TransferResult.STALE_MENU;
        }
        if (!menu.stillValid(player)) {
            return TransferResult.INVALID_MENU;
        }
        if (!menu.getCarried().isEmpty()) {
            return TransferResult.CARRIED_STACK;
        }
        if (!SortMenuPolicy.supportsStorageActions(menu)) {
            return TransferResult.UNSUPPORTED_MENU;
        }

        Optional<StorageMenuSlots> resolvedSlots = StorageMenuSlots.resolve(player, menu);
        if (resolvedSlots.isEmpty()) {
            return TransferResult.NO_SLOTS;
        }

        StorageMenuSlots slots = resolvedSlots.get();
        List<Slot> container = slots.containerSlots().stream().map(IndexedSlot::slot).toList();
        List<Slot> hotbar = new ArrayList<>();
        List<Slot> main = new ArrayList<>();
        for (IndexedSlot indexed : slots.playerSlots()) {
            (indexed.slot().getContainerSlot() < HOTBAR_END ? hotbar : main).add(indexed.slot());
        }
        // Items arriving in the inventory fill the main inventory before the hotbar.
        List<Slot> inventory = new ArrayList<>(main);
        inventory.addAll(hotbar);

        TransferContainerPayload.Action action = payload.action();
        List<Slot> origins = switch (action) {
            case DEPOSIT_MATCHING -> inventory;
            case DEPOSIT_ALL -> main;
            case RETRIEVE_MATCHING, RETRIEVE_ALL -> container;
        };
        List<Slot> destinations = action.deposits() ? container : inventory;
        List<ItemStack> representedStacks = action.matchingOnly() ? representedStacks(destinations) : List.of();

        int movedStacks = 0;
        try {
            for (Slot origin : origins) {
                ItemStack stack = origin.getItem();
                if (stack.isEmpty() || !origin.mayPickup(player) || !origin.allowModification(player)) {
                    continue;
                }
                if (action.matchingOnly() && !isRepresented(stack, representedStacks)) {
                    continue;
                }
                if (move(player, origin, destinations) > 0) {
                    movedStacks++;
                }
            }
            menu.broadcastChanges();
            return movedStacks == 0 ? TransferResult.NOTHING_MOVED : TransferResult.MOVED;
        } catch (RuntimeException failure) {
            menu.broadcastChanges();
            MiniSort.LOG.warn("Container transfer stopped after moving {} stacks", movedStacks, failure);
            return movedStacks == 0 ? TransferResult.FAILED : TransferResult.PARTIALLY_MOVED;
        }
    }

    /** Tops up matching stacks first, then fills empty slots, like a shift-click. Returns how many items moved. */
    private static int move(ServerPlayer player, Slot origin, List<Slot> destinations) {
        ItemStack moving = origin.getItem().copy();
        int before = moving.getCount();
        for (Slot destination : destinations) {
            if (moving.isEmpty()) {
                break;
            }
            ItemStack existing = destination.getItem();
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, moving)
                    && destination.allowModification(player)) {
                moving = destination.safeInsert(moving);
            }
        }
        for (Slot destination : destinations) {
            if (moving.isEmpty()) {
                break;
            }
            if (destination.getItem().isEmpty() && destination.allowModification(player)) {
                moving = destination.safeInsert(moving);
            }
        }
        int moved = before - moving.getCount();
        if (moved > 0) {
            origin.remove(moved);
            origin.setChanged();
        }
        return moved;
    }

    private static List<ItemStack> representedStacks(List<Slot> slots) {
        List<ItemStack> represented = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && !isRepresented(stack, represented)) {
                represented.add(stack.copyWithCount(1));
            }
        }
        return represented;
    }

    private static boolean isRepresented(ItemStack stack, List<ItemStack> representedStacks) {
        for (ItemStack represented : representedStacks) {
            if (ItemStack.isSameItemSameComponents(stack, represented)) {
                return true;
            }
        }
        return false;
    }

    public enum TransferResult {
        MOVED,
        NOTHING_MOVED,
        SPECTATOR,
        STALE_MENU,
        INVALID_MENU,
        CARRIED_STACK,
        UNSUPPORTED_MENU,
        NO_SLOTS,
        PARTIALLY_MOVED,
        FAILED
    }
}
