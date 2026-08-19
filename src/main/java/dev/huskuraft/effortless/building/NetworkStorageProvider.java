package dev.huskuraft.effortless.building;

import java.util.List;
import java.util.Set;

import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;

/** Optional loader-side bridge to an external item network. */
public interface NetworkStorageProvider {

    Storage create(Player player);

    /**
     * Returns a server-authoritative snapshot of the RS materials relevant to
     * the current preview. Optional providers may return an empty list.
     */
    default List<ItemStack> refinedStorageSnapshot(Player player, Set<Item> requestedItems) {
        return List.of();
    }
}
