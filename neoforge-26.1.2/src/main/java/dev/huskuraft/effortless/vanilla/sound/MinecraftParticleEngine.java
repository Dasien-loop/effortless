package dev.huskuraft.effortless.vanilla.sound;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.Direction;
import dev.huskuraft.universal.api.platform.ParticleEngine;
import dev.huskuraft.effortless.vanilla.core.MinecraftConvertor;

public record MinecraftParticleEngine(
        net.minecraft.client.particle.ParticleEngine refs
) implements ParticleEngine {

    @Override
    public void destroy(BlockPosition blockPosition, BlockState blockState) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            level.addDestroyBlockEffect(MinecraftConvertor.toPlatformBlockPosition(blockPosition), blockState.reference());
        }
    }

    @Override
    public void crack(BlockPosition blockPosition, Direction direction) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            var state = level.getBlockState(MinecraftConvertor.toPlatformBlockPosition(blockPosition));
            var particle = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state);
            net.minecraft.util.ParticleUtils.spawnParticleOnFace(
                    level,
                    MinecraftConvertor.toPlatformBlockPosition(blockPosition),
                    MinecraftConvertor.toPlatformDirection(direction),
                    particle,
                    net.minecraft.world.phys.Vec3.ZERO,
                    0.1
            );
        }
    }

}
