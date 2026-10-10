package com.iamkaf.minisort.compat.jei;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.client.MiniSortScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

import java.util.List;

/**
 * Keeps JEI's ingredient list and bookmarks clear of Minisort's buttons. Fabric loads this through its entrypoint.
 * Forge and NeoForge find plugins by annotation, so their annotated subclasses live in the loader source sets: the
 * horizontal jars carry copies of common classes, and an annotation here would register every copy.
 */
public class MiniSortJeiPlugin implements IModPlugin {
    //? if >=1.21.11 {
    @Override
    public Identifier getPluginUid() {
        return MiniSort.resource("jei");
    }
    //?} else {
    /*@Override
    public ResourceLocation getPluginUid() {
        return MiniSort.resource("jei");
    }
    *///?}

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new IGuiContainerHandler<AbstractContainerScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
                return screen instanceof MiniSortScreen miniSort ? miniSort.miniSort$buttonAreas() : List.of();
            }
        });
    }
}
