package dev.huskuraft.effortless.forge.networking;

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.CopyOnWriteArrayList;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.networking.NetByteBuf;
import dev.huskuraft.universal.api.networking.NetByteBufReceiver;
import dev.huskuraft.universal.api.networking.Networking;
import dev.huskuraft.effortless.vanilla.core.MinecraftPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

@AutoService(Networking.class)
public class ForgeNetworking implements Networking {

    public static final EventNetworkChannel CHANNEL;

    static {
        CHANNEL = NetworkRegistry.ChannelBuilder.named(Networking.getChannelId().reference())
                .clientAcceptedVersions(Networking.getCompatibilityVersionStr()::equals)
                .serverAcceptedVersions(Networking.getCompatibilityVersionStr()::equals)
                .networkProtocolVersion(Networking::getCompatibilityVersionStr)
                .eventNetworkChannel();

        // Attach the channel listeners before Forge starts locking network
        // registration. Receivers themselves are added during common/client
        // setup, but the event listener must already exist when login packets
        // begin to arrive.
        CHANNEL.addListener(event -> {
            if (event.getPayload() == null
                    || event.getSource().get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                return;
            }
            var player = MinecraftPlayer.ofNullable(Minecraft.getInstance().player);
            for (var receiver : ClientNetworking.RECEIVERS) {
                receiver.receiveBuffer(new NetByteBuf(event.getPayload().copy()), player);
            }
            event.getSource().get().setPacketHandled(true);
        });
        CHANNEL.addListener(event -> {
            if (event.getPayload() == null
                    || event.getSource().get().getDirection() != NetworkDirection.PLAY_TO_SERVER) {
                return;
            }
            var player = MinecraftPlayer.ofNullable(event.getSource().get().getSender());
            for (var receiver : ServerNetworking.RECEIVERS) {
                receiver.receiveBuffer(new NetByteBuf(event.getPayload().copy()), player);
            }
            event.getSource().get().setPacketHandled(true);
        });
    }

    @Override
    public void registerClientReceiver(NetByteBufReceiver receiver) {
        ClientNetworking.registerReceiver(receiver);
    }

    @Override
    public void registerServerReceiver(NetByteBufReceiver receiver) {
        ServerNetworking.registerReceiver(receiver);
    }

    @Override
    public void sendToClient(NetByteBuf byteBuf, Player player) {
        ServerNetworking.send(byteBuf, player);
    }

    @Override
    public void sendToServer(NetByteBuf byteBuf, Player player) {
        ClientNetworking.send(byteBuf, player);
    }

    static class ClientNetworking {
        private static final CopyOnWriteArrayList<NetByteBufReceiver> RECEIVERS = new CopyOnWriteArrayList<>();

        public static void registerReceiver(NetByteBufReceiver receiver) {
            RECEIVERS.addIfAbsent(receiver);
        }

        public static void send(NetByteBuf byteBuf, Player player) {
            var minecraftPacket = NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(new FriendlyByteBuf(byteBuf), 0), Networking.getChannelId().reference()).getThis();
            Minecraft.getInstance().getConnection().send(minecraftPacket);
        }
    }

    static class ServerNetworking {
        private static final CopyOnWriteArrayList<NetByteBufReceiver> RECEIVERS = new CopyOnWriteArrayList<>();

        public static void registerReceiver(NetByteBufReceiver receiver) {
            RECEIVERS.addIfAbsent(receiver);
        }

        public static void send(NetByteBuf byteBuf, Player player) {
            var minecraftPacket = NetworkDirection.PLAY_TO_CLIENT.buildPacket(Pair.of(new FriendlyByteBuf(byteBuf), 0), Networking.getChannelId().reference()).getThis();
            ((ServerPlayer) player.reference()).connection.send(minecraftPacket);
        }
    }

}
