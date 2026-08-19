package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.Block;
import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.BucketItem;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.core.fluid.Fluid;
import dev.huskuraft.universal.api.text.Text;

public record MinecraftBucketItem(net.minecraft.world.item.BucketItem refs) implements BucketItem {

    @Override
    public Fluid getContent() {
        return MinecraftFluid.ofNullable(refs.content);
    }

    @Override
    public boolean useContent(World world, Player player, BlockPosition blockPosition, BlockInteraction blockInteraction) {
        return refs.emptyContents(player.reference(), world.reference(), MinecraftConvertor.toPlatformBlockPosition(blockPosition), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction));
    }

    @Override
    public void useExtraContent(World world, Player player, BlockPosition blockPosition, ItemStack itemStack) {
        refs.checkExtraContent(player.reference(), world.reference(), itemStack.reference(), MinecraftConvertor.toPlatformBlockPosition(blockPosition));
    }

    @Override
    public ItemStack getDefaultStack() {
        return new MinecraftItem(refs).getDefaultStack();
    }

    @Override
    public Block getBlock() {
        return new MinecraftItem(refs).getBlock();
    }

    @Override
    public ResourceLocation getId() {
        return new MinecraftItem(refs).getId();
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState blockState) {
        return new MinecraftItem(refs).isCorrectToolForDrops(blockState);
    }

    @Override
    public int getMaxStackSize() {
        return new MinecraftItem(refs).getMaxStackSize();
    }

    @Override
    public int getMaxDamage() {
        return new MinecraftItem(refs).getMaxDamage();
    }

    @Override
    public boolean mineBlock(World world, Player player, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return new MinecraftItem(refs).mineBlock(world, player, blockPosition, blockState, itemStack);
    }

    @Override
    public Text getName(ItemStack itemStack) {
        return new MinecraftItem(refs).getName(itemStack);
    }

}
