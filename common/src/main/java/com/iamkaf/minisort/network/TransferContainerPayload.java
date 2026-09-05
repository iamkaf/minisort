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
        DEPOSIT_MATCHING(0),
        RETRIEVE_MATCHING(1);

        private final int networkId;

        Action(int networkId) {
            this.networkId = networkId;
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
