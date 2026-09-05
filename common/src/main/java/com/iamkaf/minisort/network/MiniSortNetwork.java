package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.NetworkChannel;
import com.iamkaf.minisort.MiniSort;

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

    public static void sortContainer(int containerId) {
        CHANNEL.sendToServer(new SortContainerPayload(
                containerId,
                SortContainerPayload.SortTarget.CONTAINER,
                SortContainerPayload.SortMode.REGISTRY_ID
        ));
    }

    public static void depositMatching(int containerId) {
        transfer(containerId, TransferContainerPayload.Action.DEPOSIT_MATCHING);
    }

    public static void retrieveMatching(int containerId) {
        transfer(containerId, TransferContainerPayload.Action.RETRIEVE_MATCHING);
    }

    private static void transfer(int containerId, TransferContainerPayload.Action action) {
        CHANNEL.sendToServer(new TransferContainerPayload(containerId, action));
    }
}
