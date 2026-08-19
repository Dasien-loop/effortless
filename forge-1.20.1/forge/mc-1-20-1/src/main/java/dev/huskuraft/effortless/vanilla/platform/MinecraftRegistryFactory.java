package dev.huskuraft.effortless.vanilla.platform;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.Registry;
import dev.huskuraft.universal.api.platform.PlatformReference;
import dev.huskuraft.universal.api.platform.RegistryFactory;
import dev.huskuraft.effortless.vanilla.core.MinecraftBlockState;
import dev.huskuraft.effortless.vanilla.core.MinecraftItem;
import dev.huskuraft.effortless.vanilla.core.MinecraftRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

@AutoService(RegistryFactory.class)
public final class MinecraftRegistryFactory implements RegistryFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PlatformReference> Registry<T> getRegistry(Class<T> clazz) {
        if (clazz == Item.class) return (Registry<T>) new MinecraftRegistry<>(BuiltInRegistries.ITEM, MinecraftItem::new);
        if (clazz == BlockState.class) return (Registry<T>) new MinecraftRegistry<>(Block.BLOCK_STATE_REGISTRY, MinecraftBlockState::new);
        return null;
    }


}
