package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.ResourceKey;
import dev.huskuraft.universal.api.core.ResourceLocation;

public record MinecraftResourceKey<T>(
        net.minecraft.resources.ResourceKey<?> refs
) implements ResourceKey<T> {

    @Override
    public ResourceLocation registry() {
        return new MinecraftResourceLocation(refs.registry());
    }

    @Override
    public ResourceLocation location() {
        return new MinecraftResourceLocation(refs.identifier());
    }

}
