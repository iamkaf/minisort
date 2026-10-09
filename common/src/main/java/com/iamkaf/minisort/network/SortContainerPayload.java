package com.iamkaf.minisort.network;

import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.amber.api.networking.v1.PacketDecoder;
import com.iamkaf.amber.api.networking.v1.PacketEncoder;
import com.iamkaf.amber.api.networking.v1.PacketHandler;
import com.iamkaf.minisort.sort.SortService;
import com.iamkaf.minisort.sort.SortTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts a container or the player's inventory. The client sends its sort order as the items in the open menu,
 * first to last, so the player's sort mode and the creative tabs it reads stay on the client.
 */
public record SortContainerPayload(int containerId, SortTarget target, List<Item> order)
        implements Packet<SortContainerPayload> {
    private static final SortTarget[] TARGETS = SortTarget.values();
    // A menu holds at most a few hundred distinct items; anything longer is not from Minisort.
    private static final int MAX_ORDER = 1024;

    public SortContainerPayload {
        order = List.copyOf(order);
    }

    public static final PacketEncoder<SortContainerPayload> ENCODER = (packet, buffer) -> {
        buffer.writeInt(packet.containerId);
        buffer.writeByte(packet.target.ordinal());
        buffer.writeVarInt(packet.order.size());
        for (Item item : packet.order) {
            buffer.writeVarInt(BuiltInRegistries.ITEM.getId(item));
        }
    };

    public static final PacketDecoder<SortContainerPayload> DECODER = buffer -> {
        int containerId = buffer.readInt();
        int target = buffer.readUnsignedByte();
        if (target >= TARGETS.length) {
            throw new IllegalArgumentException("Unknown sort target " + target);
        }
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ORDER) {
            throw new IllegalArgumentException("Sort order too long: " + size);
        }
        List<Item> order = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            order.add(BuiltInRegistries.ITEM.byId(buffer.readVarInt()));
        }
        return new SortContainerPayload(containerId, TARGETS[target], order);
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
