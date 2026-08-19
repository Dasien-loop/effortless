package dev.huskuraft.effortless.forge.events;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.events.CommonEventRegistry;
import dev.huskuraft.effortless.vanilla.core.MinecraftPlayer;
import dev.huskuraft.effortless.vanilla.core.MinecraftServer;
import dev.huskuraft.effortless.vanilla.core.MinecraftWorld;
import dev.huskuraft.effortless.forge.platform.ForgeInitializer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@AutoService(CommonEventRegistry.class)
public class ForgeCommonEventRegistry extends CommonEventRegistry {


    public ForgeCommonEventRegistry() {
        ForgeInitializer.getModEventBus().addListener(this::onCommonSetup);

        NeoForge.EVENT_BUS.register(this);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        // Network payloads are registered on RegisterPayloadHandlersEvent.
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        getPlayerChangeWorldEvent().invoker().onPlayerChangeWorld(MinecraftPlayer.ofNullable(event.getEntity()), MinecraftWorld.ofNullable(event.getEntity().getServer().getLevel(event.getFrom())), MinecraftWorld.ofNullable(event.getEntity().getServer().getLevel(event.getTo())));
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        getPlayerRespawnEvent().invoker().onPlayerRespawn(MinecraftPlayer.ofNullable(event.getEntity()), MinecraftPlayer.ofNullable(event.getEntity()), event.isEndConquered());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        getPlayerLoggedInEvent().invoker().onPlayerLoggedIn(MinecraftPlayer.ofNullable(event.getEntity()));
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        getPlayerLoggedOutEvent().invoker().onPlayerLoggedOut(MinecraftPlayer.ofNullable(event.getEntity()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        getServerStartingEvent().invoker().onServerStarting(new MinecraftServer(event.getServer()));
    }

    @SubscribeEvent
    public void onSeverrStarted(ServerStartedEvent event) {
        getServerStartedEvent().invoker().onServerStarted(new MinecraftServer(event.getServer()));
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        getServerStoppingEvent().invoker().onServerStopping(new MinecraftServer(event.getServer()));
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        getServerStoppedEvent().invoker().onServerStopped(new MinecraftServer(event.getServer()));
    }

}
