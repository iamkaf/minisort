package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.amber.api.networking.v1.PacketDecoder;
import com.iamkaf.amber.api.networking.v1.PacketEncoder;
import com.iamkaf.amber.api.networking.v1.PacketHandler;
import com.iamkaf.minisort.sort.SortMode;
import com.iamkaf.minisort.sort.SortService;
import net.minecraft.server.level.ServerPlayer;

public record SortContainerPayload(int containerId, SortMode mode) implements Packet<SortContainerPayload> {
    private static final SortMode[] MODES = SortMode.values();

    public static final PacketEncoder<SortContainerPayload> ENCODER = (packet, buffer) -> {
        buffer.writeInt(packet.containerId);
        buffer.writeByte(packet.mode.ordinal());
    };

    public static final PacketDecoder<SortContainerPayload> DECODER = buffer -> {
        int containerId = buffer.readInt();
        int mode = buffer.readUnsignedByte();
        if (mode >= MODES.length) {
            throw new IllegalArgumentException("Unknown sort mode " + mode);
        }
        return new SortContainerPayload(containerId, MODES[mode]);
    };

    public static final PacketHandler<SortContainerPayload> HANDLER = (packet, context) -> {
        if (!context.isServerSide()) {
            return;
        }
        context.execute(() -> {
            ServerPlayer player = context.getServerPlayer();
            if (player != null) {
                SortService.sort(player, packet);
            }
        });
    };
}
