package com.iamkaf.minisort.compat.controlify;

import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.client.MiniSortClient;
import com.iamkaf.minisort.client.MiniSortScreen;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.api.event.ControlifyEvents;
import dev.isxander.controlify.bindings.BindContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Gives the Sort key a controller binding that works inside container screens. Controlify's automatic binding for
 * a key mapping is in-game only, so this one takes the key's place. Fabric loads this class through its
 * {@code controlify} entrypoint; NeoForge through the service file in its own source set.
 */
public final class MiniSortControlify implements ControlifyEntrypoint {
    @Override
    public void onControlifyPreInit(PreInitContext context) {
        InputBindingSupplier sort = context.bindings().registerBinding(binding -> binding
                .id(MiniSort.MOD_ID, "sort")
                .name(Component.translatable(MiniSortClient.SORT_KEY.getName()))
                //? if >=1.21.9 {
                .category(MiniSortClient.SORT_KEY.getCategory().label())
                //?} else {
                /*.category(Component.translatable(MiniSortClient.SORT_KEY.getCategory()))
                *///?}
                .allowedContexts(BindContext.CONTAINER)
                .addKeyCorrelation(MiniSortClient.SORT_KEY));

        ControlifyEvents.ACTIVE_CONTROLLER_TICKED.register(event -> {
            if (currentScreen() instanceof MiniSortScreen screen && sort.on(event.controller()).justPressed()) {
                screen.miniSort$pressSortKey();
            }
        });
    }

    private static Screen currentScreen() {
        //? if >=26.2 {
        return Minecraft.getInstance().gui.screen();
        //?} else {
        /*return Minecraft.getInstance().screen;
        *///?}
    }

    @Override
    public void onControlifyInit(InitContext context) {
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
    }
}
