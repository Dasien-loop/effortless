package dev.huskuraft.effortless.vanilla.core;

import java.util.List;
import java.util.stream.Collectors;

import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.tag.RecordTag;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.effortless.vanilla.tag.MinecraftRecordTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

public record MinecraftItemStack(
        net.minecraft.world.item.ItemStack refs
) implements ItemStack {

    public static ItemStack ofNullable(net.minecraft.world.item.ItemStack refs) {
        if (refs == null) return null;
        return new MinecraftItemStack(refs);
    }

    @Override
    public Item getItem() {
        return MinecraftItem.ofNullable(refs.getItem());
    }

    @Override
    public int getCount() {
        return refs.getCount();
    }

    @Override
    public void setCount(int count) {
        refs.setCount(count);
    }

    @Override
    public Text getHoverName() {
        return new MinecraftText(refs.getHoverName());
    }

    @Override
    public List<Text> getTooltips(Player player, TooltipType flag) {
        var minecraftFlag = switch (flag) {
            case NORMAL -> TooltipFlag.NORMAL;
            case NORMAL_CREATIVE -> TooltipFlag.NORMAL.asCreative();
            case ADVANCED -> TooltipFlag.ADVANCED;
            case ADVANCED_CREATIVE -> TooltipFlag.ADVANCED.asCreative();
        };
        return refs.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, player.reference(), minecraftFlag).stream().map(text -> new MinecraftText(text)).collect(Collectors.toList());
    }

    @Override
    public ItemStack copy() {
        return new MinecraftItemStack(refs.copy());
    }

    @Override
    public RecordTag getTag() {
        var customData = refs.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : MinecraftRecordTag.ofNullable(customData.copyTag());
    }

    @Override
    public void setTag(RecordTag recordTag) {
        if (recordTag == null) {
            refs.remove(DataComponents.CUSTOM_DATA);
        } else {
            refs.set(DataComponents.CUSTOM_DATA, CustomData.of(recordTag.reference()));
        }
    }

    @Override
    public boolean damageBy(Player player, int damage) {
        if (!refs.isDamageableItem() || damage <= 0) {
            return false;
        }
        if (player.reference() instanceof ServerPlayer serverPlayer) {
            var previousCount = refs.getCount();
            refs.hurtAndBreak(damage, serverPlayer.level(), serverPlayer, ignored -> {
            });
            return refs.getCount() < previousCount;
        }
        var nextDamage = refs.getDamageValue() + damage;
        refs.setDamageValue(nextDamage);
        return nextDamage >= refs.getMaxDamage();
    }

    @Override
    public int getDamageValue() { return refs.getDamageValue(); }

    @Override
    public void setDamageValue(int damage) { refs.setDamageValue(damage); }

    @Override
    public int getMaxDamage() { return refs.getMaxDamage(); }

    @Override
    public boolean isDamageableItem() { return refs.isDamageableItem(); }
}
