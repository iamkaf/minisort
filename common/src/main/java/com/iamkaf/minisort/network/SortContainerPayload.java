package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.amber.api.networking.v1.PacketDecoder;
import com.iamkaf.amber.api.networking.v1.PacketEncoder;
import com.iamkaf.amber.api.networking.v1.PacketHandler;
import com.iamkaf.minisort.sort.SortMode;
import com.iamkaf.minisort.sort.SortService;
import com.iamkaf.minisort.sort.SortTarget;
import net.minecraft.server.level.ServerPlayer;

public record SortContainerPayload(int containerId, SortTarget target, SortMode mode)
        implements Packet<SortContainerPayload> {
    private static final SortTarget[] TARGETS = SortTarget.values();
    private static final SortMode[] MODES = SortMode.values();

    public static final PacketEncoder<SortContainerPayload> ENCODER = (packet, buffer) -> {
        buffer.writeInt(packet.containerId);
        buffer.writeByte(packet.target.ordinal());
        buffer.writeByte(packet.mode.ordinal());
    };

    public static final PacketDecoder<SortContainerPayload> DECODER = buffer -> {
        int containerId = buffer.readInt();
        int target = buffer.readUnsignedByte();
        int mode = buffer.readUnsignedByte();
        if (target >= TARGETS.length || mode >= MODES.length) {
            throw new IllegalArgumentException("Unknown sort target " + target + " or mode " + mode);
        }
        return new SortContainerPayload(containerId, TARGETS[target], MODES[mode]);
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
