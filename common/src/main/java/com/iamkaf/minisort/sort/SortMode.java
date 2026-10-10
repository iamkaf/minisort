package com.iamkaf.minisort.sort;

/** How the sort button orders a container. Each player picks one in the client config. */
public enum SortMode {
    /** The creative inventory's order, tab by tab. */
    CREATIVE,
    /** Alphabetical by registry ID, so items group by mod and then by name. */
    REGISTRY_ID
}
