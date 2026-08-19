package dev.huskuraft.effortless.forge.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;

import dev.huskuraft.effortless.building.CuriosStacks;

/** Direct Forge Curios API bridge. Kept loader-specific so Curios stays optional. */
public final class ForgeCuriosBridge {
    private ForgeCuriosBridge() {
    }

    public static List<CuriosStacks.LocatedStack> collectLocated(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> (List<CuriosStacks.LocatedStack>) scan(handler))
                .orElse(List.<CuriosStacks.LocatedStack>of());
    }

    private static List<CuriosStacks.LocatedStack> scan(
            top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler) {
            IItemHandler curios = handler.getEquippedCurios();
            var result = new ArrayList<CuriosStacks.LocatedStack>();
            // The aggregate handler can omit dynamically registered Curios
            // types in large packs. Curios' predicate query is authoritative
            // and preserves the identifier/index needed by wireless terminals.
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
            return result;
    }
}
