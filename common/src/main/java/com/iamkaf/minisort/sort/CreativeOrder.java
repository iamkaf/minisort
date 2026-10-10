package com.iamkaf.minisort.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Completes the creative inventory's order with the items no tab shows. */
public final class CreativeOrder {
    private CreativeOrder() {
    }

    /**
     * Every registered item, in creative order. An item on no tab follows the nearest item registered before it
     * that is on one, since mods register related items together.
     */
    public static <T> List<T> complete(List<T> registryOrder, Collection<T> creativeOrder) {
        Set<T> shown = new LinkedHashSet<>(creativeOrder);
        Map<T, List<T>> followers = new HashMap<>();
        List<T> leading = new ArrayList<>();
        T anchor = null;
        for (T item : registryOrder) {
            if (shown.contains(item)) {
                anchor = item;
            } else if (anchor == null) {
                leading.add(item);
            } else {
                followers.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(item);
            }
        }

        List<T> order = new ArrayList<>(leading);
        for (T item : shown) {
            order.add(item);
            order.addAll(followers.getOrDefault(item, List.of()));
        }
        return order;
    }
}
