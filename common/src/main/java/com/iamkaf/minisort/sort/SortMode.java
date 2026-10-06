package com.iamkaf.minisort.sort;

/**
 * How the sort button orders a container. Each player picks one in the client config.
 * The sort packet sends the ordinal, so append new modes at the end.
 */
public enum SortMode {
    /** Alphabetical by registry ID, so items group by mod and then by name. */
    REGISTRY_ID,
    /** Experimental. Groups items by kind, then by material, tier, shape, and color. */
    CATEGORIES
}
