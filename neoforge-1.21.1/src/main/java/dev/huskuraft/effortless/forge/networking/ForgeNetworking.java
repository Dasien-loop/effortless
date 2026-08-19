package dev.huskuraft.effortless.forge.networking;

import com.google.auto.service.AutoService;
import dev.huskuraft.effortless.Effortless;
import dev.huskuraft.effortless.vanilla.core.MinecraftPlayer;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetByteBuf;
import dev.huskuraft.universal.api.networking.NetByteBufReceiver;
import dev.huskuraft.universal.api.networking.Networking;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@AutoService(Networking.class)
public class ForgeNetworking implements Networking {

    private static volatile NetByteBufReceiver clientReceiver;
    private static volatile NetByteBufReceiver serverReceiver;

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(Integer.toString(Effortless.PROTOCOL_VERSION))
                .playBidirectional(EffortlessPayload.TYPE, EffortlessPayload.STREAM_CODEC, (payload, context) -> {
                    var receiver = context.flow() == PacketFlow.CLIENTBOUND ? clientReceiver : serverReceiver;
                    if (receiver == null) {
                        return;
                    }
                    context.enqueueWork(() -> receiver.receiveBuffer(
                            new NetByteBuf(Unpooled.wrappedBuffer(payload.data())),
                            MinecraftPlayer.ofNullable(context.player())
                    ));
                });
    }

    @Override
    public void registerClientReceiver(NetByteBufReceiver receiver) {
        clientReceiver = receiver;
    }

    @Override
    public void registerServerReceiver(NetByteBufReceiver receiver) {
        serverReceiver = receiver;
    }

    @Override
    public void sendToClient(NetByteBuf byteBuf, Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer) player.reference(), EffortlessPayload.from(byteBuf));
    }

    @Override
    public void sendToServer(NetByteBuf byteBuf, Player player) {
        PacketDistributor.sendToServer(EffortlessPayload.from(byteBuf));
    }

    private record EffortlessPayload(byte[] data) implements CustomPacketPayload {

        private static final Type<EffortlessPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Effortless.MOD_ID, Effortless.DEFAULT_CHANNEL)
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, EffortlessPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeByteArray(payload.data),
                buffer -> new EffortlessPayload(buffer.readByteArray())
        );

        private static EffortlessPayload from(NetByteBuf byteBuf) {
            var data = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), data);
            return new EffortlessPayload(data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
