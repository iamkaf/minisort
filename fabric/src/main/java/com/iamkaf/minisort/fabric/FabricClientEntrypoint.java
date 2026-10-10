package com.iamkaf.minisort.fabric;

import com.iamkaf.minisort.client.MiniSortClient;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MiniSortClient.init();
    }
}
