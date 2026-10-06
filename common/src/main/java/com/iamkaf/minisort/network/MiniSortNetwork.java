package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.NetworkChannel;
import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.sort.SortMode;
import com.iamkaf.minisort.sort.SortTarget;

public final class MiniSortNetwork {
    private static final NetworkChannel CHANNEL = NetworkChannel.create(MiniSort.resource("main"));
    private static boolean initialized;

    private MiniSortNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.register(
                SortContainerPayload.class,
                SortContainerPayload.ENCODER,
                SortContainerPayload.DECODER,
                SortContainerPayload.HANDLER
        );
        CHANNEL.register(
                TransferContainerPayload.class,
                TransferContainerPayload.ENCODER,
                TransferContainerPayload.DECODER,
                TransferContainerPayload.HANDLER
        );
    }

    public static void sort(int containerId, SortTarget target, SortMode mode) {
        CHANNEL.sendToServer(new SortContainerPayload(containerId, target, mode));
    }

    public static void transfer(int containerId, TransferContainerPayload.Action action) {
        CHANNEL.sendToServer(new TransferContainerPayload(containerId, action));
    }
}
