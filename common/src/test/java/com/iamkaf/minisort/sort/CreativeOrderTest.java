package com.iamkaf.minisort.sort;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeOrderTest {
    @Test
    void itemsOnNoTabFollowTheirRegistryNeighbour() {
        List<String> registry = List.of("oak_planks", "oak_slab", "petrified_oak_slab", "debug_stick", "stone", "barrier");
        List<String> creative = List.of("stone", "oak_planks", "oak_slab");

        assertEquals(
                List.of("stone", "barrier", "oak_planks", "oak_slab", "petrified_oak_slab", "debug_stick"),
                CreativeOrder.complete(registry, creative)
        );
    }

    @Test
    void itemsRegisteredBeforeAnyTabItemComeFirst() {
        assertEquals(
                List.of("hidden", "stone", "dirt"),
                CreativeOrder.complete(List.of("hidden", "stone", "dirt"), List.of("stone", "dirt"))
        );
    }

    @Test
    void anItemOnSeveralTabsKeepsItsFirstPlace() {
        assertEquals(
                List.of("stone", "dirt"),
                CreativeOrder.complete(List.of("dirt", "stone"), List.of("stone", "dirt", "stone"))
        );
    }
}
