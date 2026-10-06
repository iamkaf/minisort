package com.iamkaf.minisort.sort;

import java.util.Comparator;
import java.util.List;

/**
 * Orders items for the experimental category sort using only their category and registry ID.
 *
 * <p>Blocks group by material, so oak logs, planks, stairs, and doors stay together. Other items
 * group by the last word of their name, so every pickaxe, ingot, or spawn egg sits together. Tools
 * and armor follow vanilla material tiers, and colored variants follow dye order. Modded items that
 * follow vanilla naming land beside their vanilla counterparts.
 *
 * <p>Fields compare in declaration order.
 */
public record CategoryKey(
        ItemCategory category,
        int rank,
        String family,
        int subRank,
        String qualifier,
        int shape,
        int color,
        String itemId
) implements Comparable<CategoryKey> {
    private static final Comparator<CategoryKey> ORDER = Comparator
            .comparing(CategoryKey::category)
            .thenComparingInt(CategoryKey::rank)
            .thenComparing(CategoryKey::family)
            .thenComparingInt(CategoryKey::subRank)
            .thenComparing(CategoryKey::qualifier)
            .thenComparingInt(CategoryKey::shape)
            .thenComparingInt(CategoryKey::color)
            .thenComparing(CategoryKey::itemId);

    // Vanilla dye order. Uncolored items use -1 and sort first.
    private static final List<String> COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );
    private static final List<String> COLORS_LONGEST_FIRST = longestFirst(COLORS);

    // Tool and armor materials, weakest first. The index is the tier.
    private static final List<List<String>> TIERS = List.of(
            List.of("wooden", "leather"),
            List.of("stone", "chainmail"),
            List.of("copper"),
            List.of("iron"),
            List.of("golden"),
            List.of("diamond"),
            List.of("netherite")
    );
    private static final int UNKNOWN_TIER = TIERS.size();

    private static final List<String> ARMOR_SLOTS = List.of("helmet", "chestplate", "leggings", "boots");

    // Block name prefixes that describe a variant of the same material.
    private static final List<String> BLOCK_VARIANTS = List.of(
            "stripped", "polished", "smooth", "cut", "chiseled", "cracked", "mossy", "cobbled",
            "infested", "waxed", "exposed", "weathered", "oxidized", "raw"
    );

    // Block name suffixes in display order. The empty suffix is the plain block.
    private static final List<String> BLOCK_SHAPES = List.of(
            "_log", "_stem", "_wood", "_hyphae", "_planks", "_block", "", "_stairs", "_slab", "_wall",
            "_fence", "_fence_gate", "_door", "_trapdoor", "_pressure_plate", "_button", "_sign",
            "_hanging_sign", "_shelf", "_bars", "_chain", "_lantern", "_ore", "_leaves", "_sapling"
    );
    private static final List<String> BLOCK_SHAPES_LONGEST_FIRST = longestFirst(BLOCK_SHAPES);
    private static final int PLAIN_BLOCK = BLOCK_SHAPES.indexOf("");
    private static final int ORE = BLOCK_SHAPES.indexOf("_ore");

    // Ores share a material with their deepslate and nether forms.
    private static final List<String> ORE_HOSTS = List.of("deepslate", "nether");

    // Name prefixes that name a family on their own, such as every music disc.
    private static final List<String> FAMILY_PREFIXES = List.of("music_disc", "raw");

    public static CategoryKey of(ItemCategory category, String itemId) {
        String path = itemId.substring(itemId.indexOf(':') + 1);
        int color = colorOf(path);
        String name = color < 0 ? path : path.substring(COLORS.get(color).length() + 1);
        return switch (category) {
            case BLOCKS -> block(name, color, itemId);
            case TOOLS, COMBAT -> {
                // Every pickaxe together, weakest first.
                Words words = Words.of(name);
                yield new CategoryKey(category, 0, words.family(), tierOf(words.qualifier()),
                        words.qualifier(), 0, color, itemId);
            }
            case ARMOR -> {
                // Full sets together, weakest first, then head to feet.
                Words words = Words.of(name);
                int slot = ARMOR_SLOTS.indexOf(words.family());
                yield new CategoryKey(category, tierOf(words.qualifier()), words.qualifier(),
                        slot < 0 ? ARMOR_SLOTS.size() : slot, words.family(), 0, color, itemId);
            }
            case FOOD, POTIONS, MATERIALS, SPAWN_EGGS -> {
                Words words = Words.of(name);
                yield new CategoryKey(category, 0, words.family(), 0, words.qualifier(), 0, color, itemId);
            }
        };
    }

    @Override
    public int compareTo(CategoryKey other) {
        return ORDER.compare(this, other);
    }

    private static CategoryKey block(String name, int color, String itemId) {
        String material = name;
        StringBuilder variants = new StringBuilder();
        String variant = leadingWord(material, BLOCK_VARIANTS);
        while (variant != null) {
            variants.append(variant).append('_');
            material = material.substring(variant.length() + 1);
            variant = leadingWord(material, BLOCK_VARIANTS);
        }

        int shape = PLAIN_BLOCK;
        for (String suffix : BLOCK_SHAPES_LONGEST_FIRST) {
            if (!suffix.isEmpty() && material.endsWith(suffix) && material.length() > suffix.length()) {
                shape = BLOCK_SHAPES.indexOf(suffix);
                material = material.substring(0, material.length() - suffix.length());
                break;
            }
        }
        if (shape == ORE) {
            String host = leadingWord(material, ORE_HOSTS);
            if (host != null) {
                variants.append(host).append('_');
                material = material.substring(host.length() + 1);
            }
        }

        return new CategoryKey(ItemCategory.BLOCKS, 0, singular(material), 0, variants.toString(), shape, color, itemId);
    }

    // Bricks and brick stairs, or deepslate tiles and tile slabs, share a family.
    private static String singular(String material) {
        if (material.endsWith("bricks") || material.endsWith("tiles")) {
            return material.substring(0, material.length() - 1);
        }
        return material;
    }

    private static int colorOf(String path) {
        String color = leadingWord(path, COLORS_LONGEST_FIRST);
        return color == null ? -1 : COLORS.indexOf(color);
    }

    // Tiers come from the first word, so diamond_horse_armor joins the diamond set.
    private static int tierOf(String qualifier) {
        int end = qualifier.indexOf('_');
        String material = end < 0 ? qualifier : qualifier.substring(0, end);
        for (int tier = 0; tier < TIERS.size(); tier++) {
            if (TIERS.get(tier).contains(material)) {
                return tier;
            }
        }
        return UNKNOWN_TIER;
    }

    private static String leadingWord(String name, List<String> candidates) {
        for (String candidate : candidates) {
            if (name.startsWith(candidate + "_")) {
                return candidate;
            }
        }
        return null;
    }

    private static List<String> longestFirst(List<String> values) {
        return values.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();
    }

    /** A name split into its family, usually the last word, and the qualifier in front of it. */
    private record Words(String family, String qualifier) {
        static Words of(String name) {
            String prefix = leadingWord(name, FAMILY_PREFIXES);
            if (prefix != null) {
                return new Words(prefix, name.substring(prefix.length() + 1));
            }
            int lastWord = name.lastIndexOf('_');
            if (lastWord < 0) {
                return new Words(name, "");
            }
            return new Words(name.substring(lastWord + 1), name.substring(0, lastWord));
        }
    }
}
