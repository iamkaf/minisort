package com.iamkaf.minisort.compat.controlify;

import com.iamkaf.amber.api.platform.v1.Platform;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;

/**
 * NeoForge's Controlify finds entrypoints only through the service file. Fabric's Controlify reads service files
 * too, and the combined jar carries this one, so it hands over only on NeoForge: on Fabric, the copy of
 * {@link MiniSortControlify} it would reach is the NeoForge build. Fabric uses its own entrypoint instead.
 */
public final class NeoForgeControlifyEntrypoint implements ControlifyEntrypoint {
    private final ControlifyEntrypoint minisort = Platform.isNeoForge() ? new MiniSortControlify() : null;

    @Override
    public void onControlifyPreInit(PreInitContext context) {
        if (minisort != null) {
            minisort.onControlifyPreInit(context);
        }
    }

    @Override
    public void onControlifyInit(InitContext context) {
        if (minisort != null) {
            minisort.onControlifyInit(context);
        }
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
        if (minisort != null) {
            minisort.onControllersDiscovered(controlify);
        }
    }
}
