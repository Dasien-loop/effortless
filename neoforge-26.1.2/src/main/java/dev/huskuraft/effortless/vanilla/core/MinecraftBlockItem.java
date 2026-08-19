package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.Block;
import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.BlockItem;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.InteractionResult;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.text.Text;

public record MinecraftBlockItem(net.minecraft.world.item.BlockItem refs) implements BlockItem {

    public static net.minecraft.world.level.block.state.BlockState getPlacementState(
            net.minecraft.world.item.BlockItem refs,
            net.minecraft.world.item.context.BlockPlaceContext blockPlaceContext
    ) {
        try {
            var getPlacementStateMethod = refs.getClass().getDeclaredMethod("getPlacementState", net.minecraft.world.item.context.BlockPlaceContext.class);
            getPlacementStateMethod.setAccessible(true);
            return (net.minecraft.world.level.block.state.BlockState) getPlacementStateMethod.invoke(refs, blockPlaceContext);
        } catch (Exception ignored) {
        }
        try {
            var getPlacementStateMethod = refs.getClass().getDeclaredMethod("m_5965_", net.minecraft.world.item.context.BlockPlaceContext.class);
            getPlacementStateMethod.setAccessible(true);
            return (net.minecraft.world.level.block.state.BlockState) getPlacementStateMethod.invoke(refs, blockPlaceContext);
        } catch (Exception ignored) {
        }
        throw new RuntimeException("Failed to invoke getPlacementState");
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
    public BlockState getPlacementState(Player player, BlockInteraction interaction) {
        return MinecraftBlockState.ofNullable(refs.getPlacementState(MinecraftConvertor.toPlatformBlockPlaceContext(player, interaction)));
    }

    @Override
    public ResourceLocation getId() {
        return new MinecraftItem(refs).getId();
    }

    @Override
    public InteractionResult useOnBlock(Player player, BlockInteraction blockInteraction) {
        return new MinecraftItem(refs).useOnBlock(player, blockInteraction);
    }

    @Override
    public InteractionResult placeOnBlock(Player player, BlockInteraction blockInteraction) {
        return MinecraftConvertor.fromPlatformInteractionResult(refs.place(new net.minecraft.world.item.context.BlockPlaceContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), player.getItemStack(blockInteraction.getHand()).reference(), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction))));
    }

    @Override
    public boolean setBlockInWorld(World world, Player player, BlockInteraction blockInteraction, BlockState blockState) {
        return refs.placeBlock(MinecraftConvertor.toPlatformBlockPlaceContext(player, blockInteraction), blockState.reference());
    }

    @Override
    public boolean updateBlockEntityTag(World world, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return refs.updateCustomBlockEntityTag(
                MinecraftConvertor.toPlatformBlockPosition(blockPosition),
                world.reference(),
                null,
                itemStack.reference(),
                blockState.reference()
        );
    }

    @Override
    public BlockState updateBlockStateFromTag(World world, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return MinecraftBlockState.ofNullable(refs.updateBlockStateFromTag(
                MinecraftConvertor.toPlatformBlockPosition(blockPosition),
                world.reference(),
                itemStack.reference(),
                blockState.reference()
        ));
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
