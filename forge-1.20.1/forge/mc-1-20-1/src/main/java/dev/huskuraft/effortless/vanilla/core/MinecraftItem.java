package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.Block;
import dev.huskuraft.universal.api.core.BlockInteraction;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.InteractionResult;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.text.Text;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.context.UseOnContext;

public record MinecraftItem(net.minecraft.world.item.Item refs) implements Item {

    public static Item ofNullable(net.minecraft.world.item.Item refs) {
        if (refs == null) return null;
        if (refs instanceof BlockItem blockItem) return new MinecraftBlockItem(blockItem);
        if (refs instanceof DiggerItem diggerItem) return new MinecraftDiggerItem(diggerItem);
        if (refs instanceof BucketItem bucketItem) return new MinecraftBucketItem(bucketItem);
        return new MinecraftItem(refs);
    }

    @Override
    public ItemStack getDefaultStack() {
        return new MinecraftItemStack(refs.getDefaultInstance());
    }

    @Override
    public Block getBlock() {
        return new MinecraftBlock(net.minecraft.world.level.block.Block.byItem(refs));
    }

    @Override
    public ResourceLocation getId() {
        var minecraftResourceLocation = BuiltInRegistries.ITEM.getKey(refs);
        return new MinecraftResourceLocation(minecraftResourceLocation);
    }

    @Override
    public InteractionResult useOnBlock(Player player, BlockInteraction blockInteraction) {
        return MinecraftConvertor.toPlatformInteractionResult(refs.useOn(new UseOnContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction))));
    }

    @Override
    public int getMaxStackSize() {
        return refs.getMaxStackSize();
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState blockState) {
        return refs.isCorrectToolForDrops(blockState.reference());
    }

    @Override
    public boolean isSuitableForMining(BlockState blockState) {
        return refs.getDestroySpeed(refs.getDefaultInstance(), blockState.reference()) > 1.0F;
    }

    @Override
    public int getMaxDamage() {
        return refs.getMaxDamage();
    }

    @Override
    public boolean mineBlock(World world, Player player, BlockPosition blockPosition, BlockState blockState, ItemStack itemStack) {
        return refs.mineBlock(itemStack.reference(), world.reference(), blockState.reference(), MinecraftConvertor.toPlatformBlockPosition(blockPosition), player.reference());
    }

    @Override
    public Text getName(ItemStack itemStack) {
        return new MinecraftText(refs.getName(itemStack.reference()));
    }

}
