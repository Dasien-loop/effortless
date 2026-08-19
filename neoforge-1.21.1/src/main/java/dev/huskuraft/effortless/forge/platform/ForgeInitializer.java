package dev.huskuraft.effortless.forge.platform;

import dev.huskuraft.effortless.Effortless;
import dev.huskuraft.effortless.forge.networking.ForgeNetworking;
import dev.huskuraft.universal.api.networking.Networking;
import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.platform.PlatformLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(Effortless.MOD_ID)
public class ForgeInitializer {

    private static IEventBus modEventBus;

    public ForgeInitializer(IEventBus eventBus) {
        modEventBus = eventBus;
        eventBus.addListener(ForgeNetworking::registerPayloadHandlers);

        var networking = PlatformLoader.getSingleton(Networking.class);
        var entrance = Entrance.getInstance();
        networking.registerServerReceiver(entrance.getChannel()::receiveBuffer);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            var clientEntrance = ClientEntrance.getInstance();
            networking.registerClientReceiver(clientEntrance.getChannel()::receiveBuffer);
        }
    }

    public static IEventBus getModEventBus() {
        if (modEventBus == null) {
            throw new IllegalStateException("NeoForge mod event bus is not initialized");
        }
        return modEventBus;
    }

}
