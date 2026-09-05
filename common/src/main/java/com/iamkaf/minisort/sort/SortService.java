package com.iamkaf.minisort.sort;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.inventory.StorageMenuSlots;
import com.iamkaf.minisort.network.SortContainerPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class SortService {
    private static final Comparator<PooledStack> STACK_ORDER = Comparator
            .comparing((PooledStack pool) -> itemId(pool.prototype))
            .thenComparing(pool -> componentKey(pool.prototype));

    private SortService() {
    }

    public static SortResult sort(ServerPlayer player, SortContainerPayload payload) {
        if (payload.target() != SortContainerPayload.SortTarget.CONTAINER
                || payload.mode() != SortContainerPayload.SortMode.REGISTRY_ID) {
            return SortResult.UNSUPPORTED_REQUEST;
        }
        if (player.isSpectator()) {
            return SortResult.SPECTATOR;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId()) {
            return SortResult.STALE_MENU;
        }
        if (!menu.stillValid(player)) {
            return SortResult.INVALID_MENU;
        }
        if (!menu.getCarried().isEmpty()) {
            return SortResult.CARRIED_STACK;
        }
        if (!SortMenuPolicy.supportsStorageActions(menu)) {
            return SortResult.UNSUPPORTED_MENU;
        }

        List<Slot> slots = StorageMenuSlots.resolve(player, menu)
                .map(resolved -> resolved.containerSlots().stream()
                        .map(StorageMenuSlots.IndexedSlot::slot)
                        .toList())
                .orElse(List.of());
        if (slots.isEmpty() || !allSlotsMutable(player, slots)) {
            return SortResult.NO_SLOTS;
        }

        List<ItemStack> snapshot = snapshot(slots);
        List<ItemStack> plan = plan(slots, snapshot);
        if (plan == null) {
            return SortResult.SLOT_CONSTRAINT;
        }

        try {
            write(slots, plan);
            verify(slots, plan);
            menu.broadcastChanges();
            return SortResult.SORTED;
        } catch (RuntimeException commitFailure) {
            RuntimeException rollbackFailure = restore(slots, snapshot);
            menu.broadcastChanges();
            if (rollbackFailure != null) {
                commitFailure.addSuppressed(rollbackFailure);
                MiniSort.LOG.error("Container sort rollback failed", commitFailure);
                return SortResult.ROLLBACK_FAILED;
            }
            MiniSort.LOG.warn("Container sort failed and was rolled back", commitFailure);
            return SortResult.ROLLED_BACK;
        }
    }

    private static boolean allSlotsMutable(ServerPlayer player, List<Slot> slots) {
        for (Slot slot : slots) {
            if (!slot.mayPickup(player) || !slot.allowModification(player)) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> snapshot(List<Slot> slots) {
        List<ItemStack> snapshot = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            snapshot.add(slot.getItem().copy());
        }
        return snapshot;
    }

    private static List<ItemStack> plan(List<Slot> slots, List<ItemStack> snapshot) {
        List<PooledStack> pools = compact(snapshot);
        pools.sort(STACK_ORDER);

        List<ItemStack> plan = new ArrayList<>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            plan.add(ItemStack.EMPTY);
        }

        int slotIndex = 0;
        for (PooledStack pool : pools) {
            long remaining = pool.count;
            while (remaining > 0) {
                if (slotIndex >= slots.size()) {
                    return null;
                }
                Slot slot = slots.get(slotIndex);
                ItemStack probe = pool.prototype.copyWithCount(1);
                if (!slot.mayPlace(probe)) {
                    return null;
                }
                int capacity = Math.min(slot.getMaxStackSize(probe), probe.getMaxStackSize());
                if (capacity <= 0) {
                    return null;
                }
                int count = (int) Math.min(remaining, capacity);
                plan.set(slotIndex, pool.prototype.copyWithCount(count));
                remaining -= count;
                slotIndex++;
            }
        }
        return plan;
    }

    private static List<PooledStack> compact(List<ItemStack> snapshot) {
        List<PooledStack> pools = new ArrayList<>();
        for (ItemStack stack : snapshot) {
            if (stack.isEmpty()) {
                continue;
            }
            PooledStack matching = null;
            for (PooledStack pool : pools) {
                if (ItemStack.isSameItemSameComponents(pool.prototype, stack)) {
                    matching = pool;
                    break;
                }
            }
            if (matching == null) {
                pools.add(new PooledStack(stack.copyWithCount(1), stack.getCount()));
            } else {
                matching.count += stack.getCount();
            }
        }
        return pools;
    }

    private static void write(List<Slot> slots, List<ItemStack> contents) {
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).set(contents.get(i).copy());
        }
    }

    private static void verify(List<Slot> slots, List<ItemStack> expected) {
        for (int i = 0; i < slots.size(); i++) {
            if (!sameStack(slots.get(i).getItem(), expected.get(i))) {
                throw new IllegalStateException("Container rejected a planned sort write");
            }
        }
    }

    private static RuntimeException restore(List<Slot> slots, List<ItemStack> snapshot) {
        RuntimeException failure = null;
        for (int i = 0; i < slots.size(); i++) {
            try {
                slots.get(i).set(snapshot.get(i).copy());
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Could not fully restore the container");
                }
                failure.addSuppressed(exception);
            }
        }
        return failure;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() && right.isEmpty();
        }
        return left.getCount() == right.getCount() && ItemStack.isSameItemSameComponents(left, right);
    }

    private static String itemId(ItemStack stack) {
        return Objects.toString(BuiltInRegistries.ITEM.getKey(stack.getItem()), "");
    }

    private static String componentKey(ItemStack stack) {
        return stack.getComponents().stream()
                .sorted(Comparator.comparing(component -> componentTypeId(component.type())))
                .map(component -> componentTypeId(component.type()) + "=" + Objects.toString(component.value()))
                .reduce((left, right) -> left + "\u0000" + right)
                .orElse("");
    }

    private static String componentTypeId(net.minecraft.core.component.DataComponentType<?> type) {
        return Objects.toString(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type), "");
    }

    private static final class PooledStack {
        private final ItemStack prototype;
        private long count;

        private PooledStack(ItemStack prototype, long count) {
            this.prototype = prototype;
            this.count = count;
        }
    }

    public enum SortResult {
        SORTED,
        UNSUPPORTED_REQUEST,
        SPECTATOR,
        STALE_MENU,
        INVALID_MENU,
        CARRIED_STACK,
        UNSUPPORTED_MENU,
        NO_SLOTS,
        SLOT_CONSTRAINT,
        ROLLED_BACK,
        ROLLBACK_FAILED
    }
}
