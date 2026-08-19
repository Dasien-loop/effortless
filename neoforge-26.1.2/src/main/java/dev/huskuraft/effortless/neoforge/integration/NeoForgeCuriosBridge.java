package dev.huskuraft.effortless.neoforge.integration;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

import dev.huskuraft.effortless.building.CuriosStacks;

/** Direct NeoForge 26.1.2 Curios API bridge; Curios remains optional at runtime. */
public final class NeoForgeCuriosBridge {
    private NeoForgeCuriosBridge() {
    }

    public static List<CuriosStacks.LocatedStack> collectLocated(Player player) {
        return CuriosApi.getCuriosInventory(player).map(NeoForgeCuriosBridge::scan)
                .orElse(List.<CuriosStacks.LocatedStack>of());
    }

    private static List<CuriosStacks.LocatedStack> scan(
            top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler) {
        var result = new ArrayList<CuriosStacks.LocatedStack>();
        for (var found : handler.findCurios(stack -> !stack.isEmpty())) {
            var context = found.slotContext();
            result.add(new CuriosStacks.LocatedStack(found.stack(),
                    context.identifier(), context.index()));
        }
        return result;
    }
}
