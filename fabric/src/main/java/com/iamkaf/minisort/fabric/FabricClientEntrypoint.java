package com.iamkaf.minisort.fabric;

import com.iamkaf.minisort.client.ClientConfig;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.init();
    }
}
