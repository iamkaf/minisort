package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.amber.api.networking.v1.PacketDecoder;
import com.iamkaf.amber.api.networking.v1.PacketEncoder;
import com.iamkaf.amber.api.networking.v1.PacketHandler;
import com.iamkaf.minisort.transfer.TransferService;
import net.minecraft.server.level.ServerPlayer;

public record TransferContainerPayload(int containerId, Action action)
        implements Packet<TransferContainerPayload> {
    public static final PacketEncoder<TransferContainerPayload> ENCODER = (packet, buffer) -> {
        buffer.writeInt(packet.containerId);
        buffer.writeByte(packet.action.networkId);
    };

    public static final PacketDecoder<TransferContainerPayload> DECODER = buffer -> new TransferContainerPayload(
            buffer.readInt(),
            Action.fromNetworkId(buffer.readUnsignedByte())
    );

    public static final PacketHandler<TransferContainerPayload> HANDLER = (packet, context) -> {
        if (!context.isServerSide()) {
            return;
        }
        context.execute(() -> {
            ServerPlayer player = context.getServerPlayer();
            if (player != null) {
                TransferService.transfer(player, packet);
            }
        });
    };

    public enum Action {
        /** Main inventory stacks of items the container already holds, hotbar excluded. */
        DEPOSIT_MATCHING(0, true, true),
        RETRIEVE_MATCHING(1, false, true),
        /** Shift-click on Deposit: the whole main inventory, hotbar excluded. */
        DEPOSIT_ALL(2, true, false),
        /** Shift-click on Retrieve: everything that fits. */
        RETRIEVE_ALL(3, false, false);

        private final int networkId;
        private final boolean deposits;
        private final boolean matchingOnly;

        Action(int networkId, boolean deposits, boolean matchingOnly) {
            this.networkId = networkId;
            this.deposits = deposits;
            this.matchingOnly = matchingOnly;
        }

        public boolean deposits() {
            return deposits;
        }

        public boolean matchingOnly() {
            return matchingOnly;
        }

        private static Action fromNetworkId(int networkId) {
            for (Action action : values()) {
                if (action.networkId == networkId) {
                    return action;
                }
            }
            throw new IllegalArgumentException("Unknown transfer action");
        }
    }
}
