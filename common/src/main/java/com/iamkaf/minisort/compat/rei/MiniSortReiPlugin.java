package com.iamkaf.minisort.compat.rei;

import com.iamkaf.minisort.client.MiniSortScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

/**
 * Keeps REI's item list clear of Minisort's buttons. Fabric loads this through its entrypoint. NeoForge finds plugins
 * by annotation, so its annotated subclass lives in the NeoForge source set: the horizontal jar carries a copy of
 * every common class, and an annotation here would register them all.
 */
public class MiniSortReiPlugin implements REIClientPlugin {
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(AbstractContainerScreen.class, screen ->
                screen instanceof MiniSortScreen miniSort
                        ? miniSort.miniSort$buttonAreas().stream()
                                .map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()))
                                .toList()
                        : List.<Rectangle>of());
    }
}
