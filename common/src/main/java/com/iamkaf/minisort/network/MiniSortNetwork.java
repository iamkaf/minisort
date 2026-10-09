package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.NetworkChannel;
import com.iamkaf.amber.api.networking.v1.PeerAvailability;
import com.iamkaf.minisort.MiniSort;
import com.iamkaf.minisort.sort.SortTarget;
import net.minecraft.world.item.Item;

import java.util.List;

public final class MiniSortNetwork {
    // Optional, so players with Minisort can still join servers without it.
    private static final NetworkChannel CHANNEL = NetworkChannel.createOptional(MiniSort.resource("main_v1"));
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

    /** Whether the connected server has Minisort to act on the buttons. Client side only. */
    public static boolean serverSupported() {
        return CHANNEL.serverAvailability() == PeerAvailability.SUPPORTED;
    }

    public static void sort(int containerId, SortTarget target, List<Item> order) {
        CHANNEL.sendToServer(new SortContainerPayload(containerId, target, order));
    }

    public static void transfer(int containerId, TransferContainerPayload.Action action) {
        CHANNEL.sendToServer(new TransferContainerPayload(containerId, action));
    }
}
