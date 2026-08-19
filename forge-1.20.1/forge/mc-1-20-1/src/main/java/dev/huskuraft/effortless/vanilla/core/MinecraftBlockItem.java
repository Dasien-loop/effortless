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
import net.minecraft.world.item.context.BlockPlaceContext;

public record MinecraftBlockItem(net.minecraft.world.item.BlockItem refs) implements BlockItem {

    @Override
    public BlockState getPlacementState(Player player, BlockInteraction interaction) {
        var context = new BlockPlaceContext(
                player.reference(),
                MinecraftConvertor.toPlatformInteractionHand(interaction.getHand()),
                player.getItemStack(interaction.getHand()).reference(),
                MinecraftConvertor.toPlatformBlockInteraction(interaction)
        );
        try {
            var method = refs.getClass().getDeclaredMethod("getPlacementState", BlockPlaceContext.class);
            method.setAccessible(true);
            return MinecraftBlockState.ofNullable((net.minecraft.world.level.block.state.BlockState) method.invoke(refs, context));
        } catch (ReflectiveOperationException ignored) {
            // Forge's official mappings expose the inherited method with its
            // runtime name in production jars.
        }
        try {
            var method = refs.getClass().getDeclaredMethod("m_5965_", BlockPlaceContext.class);
            method.setAccessible(true);
            return MinecraftBlockState.ofNullable((net.minecraft.world.level.block.state.BlockState) method.invoke(refs, context));
        } catch (ReflectiveOperationException ignored) {
            return MinecraftBlockState.ofNullable(refs.getBlock().defaultBlockState());
        }
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
    public InteractionResult useOnBlock(Player player, BlockInteraction blockInteraction) {
        return new MinecraftItem(refs).useOnBlock(player, blockInteraction);
    }

    @Override
    public InteractionResult placeOnBlock(Player player, BlockInteraction blockInteraction) {
        return MinecraftConvertor.toPlatformInteractionResult(refs.place(new BlockPlaceContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), player.getItemStack(blockInteraction.getHand()).reference(), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction))));
    }

    @Override
    public boolean setBlockInWorld(World world, Player player, BlockInteraction blockInteraction, BlockState blockState) {
        return refs.placeBlock(new BlockPlaceContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), player.getItemStack(blockInteraction.getHand()).reference(), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction)), blockState.reference());
    }

    /**
     * The builder has already resolved the target state and calls the block's
     * placement callback afterwards.  Re-running Forge's BlockPlaceContext
     * validation here rejects the synthetic multi-block hit result, so write
     * the resolved state with normal world update flags instead.
     */
    @Override
    public boolean setBlockOnly(World world, Player player, BlockInteraction blockInteraction, BlockState blockState) {
        return world.setBlockAndUpdate(blockInteraction.getBlockPosition(), blockState);
    }

    @Override
    public boolean updateBlockEntityTag(World world, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return refs.updateCustomBlockEntityTag(MinecraftConvertor.toPlatformBlockPosition(blockPosition), world.reference(), null, itemStack.reference(), blockState.reference());
    }

    @Override
    public BlockState updateBlockStateFromTag(World world, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return MinecraftBlockState.ofNullable(refs.updateBlockStateFromTag(MinecraftConvertor.toPlatformBlockPosition(blockPosition), world.reference(), itemStack.reference(), blockState.reference()));
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
