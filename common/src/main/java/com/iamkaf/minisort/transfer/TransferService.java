package com.iamkaf.minisort.transfer;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.inventory.StorageMenuSlots;
import com.iamkaf.minisort.inventory.StorageMenuSlots.IndexedSlot;
import com.iamkaf.minisort.network.TransferContainerPayload;
import com.iamkaf.minisort.sort.SortMenuPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TransferService {
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
        boolean depositMatching = payload.action() == TransferContainerPayload.Action.DEPOSIT_MATCHING;
        List<IndexedSlot> origins = depositMatching
                ? slots.playerSlots()
                : slots.containerSlots();
        List<IndexedSlot> destinations = depositMatching
                ? slots.containerSlots()
                : slots.playerSlots();
        List<ItemStack> representedStacks = representedStacks(destinations);

        int movedStacks = 0;
        try {
            for (IndexedSlot origin : origins) {
                ItemStack stack = origin.slot().getItem();
                if (stack.isEmpty() || !origin.slot().mayPickup(player) || !origin.slot().allowModification(player)) {
                    continue;
                }
                if (!isRepresented(stack, representedStacks)) {
                    continue;
                }

                int beforeCount = stack.getCount();
                menu.quickMoveStack(player, origin.menuIndex());
                if (origin.slot().getItem().getCount() < beforeCount) {
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

    private static List<ItemStack> representedStacks(List<IndexedSlot> slots) {
        List<ItemStack> represented = new ArrayList<>();
        for (IndexedSlot indexedSlot : slots) {
            ItemStack stack = indexedSlot.slot().getItem();
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
