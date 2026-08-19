package dev.huskuraft.effortless.building;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;

public interface Storage {

    Storage FULL = new Storage() {
        @Override
        public Optional<ItemStack> searchTag(ItemStack stack) {
            return Optional.of(stack.copy());
        }

        @Override
        public Optional<ItemStack> search(Item item) {
            return Optional.of(item.getDefaultStack());
        }

        @Override
        public boolean consume(ItemStack stack) {
            return true;
        }

        @Override
        public int consume(Item item, int count) {
            return count;
        }

        @Override
        public int getCount(Item item) {
            return Integer.MAX_VALUE;
        }

        @Override
        public List<ItemStack> contents() {
            return List.of();
        }

    };
    Storage EMPTY = new Storage() {
        @Override
        public Optional<ItemStack> searchTag(ItemStack stack) {
            return Optional.empty();
        }

        @Override
        public Optional<ItemStack> search(Item item) {
            return Optional.empty();
        }

        @Override
        public boolean consume(ItemStack stack) {
            return false;
        }

        @Override
        public int consume(Item item, int count) {
            return 0;
        }

        @Override
        public int getCount(Item item) {
            return 0;
        }

        @Override
        public List<ItemStack> contents() {
            return List.of();
        }

    };

    static Storage create(Player player, boolean copy) {
        return new Storage() {
            private final Storage storage;

            {
                storage = switch (player.getGameMode()) {
                    case SURVIVAL, ADVENTURE -> {
                        if (copy) {
                            yield Storage.merge(
                                    Storage.create(player.getInventory().getItems().stream().map(ItemStack::copy).toList(), false),
                                    Storage.preview(network(player))
                            );
                        } else {
                            yield Storage.merge(
                                    Storage.create(player.getInventory().getItems(), false),
                                    network(player)
                            );
                        }
                    }
                    case CREATIVE -> Storage.merge(
                            Storage.create(player.getInventory().getItems().stream().map(ItemStack::copy).toList(), true),
                            full()
                    );
                    case SPECTATOR -> empty();
                };
            }

            private Storage getStorage() {
                return storage;
            }

            @Override
            public Optional<ItemStack> searchTag(ItemStack stack) {
                return getStorage().searchTag(stack);
            }

            @Override
            public Optional<ItemStack> search(Item item) {
                return getStorage().search(item);
            }

            @Override
            public boolean consume(ItemStack stack) {
                return getStorage().consume(stack);
            }

            @Override
            public int consume(Item item, int count) {
                return getStorage().consume(item, count);
            }

            @Override
            public Optional<ItemStack> materialize(Item item, int count) {
                return getStorage().materialize(item, count);
            }

            @Override
            public int getCount(Item item) {
                return getStorage().getCount(item);
            }

            @Override
            public List<ItemStack> contents() {
                return getStorage().contents();
            }

        };
    }

    static Storage network(Player player) {
        try {
            var network = ServiceLoader.load(NetworkStorageProvider.class)
                    .findFirst()
                    .map(provider -> provider.create(player))
                    .orElseGet(Storage::empty);
            if (player.getWorld().isClient()) {
                var snapshot = ClientMaterialSnapshotCache.get(player);
                if (!snapshot.isEmpty()) {
                    return Storage.merge(network, Storage.create(snapshot, false));
                }
            }
            return network;
        } catch (ServiceConfigurationError | LinkageError | RuntimeException ignored) {
            // Optional integrations must never prevent Effortless from loading alone.
            return Storage.empty();
        }
    }

    static List<ItemStack> refinedStorageSnapshot(Player player, java.util.Set<Item> requestedItems) {
        try {
            return ServiceLoader.load(NetworkStorageProvider.class)
                    .findFirst()
                    .map(provider -> provider.refinedStorageSnapshot(player, requestedItems))
                    .orElseGet(List::of);
        } catch (ServiceConfigurationError | LinkageError | RuntimeException ignored) {
            return List.of();
        }
    }


    static Storage merge(Storage... storages) {
        return new Storage() {
            @Override
            public Optional<ItemStack> searchTag(ItemStack stack) {
                for (var storage : storages) {
                    var found = storage.searchTag(stack);
                    if (found.isPresent()) {
                        return found;
                    }
                }
                return Optional.empty();
            }

            @Override
            public Optional<ItemStack> search(Item item) {
                for (var storage : storages) {
                    var found = storage.search(item);
                    if (found.isPresent()) {
                        return found;
                    }
                }
                return Optional.empty();
            }

            @Override
            public boolean consume(ItemStack stack) {
                return consume(stack.getItem(), stack.getCount()) >= stack.getCount();
            }

            @Override
            public int consume(Item item, int count) {
                var consumed = 0;
                for (var storage : storages) {
                    if (consumed >= count) {
                        return consumed;
                    }
                    // Keep search and consume bound to the same source. If a
                    // source reports material but cannot complete its own
                    // extraction, falling through to another source causes
                    // the operation to place a block without charging the
                    // source that supplied the preview stack.
                    var available = Math.min(count - consumed, storage.getCount(item));
                    if (available <= 0) {
                        continue;
                    }
                    var extracted = storage.consume(item, available);
                    consumed += extracted;
                    if (extracted < available) {
                        return consumed;
                    }
                }
                return consumed;
            }

            @Override
            public Optional<ItemStack> materialize(Item item, int count) {
                for (var storage : storages) {
                    var materialized = storage.materialize(item, count);
                    if (materialized.isPresent()) {
                        return materialized;
                    }
                }
                return Optional.empty();
            }

            @Override
            public int getCount(Item item) {
                var result = 0L;
                for (var storage : storages) {
                    result += storage.getCount(item);
                }
                return (int) Math.min(result, Integer.MAX_VALUE);
            }

            @Override
            public List<ItemStack> contents() {
                return Arrays.stream(storages).map(Storage::contents).flatMap(List::stream).toList();
            }

        };
    }

    static Storage full() {
        return FULL;
    }

    static Storage empty() {
        return EMPTY;
    }

    /**
     * Exposes a network for client-side previews without mutating the real network.
     * Reservations are kept locally so a multi-block preview still accounts for
     * items already consumed by earlier operations in the same preview.
     */
    static Storage preview(Storage delegate) {
        return new Storage() {
            private final Map<Item, Integer> reserved = new HashMap<>();

            @Override
            public Optional<ItemStack> searchTag(ItemStack stack) {
                return delegate.searchTag(stack);
            }

            @Override
            public Optional<ItemStack> search(Item item) {
                return getCount(item) > 0
                        ? Optional.of(item.getDefaultStack())
                        : Optional.empty();
            }

            @Override
            public boolean consume(ItemStack stack) {
                return consume(stack.getItem(), stack.getCount()) >= stack.getCount();
            }

            @Override
            public int consume(Item item, int count) {
                if (count <= 0) {
                    return 0;
                }
                var available = getCount(item);
                var consumed = Math.min(available, count);
                if (consumed > 0) {
                    reserved.merge(item, consumed, Integer::sum);
                }
                return consumed;
            }

            @Override
            public Optional<ItemStack> materialize(Item item, int count) {
                return Optional.empty();
            }

            @Override
            public int getCount(Item item) {
                var remaining = (long) delegate.getCount(item) - reserved.getOrDefault(item, 0);
                return (int) Math.max(0, Math.min(Integer.MAX_VALUE, remaining));
            }

            @Override
            public List<ItemStack> contents() {
                return delegate.contents();
            }
        };
    }

    static Storage create(List<ItemStack> itemStacks, boolean infinite) {
        return new Storage() {

            private final Map<Item, ItemStack> cache = new HashMap<>();

            // FIXME: 24/12/23
            @Override
            public Optional<ItemStack> search(Item item) {
                var last = cache.get(item);
                if (last != null && !last.isEmpty()) {
                    return Optional.of(last).map(stack -> infinite ? stack.copy() : stack);
                }
                for (var itemStack : itemStacks) {
                    if (itemStack.getItem().equals(item) && !itemStack.isEmpty()) {
                        cache.put(item, itemStack);
                        return Optional.of(itemStack).map(stack -> infinite ? stack.copy() : stack);
                    }
                }
                cache.put(item, ItemStack.empty());
                return Optional.empty();
            }

            @Override
            public Optional<ItemStack> searchTag(ItemStack itemStack) {
                return Optional.empty();
            }

            @Override
            public boolean consume(ItemStack itemStack) {
                return consume(itemStack.getItem(), itemStack.getCount()) >= itemStack.getCount();
            }

            @Override
            public int consume(Item item, int count) {
                if (infinite) {
                    return count;
                }
                var consumed = 0;
                for (var content : contents()) {
                    if (content.getItem().equals(item)) {
                        var available = Math.min(content.getCount(), count - consumed);
                        content.decrease(available);
                        consumed += available;
                    }
                    if (consumed >= count) {
                        return consumed;
                    }
                }
                return consumed;
            }

            @Override
            public int getCount(Item item) {
                if (infinite) {
                    return Integer.MAX_VALUE;
                }
                var result = 0;
                for (var content : contents()) {
                    if (content.getItem().equals(item)) {
                        result += content.getCount();
                    }
                }
                return result;
            }

            @Override
            public List<ItemStack> contents() {
                return itemStacks;
            }

        };
    }

    Optional<ItemStack> searchTag(ItemStack stack);

    Optional<ItemStack> search(Item item);

    boolean consume(ItemStack stack);

    int consume(Item item, int count);

    /**
     * Retrieves real material for an operation when a storage backend requires
     * its own secure extraction path. Ordinary inventories keep using search
     * and consume, so optional integrations can opt in without changing them.
     */
    default Optional<ItemStack> materialize(Item item, int count) {
        return Optional.empty();
    }

    int getCount(Item item);

//    default boolean contains(Item item) {
//        var result = search(item);
//        return result.isPresent() && !result.get().isEmpty();
//    }
//

    List<ItemStack> contents();

}
