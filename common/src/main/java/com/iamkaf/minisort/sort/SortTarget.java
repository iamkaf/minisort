package com.iamkaf.minisort.sort;

/**
 * Which part of the open screen a sort request reorders.
 * The sort packet sends the ordinal, so append new targets at the end.
 */
public enum SortTarget {
    /** The storage block's slots, such as a chest's. */
    CONTAINER,
    /** The player's 27 main inventory slots. The hotbar stays where it is. */
    INVENTORY
}
