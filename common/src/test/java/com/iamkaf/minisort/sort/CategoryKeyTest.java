package com.iamkaf.minisort.sort;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryKeyTest {
    @Test
    void groupsToolsByTypeWeakestFirst() {
        assertEquals(
                List.of("wooden_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe", "netherite_pickaxe",
                        "mymod:ruby_pickaxe", "wooden_shovel", "stone_shovel"),
                sorted(ItemCategory.TOOLS, "diamond_pickaxe", "stone_shovel", "mymod:ruby_pickaxe", "netherite_pickaxe",
                        "wooden_shovel", "iron_pickaxe", "golden_pickaxe", "wooden_pickaxe")
        );
    }

    @Test
    void keepsArmorSetsTogetherFromHeadToFeet() {
        assertEquals(
                List.of("iron_helmet", "iron_chestplate", "diamond_helmet", "diamond_boots", "diamond_horse_armor",
                        "turtle_helmet"),
                sorted(ItemCategory.ARMOR, "diamond_boots", "turtle_helmet", "iron_chestplate", "diamond_horse_armor",
                        "diamond_helmet", "iron_helmet")
        );
    }

    @Test
    void groupsBlocksByMaterialThenShape() {
        assertEquals(
                List.of("oak_log", "oak_planks", "oak_stairs", "oak_door", "stripped_oak_log", "stone", "stone_stairs",
                        "stone_bricks", "stone_brick_stairs", "mossy_stone_bricks"),
                sorted(ItemCategory.BLOCKS, "oak_door", "stone_brick_stairs", "stripped_oak_log", "stone", "oak_planks",
                        "mossy_stone_bricks", "oak_log", "stone_stairs", "stone_bricks", "oak_stairs")
        );
    }

    @Test
    void ordersColorsLikeDyesAfterTheUncoloredBlock() {
        assertEquals(
                List.of("terracotta", "white_terracotta", "black_terracotta", "white_wool", "light_blue_wool", "blue_wool"),
                sorted(ItemCategory.BLOCKS, "blue_wool", "black_terracotta", "light_blue_wool", "terracotta",
                        "white_wool", "white_terracotta")
        );
    }

    @Test
    void keepsOresWithTheirMaterial() {
        assertEquals(
                List.of("iron_block", "iron_ore", "deepslate_iron_ore", "raw_iron_block"),
                sorted(ItemCategory.BLOCKS, "raw_iron_block", "deepslate_iron_ore", "iron_ore", "iron_block")
        );
    }

    @Test
    void putsCookedFoodAfterItsRawForm() {
        assertEquals(
                List.of("apple", "golden_apple", "beef", "cooked_beef"),
                sorted(ItemCategory.FOOD, "cooked_beef", "golden_apple", "beef", "apple")
        );
    }

    private static List<String> sorted(ItemCategory category, String... ids) {
        return Arrays.stream(ids)
                .map(id -> id.contains(":") ? id : "minecraft:" + id)
                .map(id -> CategoryKey.of(category, id))
                .sorted()
                .map(key -> key.itemId().replace("minecraft:", ""))
                .toList();
    }
}
