package com.iamkaf.minisort.sort;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

import java.util.Arrays;
import java.util.List;

/** Category sort groups, in chest order. Mirrors the creative tabs players already know. */
public enum ItemCategory {
    BLOCKS,
    TOOLS,
    COMBAT,
    ARMOR,
    FOOD,
    POTIONS,
    MATERIALS,
    SPAWN_EGGS;

    // Tags are looked up by ID because the ItemTags constants differ between Minecraft versions.
    // A tag that does not exist on a version simply matches nothing. The c: tags catch modded items.
    private static final List<TagKey<Item>> COMBAT_TAGS = tags(
            "minecraft:swords", "minecraft:spears", "minecraft:arrows", "minecraft:enchantable/bow",
            "minecraft:enchantable/crossbow", "minecraft:enchantable/trident", "minecraft:enchantable/mace",
            "c:tools/shield", "c:tools/bow", "c:tools/crossbow", "c:tools/spear", "c:tools/trident", "c:tools/mace"
    );
    private static final List<TagKey<Item>> ARMOR_TAGS = tags(
            "minecraft:head_armor", "minecraft:chest_armor", "minecraft:leg_armor", "minecraft:foot_armor"
    );
    private static final List<TagKey<Item>> TOOL_TAGS = tags(
            "minecraft:pickaxes", "minecraft:axes", "minecraft:shovels", "minecraft:hoes", "minecraft:boats",
            "minecraft:chest_boats", "minecraft:compasses", "minecraft:bundles", "c:tools"
    );

    public static ItemCategory of(ItemStack stack, Player player) {
        Item item = stack.getItem();
        if (item instanceof SpawnEggItem) {
            return SPAWN_EGGS;
        }
        // Fish buckets carry a food component but cannot be eaten.
        if (stack.has(DataComponents.FOOD) && stack.getUseDuration(player) > 0) {
            return FOOD;
        }
        // Before potions, so tipped arrows stay with the other arrows.
        if (stack.is(Items.SHIELD) || isAny(stack, COMBAT_TAGS)) {
            return COMBAT;
        }
        if (stack.has(DataComponents.POTION_CONTENTS)) {
            return POTIONS;
        }
        // Heads and carved pumpkins are wearable blocks, so they stay with blocks.
        if (!(item instanceof BlockItem)
                && (isAny(stack, ARMOR_TAGS) || player.getEquipmentSlotForItem(stack).getType() != EquipmentSlot.Type.HAND)) {
            return ARMOR;
        }
        if (stack.isDamageableItem() || isAny(stack, TOOL_TAGS)) {
            return TOOLS;
        }
        if (item instanceof BlockItem) {
            return BLOCKS;
        }
        return MATERIALS;
    }

    private static boolean isAny(ItemStack stack, List<TagKey<Item>> tags) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static List<TagKey<Item>> tags(String... ids) {
        return Arrays.stream(ids)
                //? if >=1.21.11 {
                .map(id -> TagKey.create(Registries.ITEM, Identifier.parse(id)))
                //?} else {
                /*.map(id -> TagKey.create(Registries.ITEM, ResourceLocation.parse(id)))
                *///?}
                .toList();
    }
}
