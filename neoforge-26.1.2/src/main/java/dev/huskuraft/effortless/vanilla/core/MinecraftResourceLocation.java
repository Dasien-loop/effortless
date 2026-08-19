package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.ResourceLocation;

public record MinecraftResourceLocation(
        net.minecraft.resources.Identifier refs
) implements ResourceLocation {

    public static ResourceLocation ofNullable(net.minecraft.resources.Identifier refs) {
        if (refs == null) return null;
        return new MinecraftResourceLocation(refs);
    }

    @Override
    public String getNamespace() {
        return refs.getNamespace();
    }

    @Override
    public String getPath() {
        return refs.getPath();
    }

    @Override
    public String toString() {
        return getNamespace() + ":" + getPath();
    }
}
