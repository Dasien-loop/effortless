package dev.huskuraft.effortless.forge.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.platform.Environment;
import dev.huskuraft.universal.api.platform.LoaderType;
import dev.huskuraft.universal.api.platform.Mod;
import dev.huskuraft.universal.api.platform.Platform;
import net.neoforged.fml.loading.FMLLoader;

@AutoService(Platform.class)
public class ForgePlatform implements Platform {

    @Override
    public LoaderType getLoaderType() {
        return LoaderType.FORGE;
    }

    @Override
    public String getLoaderVersion() {
        return FMLLoader.getCurrent().getVersionInfo().neoForgeVersion();
    }

    @Override
    public String getGameVersion() {
        return FMLLoader.getCurrent().getVersionInfo().mcVersion();
    }

    @Override
    public List<Mod> getRunningMods() {
        return FMLLoader.getCurrent().getLoadingModList().getMods().stream().map(ForgeMod::new).collect(Collectors.toList());
    }

    @Override
    public Path getGameDir() {
        return FMLLoader.getCurrent().getGameDir();
    }

    @Override
    public Environment getEnvironment() {
        return switch (FMLLoader.getCurrent().getDist()) {
            case CLIENT -> Environment.CLIENT;
            case DEDICATED_SERVER -> Environment.SERVER;
        };
    }

    @Override
    public boolean isDevelopment() {
        return !FMLLoader.getCurrent().isProduction();
    }


}
