package dev.huskuraft.effortless.building;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.huskuraft.effortless.Effortless;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;

/**
 * Optional Refined Storage bridge. It intentionally uses reflection because
 * RS 1.20, 1.21 and 26.1 expose incompatible wireless-terminal APIs.
 */
public final class RefinedStorageMaterialStorage {
    private static final Set<String> LEGACY_TERMINALS = Set.of(
            "com.refinedmods.refinedstorage.item.WirelessGridItem",
            "com.refinedmods.refinedstorage.item.WirelessCraftingMonitorItem",
            "com.refinedmods.refinedstorage.item.WirelessFluidGridItem",
            "com.refinedmods.refinedstorageaddons.item.WirelessCraftingGridItem"
    );
    private static final Set<String> MODERN_TERMINALS = Set.of(
            "com.refinedmods.refinedstorage.common.grid.WirelessGridItem",
            "com.refinedmods.refinedstorage.quartzarsenal.common.wirelesscraftinggrid.WirelessCraftingGridItem"
    );

    private RefinedStorageMaterialStorage() {
    }

    public static Storage create(Player player, Function<Object, ItemStack> wrapStack) {
        try {
            if (classExists("com.refinedmods.refinedstorage.item.NetworkItem")) {
                return new LegacyStorage(player, wrapStack);
            }
            if (classExists("com.refinedmods.refinedstorage.common.support.network.item.NetworkItemHelperImpl")) {
                return new ModernStorage(player, wrapStack);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            Effortless.LOGGER.debug("Refined Storage integration unavailable", failure);
        }
        return Storage.empty();
    }

    /** Builds a compact count-only snapshot for the current preview. */
    public static List<ItemStack> snapshot(
            Player player, Set<Item> requestedItems, Function<Object, ItemStack> wrapStack) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            return List.of();
        }
        var storage = create(player, wrapStack);
        var counts = new HashMap<Item, Long>();
        for (var stack : storage.contents()) {
            if (stack != null && !stack.isEmpty() && requestedItems.contains(stack.getItem())) {
                counts.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
            }
        }
        return requestedItems.stream()
                .sorted(Comparator.comparing(item -> item.getId().toString()))
                .filter(item -> counts.getOrDefault(item, 0L) > 0)
                .map(item -> {
                    var stack = item.getDefaultStack();
                    stack.setCount((int) Math.min(Integer.MAX_VALUE, counts.get(item)));
                    return stack;
                })
                .toList();
    }

    private abstract static class AbstractStorage implements Storage {
        final Object nativePlayer;
        final Function<Object, ItemStack> wrapStack;

        AbstractStorage(Player player, Function<Object, ItemStack> wrapStack) {
            this.nativePlayer = player.reference();
            this.wrapStack = wrapStack;
        }

        @Override
        public Optional<ItemStack> searchTag(ItemStack stack) {
            return Optional.empty();
        }

        @Override
        public Optional<ItemStack> search(Item item) {
            return contents().stream().filter(stack -> sameItem(stack.getItem(), item) && !stack.isEmpty())
                    .findFirst().map(ItemStack::copy);
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
            var consumed = 0;
            var nativeItem = nativeDefaultStack(item);
            try {
                // RS's extraction APIs deliver into the player inventory. Remove
                // the delivered stack immediately so the caller can place a
                // virtual stack without duplicating network items.
                while (consumed < count) {
                    var before = snapshotInventory();
                    var carriedBefore = carriedStackCount();
                    if (materialize(item, 1).isEmpty()) {
                        break;
                    }
                    var changed = changedInventoryStack(call(nativeItem, "getItem"), before);
                    if (changed.isPresent()) {
                        var delivered = changed.get().reference();
                        call(delivered, "shrink", 1);
                        consumed++;
                        continue;
                    }
                    var carried = changedCarriedStack(call(nativeItem, "getItem"), carriedBefore);
                    if (carried.isEmpty()) {
                        break;
                    }
                    call(carried.get().reference(), "shrink", 1);
                    consumed++;
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("Could not consume from Refined Storage wireless terminal", failure);
            }
            return consumed;
        }

        @Override
        public int getCount(Item item) {
            long total = 0;
            for (var stack : contents()) {
                if (sameItem(stack.getItem(), item)) {
                    total += stack.getCount();
                }
            }
            return (int) Math.min(Integer.MAX_VALUE, total);
        }

        Object nativeDefaultStack(Item item) {
            return item.getDefaultStack().reference();
        }

        List<Object> inventoryStacks() throws ReflectiveOperationException {
            var inventory = call(nativePlayer, "getInventory");
            var slots = ((Number) call(inventory, "getContainerSize")).intValue();
            var stacks = new ArrayList<Object>();
            for (var slot = 0; slot < slots; slot++) {
                stacks.add(call(inventory, "getItem", slot));
            }
            return stacks;
        }

        IdentityHashMap<Object, Integer> snapshotInventory() throws ReflectiveOperationException {
            var snapshot = new IdentityHashMap<Object, Integer>();
            for (var stack : inventoryStacks()) {
                snapshot.put(stack, stackCount(stack));
            }
            return snapshot;
        }

        int carriedStackCount() {
            try {
                var menu = containerMenu();
                return stackCount(call(menu, "getCarried"));
            } catch (ReflectiveOperationException ignored) {
                return 0;
            }
        }

        Optional<ItemStack> changedInventoryStack(Object nativeItem, IdentityHashMap<Object, Integer> before)
                throws ReflectiveOperationException {
            for (var stack : inventoryStacks()) {
                if (sameNativeItem(call(stack, "getItem"), nativeItem)
                        && stackCount(stack) > before.getOrDefault(stack, 0)) {
                    return Optional.of(wrapStack.apply(stack));
                }
            }
            return Optional.empty();
        }

        Optional<ItemStack> changedCarriedStack(Object nativeItem, int before)
                throws ReflectiveOperationException {
            var menu = containerMenu();
            var carried = call(menu, "getCarried");
            return sameNativeItem(call(carried, "getItem"), nativeItem)
                    && stackCount(carried) > before
                    ? Optional.of(wrapStack.apply(carried))
                    : Optional.empty();
        }

        private Object containerMenu() throws ReflectiveOperationException {
            for (var field : allFields(nativePlayer.getClass())) {
                if (field.getName().equals("containerMenu") || field.getName().equals("f_36096_")) {
                    field.setAccessible(true);
                    return field.get(nativePlayer);
                }
            }
            throw new NoSuchFieldException("containerMenu");
        }
    }

    /** RS 1.20.1's NetworkItem API, including Refined Storage Addons terminals. */
    private static final class LegacyStorage extends AbstractStorage {
        private final Object server;
        private final List<Object> terminals;

        LegacyStorage(Player player, Function<Object, ItemStack> wrapStack) throws ReflectiveOperationException {
            super(player, wrapStack);
            // LocalPlayer has no server accessor. This is expected for the
            // client-side preview; the server-side ServerPlayer exposes it.
            server = tryCall(nativePlayer, "getServer");
            terminals = new ArrayList<>();
            terminals.addAll(inventoryStacks());
            terminals.addAll(CuriosStacks.collect(nativePlayer));
            terminals.removeIf(stack -> !isTerminal(stack, LEGACY_TERMINALS));
        }

        @Override
        public List<ItemStack> contents() {
            if (server == null) {
                return List.of();
            }
            try {
                var result = new ArrayList<ItemStack>();
                var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
                for (var network : networks()) {
                    if (!seen.add(network)) {
                        continue;
                    }
                    var cache = call(network, "getItemStorageCache");
                    var list = call(cache, "getList");
                    for (var entry : (Iterable<?>) call(list, "getStacks")) {
                        var stack = call(entry, "getStack");
                        if (!isEmptyStack(stack)) {
                            result.add(wrapStack.apply(stack));
                        }
                    }
                }
                return result;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("Could not read Refined Storage wireless contents", failure);
                return List.of();
            }
        }

        @Override
        public Optional<ItemStack> materialize(Item item, int count) {
            if (server == null || count <= 0) {
                return Optional.empty();
            }
            try {
                var target = nativeDefaultStack(item);
                Optional<ItemStack> extracted = Optional.empty();
                for (var requested = 0; requested < count; requested++) {
                    var before = snapshotInventory();
                    var carriedBefore = carriedStackCount();
                    var extractedThisPass = false;
                    for (var network : networks()) {
                        var handler = call(network, "getItemGridHandler");
                        // RS 1.20.1 uses bit flags: EXTRACT_SINGLE (2) must be
                        // combined with EXTRACT_SHIFT (4) to insert the result
                        // into the player inventory. Using 2 alone puts it on
                        // the menu cursor, which our inventory transaction
                        // cannot observe or remove.
                        call(handler, "onExtract", nativePlayer, target, -1, 6);
                        var changed = changedInventoryStack(call(target, "getItem"), before);
                        if (changed.isEmpty()) {
                            changed = changedCarriedStack(call(target, "getItem"), carriedBefore);
                        }
                        if (changed.isPresent()) {
                            extracted = changed;
                            extractedThisPass = true;
                            break;
                        }
                    }
                    if (!extractedThisPass) {
                        return Optional.empty();
                    }
                }
                return extracted;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("Could not extract from Refined Storage wireless terminal", failure);
                return Optional.empty();
            }
        }

        private List<Object> networks() throws ReflectiveOperationException {
            var networks = new ArrayList<Object>();
            for (var stack : terminals) {
                var item = call(stack, "getItem");
                Consumer<Object> found = networks::add;
                Consumer<Object> ignored = message -> { };
                call(item, "applyNetwork", server, stack, found, ignored);
            }
            return networks;
        }
    }

    /** RS 2.x/3.x wireless API, including Quartz Arsenal and Curios references. */
    private static final class ModernStorage extends AbstractStorage {
        private final List<ModernTerminal> terminals;

        ModernStorage(Player player, Function<Object, ItemStack> wrapStack) throws ReflectiveOperationException {
            super(player, wrapStack);
            terminals = findTerminals();
        }

        @Override
        public List<ItemStack> contents() {
            try {
                var result = new ArrayList<ItemStack>();
                var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
                var playerActor = Class.forName("com.refinedmods.refinedstorage.common.api.storage.PlayerActor");
                for (var terminal : terminals) {
                    if (!Boolean.TRUE.equals(call(terminal.context, "isActive"))) {
                        continue;
                    }
                    var storage = call(terminal.grid, "getItemStorage");
                    if (!seen.add(storage)) {
                        continue;
                    }
                    for (var tracked : (Iterable<?>) call(terminal.grid, "getResources", playerActor)) {
                        var amount = call(tracked, "resourceAmount");
                        var resource = call(amount, "resource");
                        if (!resource.getClass().getName().endsWith("ItemResource")) {
                            continue;
                        }
                        var nativeStack = call(resource, "toItemStack", ((Number) call(amount, "amount")).longValue());
                        if (!isEmptyStack(nativeStack)) {
                            result.add(wrapStack.apply(nativeStack));
                        }
                    }
                }
                return result;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("Could not read Refined Storage wireless contents", failure);
                return List.of();
            }
        }

        @Override
        public Optional<ItemStack> materialize(Item item, int count) {
            if (count <= 0) {
                return Optional.empty();
            }
            try {
                var nativeItem = call(nativeDefaultStack(item), "getItem");
                Optional<ItemStack> extracted = Optional.empty();
                for (var requested = 0; requested < count; requested++) {
                    var before = snapshotInventory();
                    var extractedThisPass = false;
                    for (var terminal : terminals) {
                        if (!Boolean.TRUE.equals(call(terminal.context, "isActive"))) {
                            continue;
                        }
                        var resource = callStatic(
                                Class.forName("com.refinedmods.refinedstorage.common.support.resource.ItemResource"),
                                "ofItemStack", nativeDefaultStack(item));
                        var resourceType = call(resource, "getResourceType");
                        var operations = call(terminal.grid, "createOperations", resourceType, nativePlayer);
                        var mode = Class.forName("com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode")
                                .getField("SINGLE_RESOURCE").get(null);
                        var destination = inventoryDestination();
                        if (!Boolean.TRUE.equals(call(operations, "extract", resource, mode, destination))) {
                            continue;
                        }
                        var changed = changedInventoryStack(nativeItem, before);
                        if (changed.isPresent()) {
                            extracted = changed;
                            extractedThisPass = true;
                            break;
                        }
                    }
                    if (!extractedThisPass) {
                        return Optional.empty();
                    }
                }
                return extracted;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("Could not extract from Refined Storage wireless terminal", failure);
                return Optional.empty();
            }
        }

        private List<ModernTerminal> findTerminals() throws ReflectiveOperationException {
            if (!Class.forName("net.minecraft.server.level.ServerPlayer").isInstance(nativePlayer)) {
                return List.of();
            }
            var terminalItems = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            for (var stack : inventoryStacks()) {
                if (isTerminal(stack, MODERN_TERMINALS)) {
                    terminalItems.add(call(stack, "getItem"));
                }
            }
            for (var stack : CuriosStacks.collect(nativePlayer)) {
                if (isTerminal(stack, MODERN_TERMINALS)) {
                    terminalItems.add(call(stack, "getItem"));
                }
            }
            if (terminalItems.isEmpty()) {
                return List.of();
            }

            var references = new ArrayList<Object>();
            var modern = classExists("com.refinedmods.refinedstorage.common.support.slotreference.InventoryPlayerSlotReferenceProvider");
            references.addAll(findReferences(modern
                    ? "com.refinedmods.refinedstorage.common.support.slotreference.InventoryPlayerSlotReferenceProvider"
                    : "com.refinedmods.refinedstorage.common.support.slotreference.InventorySlotReferenceProvider", terminalItems));
            var curiosProvider = modern
                    ? "com.refinedmods.refinedstorage.curios.CuriosPlayerSlotReferenceProvider"
                    : "com.refinedmods.refinedstorage.curios.CuriosSlotReferenceProvider";
            if (classExists(curiosProvider)) {
                references.addAll(findReferences(curiosProvider, terminalItems));
            }

            var helper = newInstance(Class.forName(
                    "com.refinedmods.refinedstorage.common.support.network.item.NetworkItemHelperImpl"));
            var gridClass = Class.forName("com.refinedmods.refinedstorage.common.grid.WirelessGrid");
            var constructor = gridClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            var result = new ArrayList<ModernTerminal>();
            for (var reference : references) {
                var stack = unwrap(call(reference, modern ? "get" : "resolve", nativePlayer));
                if (stack == null || !isTerminal(stack, MODERN_TERMINALS)) {
                    continue;
                }
                var context = call(helper, "createContext", stack, nativePlayer, reference);
                result.add(new ModernTerminal(context, constructor.newInstance(context)));
            }
            return result;
        }

        private List<Object> findReferences(String providerName, Set<Object> terminalItems)
                throws ReflectiveOperationException {
            var provider = newInstance(Class.forName(providerName));
            var result = call(provider, "find", nativePlayer, terminalItems);
            return result instanceof List<?> list ? new ArrayList<>(list) : List.of();
        }

        private Object inventoryDestination() throws ReflectiveOperationException {
            var insertableStorage = Class.forName("com.refinedmods.refinedstorage.api.storage.InsertableStorage");
            InvocationHandler handler = (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, arguments);
                }
                if (!method.getName().equals("insert")) {
                    return 0L;
                }
                var resource = arguments[0];
                var requested = ((Number) arguments[1]).longValue();
                var action = String.valueOf(arguments[2]);
                if ("SIMULATE".equals(action)) {
                    return requested;
                }
                var stack = call(resource, "toItemStack", requested);
                var inventory = call(nativePlayer, "getInventory");
                call(inventory, "add", stack);
                return requested - stackCount(stack);
            };
            return Proxy.newProxyInstance(insertableStorage.getClassLoader(), new Class<?>[] {insertableStorage}, handler);
        }
    }

    private record ModernTerminal(Object context, Object grid) {
    }

    private static boolean isTerminal(Object stack, Set<String> names) {
        try {
            return stack != null && !isEmptyStack(stack) && isType(call(stack, "getItem"), names);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isType(Object object, Set<String> names) {
        for (var type = object == null ? null : object.getClass(); type != null; type = type.getSuperclass()) {
            if (names.contains(type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, RefinedStorageMaterialStorage.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean sameItem(Item left, Item right) {
        return left != null && right != null && left.getId().equals(right.getId());
    }

    private static boolean sameNativeItem(Object left, Object right) {
        return left == right || left != null && left.equals(right);
    }

    private static boolean isEmptyStack(Object stack) {
        try {
            return stack == null || Boolean.TRUE.equals(call(stack, "isEmpty"));
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static int stackCount(Object stack) {
        try {
            return ((Number) call(stack, "getCount")).intValue();
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static Object unwrap(Object value) throws ReflectiveOperationException {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private static Object newInstance(Class<?> type) throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static Object call(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            throw new NoSuchMethodException("null#" + name);
        }
        for (var method : allMethods(target.getClass())) {
            if (methodNameMatches(method, name) && compatible(method.getParameterTypes(), arguments)) {
                method.setAccessible(true);
                return method.invoke(target, arguments);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
    }

    private static Object tryCall(Object target, String name, Object... arguments) {
        try {
            return call(target, name, arguments);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static List<java.lang.reflect.Method> allMethods(Class<?> type) {
        var methods = new ArrayList<java.lang.reflect.Method>();
        Collections.addAll(methods, type.getMethods());
        for (var current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(methods, current.getDeclaredMethods());
        }
        return methods;
    }

    private static List<java.lang.reflect.Field> allFields(Class<?> type) {
        var fields = new ArrayList<java.lang.reflect.Field>();
        for (var current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(fields, current.getDeclaredFields());
        }
        return fields;
    }

    /** Forge 1.20.1 production jars use SRG names for these vanilla methods. */
    private static boolean methodNameMatches(java.lang.reflect.Method method, String requested) {
        if (method.getName().equals(requested)) return true;
        return switch (requested) {
            case "getServer" -> method.getName().equals("m_20194_");
            case "getInventory" -> method.getName().equals("m_150109_");
            case "getContainerMenu" -> method.getName().equals("m_5893_");
            case "getCarried" -> method.getName().equals("m_142621_");
            case "getContainerSize" -> method.getName().equals("m_6643_");
            case "getItem" -> method.getName().equals("m_8020_") || method.getName().equals("m_41720_");
            case "getCount" -> method.getName().equals("m_41613_");
            case "isEmpty" -> method.getName().equals("m_41619_");
            case "add" -> method.getName().equals("m_36054_");
            case "shrink" -> method.getName().equals("m_41774_");
            default -> false;
        };
    }

    private static Object callStatic(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
        for (var method : allMethods(type)) {
            if (method.getName().equals(name) && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    && compatible(method.getParameterTypes(), arguments)) {
                method.setAccessible(true);
                return method.invoke(null, arguments);
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static boolean compatible(Class<?>[] types, Object[] arguments) {
        if (types.length != arguments.length) return false;
        for (var index = 0; index < types.length; index++) {
            if (arguments[index] == null) {
                if (types[index].isPrimitive()) return false;
            } else if (!box(types[index]).isInstance(arguments[index])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
