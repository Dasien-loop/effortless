package dev.huskuraft.effortless.networking.packets.player;

import java.util.List;
import java.util.UUID;

import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetByteBuf;
import dev.huskuraft.universal.api.networking.NetByteBufSerializer;
import dev.huskuraft.universal.api.networking.Packet;
import dev.huskuraft.effortless.networking.packets.AllPacketListener;

/** Server-authoritative counts for materials used by one preview. */
public record PlayerMaterialSnapshotPacket(
        UUID playerId,
        UUID contextId,
        List<ItemStack> items
) implements Packet<AllPacketListener> {

    @Override
    public void handle(AllPacketListener packetListener, Player sender) {
        packetListener.handle(this, sender);
    }

    public static class Serializer implements NetByteBufSerializer<PlayerMaterialSnapshotPacket> {
        @Override
        public PlayerMaterialSnapshotPacket read(NetByteBuf byteBuf) {
            return new PlayerMaterialSnapshotPacket(
                    byteBuf.readUUID(),
                    byteBuf.readUUID(),
                    byteBuf.readList(NetByteBuf::readItemStack)
            );
        }

        @Override
        public void write(NetByteBuf byteBuf, PlayerMaterialSnapshotPacket packet) {
            byteBuf.writeUUID(packet.playerId());
            byteBuf.writeUUID(packet.contextId());
            byteBuf.writeList(packet.items(), NetByteBuf::writeItemStack);
        }
    }
}
