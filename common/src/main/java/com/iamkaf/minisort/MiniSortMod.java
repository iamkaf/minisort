package com.iamkaf.minisort;

import com.iamkaf.amber.api.core.v2.AmberInitializer;
import com.iamkaf.amber.api.event.v1.events.common.ServerTickEvents;
import com.iamkaf.minisort.client.ButtonConfig;
import com.iamkaf.minisort.network.MiniSortNetwork;
import com.iamkaf.minisort.refill.RefillConfig;
import com.iamkaf.minisort.refill.RefillQueue;

public final class MiniSortMod {
    private static boolean initialized;

    private MiniSortMod() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        AmberInitializer.initialize(MiniSort.MOD_ID);
        ButtonConfig.init();
        RefillConfig.init();
        ServerTickEvents.END_SERVER_TICK.register(RefillQueue::drain);
        MiniSortNetwork.init();
        MiniSort.LOG.info("Initialized {}", MiniSort.MOD_NAME);
    }
}
