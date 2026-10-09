package com.iamkaf.minisort.client;

import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/** What every container screen exposes to integrations, such as recipe viewers and controller mods. */
public interface MiniSortScreen {
    /** The screen areas Minisort's visible buttons cover, for other mods to keep clear. */
    List<Rect2i> miniSort$buttonAreas();

    /** Does what the sort key does on this screen. Returns whether it sorted something. */
    boolean miniSort$pressSortKey();
}
