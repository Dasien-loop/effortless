package dev.huskuraft.effortless.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.auto.service.AutoService;

import appeng.api.config.Actionable;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;
import dev.huskuraft.effortless.building.NetworkStorageProvider;
import dev.huskuraft.effortless.building.CuriosStacks;
import dev.huskuraft.effortless.building.OptionalMaterialStorages;
import dev.huskuraft.effortless.building.Storage;
import dev.huskuraft.effortless.vanilla.core.MinecraftItemStack;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.platform.Platform;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;

@AutoService(NetworkStorageProvider.class)
public final class NetworkStorageProviderImpl implements NetworkStorageProvider {

    @Override
    public Storage create(Player player) {
        var storages = new ArrayList<Storage>();
        if (Platform.getInstance().findMod("ae2").isPresent()) {
            storages.add(Ae2Storage.create(player));
        }
        if (Platform.getInstance().findMod("beyonddimensions").isPresent()) {
            storages.add(BeyondDimensionsStorage.create(player));
        }
        if (Platform.getInstance().findMod("refinedstorage").isPresent()) {
            storages.add(OptionalMaterialStorages.refinedStorage(player,
                    stack -> new MinecraftItemStack((net.minecraft.world.item.ItemStack) stack)));
        }
        if (Platform.getInstance().findMod("sophisticatedbackpacks").isPresent()) {
            storages.add(OptionalMaterialStorages.sophisticatedBackpacks(player,
                    stack -> new MinecraftItemStack((net.minecraft.world.item.ItemStack) stack)));
        }
        if (Platform.getInstance().findMod("projecte").isPresent()) {
            storages.add(OptionalMaterialStorages.projectE(player));
        }
        return storages.isEmpty() ? Storage.empty() : Storage.merge(storages.toArray(Storage[]::new));
    }

    @Override
    public List<ItemStack> refinedStorageSnapshot(Player player, Set<Item> requestedItems) {
        if (Platform.getInstance().findMod("refinedstorage").isEmpty()) {
            return List.of();
        }
        return dev.huskuraft.effortless.building.RefinedStorageMaterialStorage.snapshot(
                player, requestedItems,
                stack -> new MinecraftItemStack((net.minecraft.world.item.ItemStack) stack));
    }

    private static final class Ae2Storage {
        private Ae2Storage() {
        }

        static Storage create(Player universalPlayer) {
            net.minecraft.world.entity.player.Player player = universalPlayer.reference();
            var terminals = new ArrayList<TerminalAccess>();
            var inventory = player.getInventory();
            for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
                var stack = inventory.getItem(slot);
                if (stack.getItem() instanceof WirelessTerminalItem terminal) {
                    var host = new WirelessTerminalMenuHost<>(terminal, player,
                            MenuLocators.forInventorySlot(slot), (ignoredPlayer, ignoredMenu) -> {
                            });
                    terminals.add(new TerminalAccess(host, terminal, stack));
                }
            }
            for (var curio : CuriosStacks.collectLocated(player)) {
                var curiosStack = (net.minecraft.world.item.ItemStack) curio.stack();
                if (curiosStack.getItem() instanceof WirelessTerminalItem terminal) {
                    terminals.add(new TerminalAccess(createCuriosHost(player, terminal, curiosStack, curio),
                            terminal, curiosStack));
                }
            }
            if (terminals.isEmpty()) {
                return Storage.empty();
            }
            return new Storage() {
                @Override
                public Optional<ItemStack> searchTag(ItemStack stack) {
                    return Optional.empty();
                }

                @Override
                public Optional<ItemStack> search(Item item) {
                    return findContent(item);
                }

                @Override
                public boolean consume(ItemStack stack) {
                    return consume(stack.getItem(), stack.getCount()) >= stack.getCount();
                }

                @Override
                public int consume(Item item, int count) {
                    return (int) Math.min(Integer.MAX_VALUE, extract(item, count, Actionable.MODULATE));
                }

                @Override
                public int getCount(Item item) {
                    // Client previews must use AE2's synchronized contents; powered
                    // simulation can return zero without a server-side energy source.
                    return countContent(item);
                }

                @Override
                public List<ItemStack> contents() {
                    var result = new ArrayList<ItemStack>();
                    var seen = Collections.newSetFromMap(new IdentityHashMap<MEStorage, Boolean>());
                    for (var access : terminals) {
                        var inventory = access.inventory(player);
                        if (inventory == null || !seen.add(inventory)) {
                            continue;
                        }
                        for (var entry : inventory.getAvailableStacks()) {
                            if (entry.getKey() instanceof AEItemKey key && entry.getLongValue() > 0) {
                                var amount = (int) Math.min(Integer.MAX_VALUE, entry.getLongValue());
                                result.add(new MinecraftItemStack(key.toStack(amount)));
                            }
                        }
                    }
                    return result;
                }

                private Optional<ItemStack> findContent(Item item) {
                    return contents().stream()
                            .filter(stack -> sameItem(stack.getItem(), item) && !stack.isEmpty())
                            .findFirst()
                            .map(ItemStack::copy);
                }

                private int countContent(Item item) {
                    long count = 0;
                    for (var stack : contents()) {
                        if (sameItem(stack.getItem(), item)) {
                            count += stack.getCount();
                        }
                    }
                    return (int) Math.min(Integer.MAX_VALUE, count);
                }

                private long extract(Item item, long count, Actionable action) {
                    net.minecraft.world.item.ItemStack stack = item.getDefaultStack().reference();
                    var key = AEItemKey.of(stack);
                    var source = new PlayerSource(player);
                    long extracted = 0;
                    for (var access : terminals) {
                        if (!access.connected(player)) {
                            continue;
                        }
                        var inventory = access.inventory(player);
                        if (inventory == null) {
                            continue;
                        }
                        extracted += StorageHelper.poweredExtraction(access.energySource(player), inventory, key,
                                count - extracted, source, action);
                        if (extracted >= count) {
                            break;
                        }
                    }
                    return extracted;
                }

            };
        }

        private static WirelessTerminalMenuHost<?> createCuriosHost(
                net.minecraft.world.entity.player.Player player,
                WirelessTerminalItem terminal,
                net.minecraft.world.item.ItemStack stack,
                CuriosStacks.LocatedStack curio) {
            try {
                var universalItem = Class.forName("de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem");
                if (curio.identifier() != null && universalItem.isInstance(terminal)) {
                    var locatorType = Class.forName("de.mari_023.ae2wtlib.curio.CurioLocator");
                    var locator = locatorType.getConstructor(String.class, int.class)
                            .newInstance(curio.identifier(), curio.index());
                    var menuLocator = Class.forName("appeng.menu.locator.MenuLocator");
                    var host = universalItem.getMethod("getMenuHost", net.minecraft.world.entity.player.Player.class,
                            menuLocator, net.minecraft.world.item.ItemStack.class)
                            .invoke(terminal, player, locator, stack);
                    if (host instanceof WirelessTerminalMenuHost<?> terminalHost) {
                        return terminalHost;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // AE2WTLib is optional. Vanilla AE2 terminals use the fallback.
            }
            try {
                // AE2WTLib 19.x moved the universal terminal API to ItemWT
                // and relies on AE2's built-in Curios locator.
                var itemWt = Class.forName("de.mari_023.ae2wtlib.api.terminal.ItemWT");
                if (itemWt.isInstance(terminal)) {
                    var locator = MenuLocators.forCurioSlot(curio.index());
                    var host = itemWt.getMethod("getMenuHost",
                                    net.minecraft.world.entity.player.Player.class,
                                    Class.forName("appeng.menu.locator.ItemMenuHostLocator"),
                                    net.minecraft.world.phys.BlockHitResult.class)
                            .invoke(terminal, player, locator, null);
                    if (host instanceof WirelessTerminalMenuHost<?> terminalHost) {
                        return terminalHost;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Older AE2WTLib versions use the API above this block.
            }
            return new WirelessTerminalMenuHost<>(terminal, player, null, (ignoredPlayer, ignoredMenu) -> {
            });
        }

        private record TerminalAccess(
                WirelessTerminalMenuHost<?> host,
                WirelessTerminalItem item,
                net.minecraft.world.item.ItemStack stack
        ) {
            boolean connected(net.minecraft.world.entity.player.Player player) {
                if (host != null && host.getLinkStatus().connected()) return true;
                var grid = linkedGrid(player);
                return grid != null && !grid.isEmpty();
            }

            private appeng.api.networking.IGrid linkedGrid(
                    net.minecraft.world.entity.player.Player player) {
                return item.getLinkedGrid(stack, player.level(), ignored -> {
                });
            }

            private appeng.api.networking.energy.IEnergySource energySource(
                    net.minecraft.world.entity.player.Player player) {
                if (host != null && host.getLinkStatus().connected()) {
                    return host;
                }
                return (amount, action, multiplier) -> {
                    var power = multiplier.multiply(amount);
                    if (!item.hasPower(player, power, stack)) {
                        return 0;
                    }
                    return action == Actionable.SIMULATE || item.usePower(player, power, stack) ? amount : 0;
                };
            }

            MEStorage inventory(net.minecraft.world.entity.player.Player player) {
                // The item link is authoritative for terminals stored in a
                // Curios slot; the host cache may not be initialized yet.
                var grid = linkedGrid(player);
                if (grid != null && !grid.isEmpty()) {
                    var storage = grid.getStorageService();
                    if (storage != null && storage.getInventory() != null) {
                        return storage.getInventory();
                    }
                }
                if (host != null) {
                    var hostInventory = host.getInventory();
                    if (hostInventory != null) {
                        return hostInventory;
                    }
                }
                // On the server the menu host can be a NullInventory until its
                // menu tick runs.  The terminal's linked grid is authoritative
                // and is available immediately, so use the grid storage service
                // for previews.  Keep the host path for client-side rendering,
                // where AE2 synchronizes a local terminal cache instead.
                return null;
            }
        }

    }

    private static boolean sameItem(Item left, Item right) {
        return left != null && right != null && left.getId().equals(right.getId());
    }

    private static final class BeyondDimensionsStorage {
        private BeyondDimensionsStorage() {
        }

        static Storage create(Player universalPlayer) {
            var network = DimensionsNet.getPrimaryNetFromPlayer(universalPlayer.reference());
            if (network == null) {
                return Storage.empty();
            }
            UnifiedStorage storage = network.getUnifiedStorage();
            return new Storage() {
                @Override
                public Optional<ItemStack> searchTag(ItemStack stack) {
                    return Optional.empty();
                }

                @Override
                public Optional<ItemStack> search(Item item) {
                    return contents().stream()
                            .filter(stack -> sameItem(stack.getItem(), item) && !stack.isEmpty())
                            .findFirst()
                            .map(ItemStack::copy);
                }

                @Override
                public boolean consume(ItemStack stack) {
                    return consume(stack.getItem(), stack.getCount()) >= stack.getCount();
                }

                @Override
                public int consume(Item item, int count) {
                    return (int) Math.min(Integer.MAX_VALUE,
                            storage.extract(new ItemStackKey(item.getDefaultStack().reference()), count, false, false).amount());
                }

                @Override
                public int getCount(Item item) {
                    long count = 0;
                    for (var stack : contents()) {
                        if (sameItem(stack.getItem(), item)) {
                            count += stack.getCount();
                        }
                    }
                    return (int) Math.min(Integer.MAX_VALUE, count);
                }

                @Override
                public List<ItemStack> contents() {
                    var result = new ArrayList<ItemStack>();
                    for (var entry : storage.getStorage()) {
                        if (entry.key() instanceof ItemStackKey key && entry.amount() > 0) {
                            var amount = Math.min(Integer.MAX_VALUE, entry.amount());
                            result.add(new MinecraftItemStack(key.copyStackWithCount(amount)));
                        }
                    }
                    return result;
                }
            };
        }
    }
}
