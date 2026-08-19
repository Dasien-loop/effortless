package dev.huskuraft.effortless.building;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;

/**
 * Client-side copy of the latest server-authoritative RS material snapshot.
 * Only the local preview context is retained, so stale data cannot leak into
 * another preview while the next server response is in flight.
 */
public final class ClientMaterialSnapshotCache {
    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private ClientMaterialSnapshotCache() {
    }

    public static void activate(Player player, UUID contextId) {
        if (player == null || contextId == null) {
            return;
        }
        SNAPSHOTS.compute(player.getId(), (ignored, current) ->
                current != null && contextId.equals(current.contextId())
                        ? current
                        : new Snapshot(contextId, List.of()));
    }

    public static void update(Player player, UUID contextId, List<ItemStack> items) {
        if (player == null || contextId == null) {
            return;
        }
        SNAPSHOTS.compute(player.getId(), (ignored, current) -> {
            if (current == null || !contextId.equals(current.contextId())) {
                return current;
            }
            var copies = new ArrayList<ItemStack>(items.size());
            for (var item : items) {
                if (item != null && !item.isEmpty() && item.getCount() > 0) {
                    copies.add(item.copy());
                }
            }
            return new Snapshot(contextId, List.copyOf(copies));
        });
    }

    public static List<ItemStack> get(Player player) {
        if (player == null) {
            return List.of();
        }
        var snapshot = SNAPSHOTS.get(player.getId());
        return snapshot == null ? List.of() : snapshot.items();
    }

    public static void clear(Player player) {
        if (player != null) {
            SNAPSHOTS.remove(player.getId());
        }
    }

    private record Snapshot(UUID contextId, List<ItemStack> items) {
    }
}
