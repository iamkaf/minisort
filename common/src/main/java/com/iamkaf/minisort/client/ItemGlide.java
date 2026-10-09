package com.iamkaf.minisort.client;

import com.iamkaf.amber.api.platform.v1.Platform;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the items of a sort, deposit, or retrieve Minisort asked for gliding from their old slots to their new ones.
 * It arms when the player presses one, and plays only if the next change to those slots keeps every item's total:
 * anything else, like a click, a hopper, or another mod's sort, disarms it. Only one screen is open at a time, so the
 * state is global.
 */
public final class ItemGlide {
    private static final long DURATION_NANOS = 120_000_000L;
    // A sort the server rejected changes nothing; stop waiting for it after this long.
    private static final long ARMED_NANOS = 1_000_000_000L;
    // Smooth Swapping animates every slot change, ours included; two animations at once would fight.
    private static final boolean OTHER_ANIMATION = Platform.isModLoaded("smoothswapping");

    private static @Nullable Armed armed;
    private static @Nullable Playing playing;

    private ItemGlide() {
    }

    /** Remembers what the slots an action touches hold, just before its request goes out. */
    public static void arm(AbstractContainerMenu menu, List<Slot> slots) {
        playing = null;
        armed = null;
        if (OTHER_ANIMATION || !ClientConfig.itemAnimation() || slots.isEmpty()) {
            return;
        }
        Map<Slot, ItemStack> before = new LinkedHashMap<>();
        for (Slot slot : slots) {
            before.put(slot, slot.getItem().copy());
        }
        armed = new Armed(menu, before, System.nanoTime() + ARMED_NANOS);
    }

    /** Runs once per frame before the slots draw: starts the glide when the sort lands, and ends it when done. */
    public static void frame(AbstractContainerMenu menu) {
        long now = System.nanoTime();
        Armed waiting = armed;
        if (waiting != null) {
            if (waiting.menu != menu) {
                armed = null;
            } else if (changed(waiting.before)) {
                armed = null;
                playing = sameTotals(waiting.before) ? pair(menu, waiting.before, now) : null;
            } else if (now > waiting.deadline) {
                armed = null;
            }
        }
        Playing current = playing;
        if (current != null && (current.menu != menu || now - current.start >= DURATION_NANOS)) {
            playing = null;
        } else if (current != null) {
            // Whatever changes a slot mid-flight, like a click or a hopper, lands that item at once.
            current.flights.entrySet().removeIf(flight -> !ItemStack.matches(flight.getKey().getItem(), flight.getValue().landed));
            current.ghosts.removeIf(ghost -> !ItemStack.matches(ghost.to.getItem(), ghost.landed));
        }
    }

    /** How far from its own position this slot's contents should draw now, or null when they rest in place. */
    public static @Nullable Offset offset(Slot slot) {
        Playing current = playing;
        Flight flight = current == null ? null : current.flights.get(slot);
        return flight == null ? null : current.offset(flight.fromX - slot.x, flight.fromY - slot.y);
    }

    /** Items merged into another slot, drawn on their way there without a count. Positions are slot-relative. */
    public static List<Ghost> ghosts() {
        Playing current = playing;
        return current == null ? List.of() : current.ghosts;
    }

    public static Offset ghostOffset(Ghost ghost) {
        Playing current = playing;
        return current == null ? new Offset(0, 0) : current.offset(ghost.fromX - ghost.to.x, ghost.fromY - ghost.to.y);
    }

    private static boolean changed(Map<Slot, ItemStack> before) {
        for (Map.Entry<Slot, ItemStack> entry : before.entrySet()) {
            if (!ItemStack.matches(entry.getKey().getItem(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameTotals(Map<Slot, ItemStack> before) {
        List<ItemStack> was = totals(before.values());
        List<ItemStack> now = totals(before.keySet().stream().map(Slot::getItem).toList());
        if (was.size() != now.size()) {
            return false;
        }
        for (ItemStack total : was) {
            if (now.stream().noneMatch(other -> ItemStack.isSameItemSameComponents(total, other) && total.getCount() == other.getCount())) {
                return false;
            }
        }
        return true;
    }

    /** One stack per distinct item, holding that item's total count. */
    private static List<ItemStack> totals(Iterable<ItemStack> stacks) {
        List<ItemStack> totals = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack total = totals.stream().filter(other -> ItemStack.isSameItemSameComponents(other, stack)).findFirst().orElse(null);
            if (total == null) {
                totals.add(stack.copy());
            } else {
                total.grow(stack.getCount());
            }
        }
        return totals;
    }

    /**
     * Pairs old slots with new ones, item by item. Counts pour from the old slots into the new ones in slot order,
     * as the server pools them. A new slot that held some of the item already stays put; otherwise it glides in from
     * the old slot that gave it the most. Every other old slot that gave it some flies in as a ghost.
     */
    private static Playing pair(AbstractContainerMenu menu, Map<Slot, ItemStack> before, long now) {
        Map<Slot, Flight> flights = new IdentityHashMap<>();
        List<Ghost> ghosts = new ArrayList<>();
        for (ItemStack total : totals(before.values())) {
            List<Slot> sources = new ArrayList<>();
            List<Integer> left = new ArrayList<>();
            before.forEach((slot, was) -> {
                if (ItemStack.isSameItemSameComponents(was, total)) {
                    sources.add(slot);
                    left.add(was.getCount());
                }
            });
            int source = 0;
            for (Slot to : before.keySet()) {
                ItemStack landed = to.getItem();
                if (!ItemStack.isSameItemSameComponents(landed, total)) {
                    continue;
                }
                Map<Slot, Integer> given = new LinkedHashMap<>();
                int need = landed.getCount();
                while (need > 0 && source < sources.size()) {
                    int take = Math.min(need, left.get(source));
                    given.merge(sources.get(source), take, Integer::sum);
                    left.set(source, left.get(source) - take);
                    need -= take;
                    if (left.get(source) == 0) {
                        source++;
                    }
                }
                Slot primary = given.containsKey(to) ? to
                        : given.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(to);
                ItemStack frozen = landed.copy();
                if (primary != to) {
                    flights.put(to, new Flight(primary.x, primary.y, frozen));
                }
                for (Slot from : given.keySet()) {
                    if (from != primary && from != to) {
                        ghosts.add(new Ghost(before.get(from).copyWithCount(1), from.x, from.y, to, frozen));
                    }
                }
            }
        }
        return new Playing(menu, now, flights, ghosts);
    }

    public record Offset(float x, float y) {
    }

    public record Ghost(ItemStack item, int fromX, int fromY, Slot to, ItemStack landed) {
    }

    private record Flight(int fromX, int fromY, ItemStack landed) {
    }

    private record Armed(AbstractContainerMenu menu, Map<Slot, ItemStack> before, long deadline) {
    }

    private record Playing(AbstractContainerMenu menu, long start, Map<Slot, Flight> flights, List<Ghost> ghosts) {
        /** The offset still left to travel from a full offset of (dx, dy), easing out over the glide. */
        Offset offset(int dx, int dy) {
            float t = Math.min(1F, (System.nanoTime() - start) / (float) DURATION_NANOS);
            float remaining = (1F - t) * (1F - t) * (1F - t);
            return new Offset(dx * remaining, dy * remaining);
        }
    }
}
