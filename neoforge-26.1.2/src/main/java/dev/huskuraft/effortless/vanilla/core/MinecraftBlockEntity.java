package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.BlockEntity;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.tag.RecordTag;
import dev.huskuraft.effortless.vanilla.tag.MinecraftRecordTag;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public record MinecraftBlockEntity(net.minecraft.world.level.block.entity.BlockEntity refs) implements BlockEntity {

    public static BlockEntity ofNullable(net.minecraft.world.level.block.entity.BlockEntity refs) {
        if (refs == null) return null;
        if (refs instanceof BaseContainerBlockEntity baseContainerBlockEntity) return new MinecraftContainerBlockEntity(baseContainerBlockEntity);
        return new MinecraftBlockEntity(refs);
    }

    @Override
    public BlockState getBlockState() {
        return MinecraftBlockState.ofNullable(refs.getBlockState());
    }

    @Override
    public BlockPosition getBlockPosition() {
        return MinecraftConvertor.toPlatformBlockPosition(refs.getBlockPos());
    }

    @Override
    public World getWorld() {
        return MinecraftWorld.ofNullable(refs.getLevel());
    }

    @Override
    public RecordTag getTag() {
        var registries = refs.getLevel() == null ? net.minecraft.core.RegistryAccess.EMPTY : refs.getLevel().registryAccess();
        return MinecraftRecordTag.ofNullable(refs.saveWithoutMetadata(registries));
    }

    @Override
    public void setTag(RecordTag recordTag) {
        var registries = refs.getLevel() == null ? net.minecraft.core.RegistryAccess.EMPTY : refs.getLevel().registryAccess();
        var tag = (net.minecraft.nbt.CompoundTag) recordTag.refs();
        refs.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,
                registries,
                tag
        ));
    }
}
