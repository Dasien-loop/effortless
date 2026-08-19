package dev.huskuraft.effortless.neoforge.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import dev.huskuraft.effortless.building.CuriosStacks;

/** Direct NeoForge 1.21.1 Curios API bridge; Curios remains optional at runtime. */
public final class NeoForgeCuriosBridge {
    private NeoForgeCuriosBridge() {
    }

    public static List<CuriosStacks.LocatedStack> collectLocated(Player player) {
        return CuriosApi.getCuriosInventory(player).map(NeoForgeCuriosBridge::scan)
                .orElse(List.<CuriosStacks.LocatedStack>of());
    }

    private static List<CuriosStacks.LocatedStack> scan(
            top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler) {
        IItemHandler curios = handler.getEquippedCurios();
        var result = new ArrayList<CuriosStacks.LocatedStack>();
        // Use Curios' predicate query first so AE2WTLib receives the exact
        // slot identifier/index needed to construct its CurioLocator.
        for (var found : handler.findCurios(stack -> !stack.isEmpty())) {
            var context = found.slotContext();
            result.add(new CuriosStacks.LocatedStack(found.stack(),
                    context.identifier(), context.index()));
        }
        if (!result.isEmpty()) {
            return result;
        }
        for (var slot = 0; slot < curios.getSlots(); slot++) {
            ItemStack stack = curios.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                result.add(new CuriosStacks.LocatedStack(stack, null, -1));
            }
        }
        // Curios integrations can expose dynamically-added slots through the
        // capability's predicate query even when the aggregate item handler
        // has not been synchronized yet.
        return result;
    }
}
