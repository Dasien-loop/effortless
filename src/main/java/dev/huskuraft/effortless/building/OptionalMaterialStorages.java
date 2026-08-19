package dev.huskuraft.effortless.building;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.effortless.Effortless;

/**
 * Loader-neutral optional integrations.  External classes are resolved only
 * after their owning mod has been detected, so they cannot prevent standalone
 * Effortless installations from loading.
 */
public final class OptionalMaterialStorages {

    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private OptionalMaterialStorages() {
    }

    public static Storage refinedStorage(Player player, Function<Object, ItemStack> wrapStack) {
        return RefinedStorageMaterialStorage.create(player, wrapStack);
    }

    public static Storage sophisticatedBackpacks(Player player, Function<Object, ItemStack> wrapStack) {
        try {
            var wrapperClass = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper");
            var nativePlayer = player.reference();
            var handlers = new ArrayList<Object>();
            var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            // Sophisticated Core registers handlers for backpacks in the
            // player's equipment/container providers. Prefer that API because
            // it also understands nested and dynamically-added Curios slots.
            collectRegisteredBackpackHandlers(nativePlayer, handlers, seen);
            // Sophisticated Core may register generic handlers even when a
            // backpack is only present in an equipment/Curios slot. Always
            // scan accessible stacks as well so those backpacks become a real
            // material source instead of being hidden by the registered list.
            for (var stack : accessiblePlayerStacks(nativePlayer)) {
                if (stack != null && seen.add(stack)) {
                    addBackpackHandler(handlers, wrapperClass, stack, tryCall(nativePlayer, "level"));
                }
            }
            return handlers.isEmpty() ? Storage.empty() : new BackpackStorage(handlers, wrapStack);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            Effortless.LOGGER.debug("Sophisticated Backpacks integration unavailable", failure);
            return Storage.empty();
        }
    }

    /**
     * Collects normal inventory, equipment and Curios stacks. Some equipment
     * integrations do not expose their slots through Player#getInventory(),
     * so the explicit equipment accessors are intentionally included.
     */
    private static List<Object> accessiblePlayerStacks(Object player) throws ReflectiveOperationException {
        var result = new ArrayList<Object>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        var inventory = call(player, "getInventory");
        var slots = ((Number) call(inventory, "getContainerSize")).intValue();
        for (var slot = 0; slot < slots; slot++) {
            addStack(result, seen, call(inventory, "getItem", slot));
        }
        addStack(result, seen, tryCall(player, "getMainHandItem"));
        addStack(result, seen, tryCall(player, "getOffhandItem"));
        var armor = tryCall(player, "getArmorSlots");
        if (armor instanceof Iterable<?> iterable) {
            for (var stack : iterable) {
                addStack(result, seen, stack);
            }
        }
        try {
            for (var stack : CuriosStacks.collect(player)) {
                addStack(result, seen, stack);
            }
        } catch (LinkageError | RuntimeException ignored) {
            // Curios is optional. A broken/absent Curios provider must not
            // hide backpacks that are present in the ordinary inventory.
        }
        return result;
    }

    private static void addStack(List<Object> result, Set<Object> seen, Object stack) {
        if (stack != null && seen.add(stack)) {
            result.add(stack);
        }
    }

    private static Object tryCall(Object target, String name, Object... arguments) {
        try {
            return call(target, name, arguments);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void addBackpackHandler(
            List<Object> handlers, Class<?> wrapperClass, Object stack, Object level)
            throws ReflectiveOperationException {
        if (stack == null || isEmptyStack(stack) || !isSophisticatedBackpack(stack)) {
            return;
        }
        Object wrapper;
        try {
            wrapper = level == null
                    ? callStatic(wrapperClass, "fromStack", stack)
                    : callStatic(wrapperClass, "fromStack", stack, level);
        } catch (ReflectiveOperationException ignored) {
            try {
                wrapper = callStatic(wrapperClass, "fromStack", stack);
            } catch (ReflectiveOperationException ignoredAgain) {
                Constructor<?> constructor = wrapperClass.getConstructors()[0];
                wrapper = constructor.newInstance(stack);
            }
        }
        // Newer Sophisticated Backpacks lazily initializes the NBT-backed
        // handler. Calling onInit is harmless on versions where it is already
        // initialized and is required for handlers created outside a menu.
        if (level != null) {
            tryCall(wrapper, "onInit", level);
        }
        var handler = call(wrapper, "getInventoryHandler");
        if (handler != null && !handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    private static void collectRegisteredBackpackHandlers(
            Object player, List<Object> handlers, Set<Object> seen) {
        try {
            var helper = Class.forName("net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper");
            for (var methodName : List.of("getItemHandlersFromPlayerIncludingContainers",
                    "getEquipmentItemHandlersFromPlayer")) {
                var method = findStaticMethod(helper, methodName, player.getClass());
                if (method == null) continue;
                var value = method.invoke(null, player);
                if (!(value instanceof Iterable<?> iterable)) continue;
                for (var handler : iterable) {
                    if (handler == null) continue;
                    var name = handler.getClass().getName();
                    if ((name.contains("sophisticatedbackpacks")
                            || name.equals("net.p3pp3rf1y.sophisticatedcore.inventory.InventoryIOHandler"))
                            && seen.add(handler)) {
                        handlers.add(handler);
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Older Sophisticated Core versions do not expose these helpers.
        }
    }

    private static Method findStaticMethod(Class<?> owner, String name, Class<?> argumentType) {
        for (var method : allMethods(owner)) {
            if (method.getName().equals(name) && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    /** Supports ProjectE 1.20.1 Forge and 1.21.1 NeoForge. */
    public static Storage projectE(Player player) {
        try {
            Class.forName("moze_intel.projecte.api.capabilities.PECapabilities");
            return new ProjectEStorage(player);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return Storage.empty();
        }
    }

    private static final class BackpackStorage implements Storage {
        private final List<Object> handlers;
        private final Function<Object, ItemStack> wrapStack;
        private final List<CachedBackpackSlot> slots = new ArrayList<>();
        private final java.util.Map<String, Integer> counts = new HashMap<>();
        private boolean cacheInitialized;

        private BackpackStorage(List<Object> handlers, Function<Object, ItemStack> wrapStack) {
            this.handlers = handlers;
            this.wrapStack = wrapStack;
        }

        @Override
        public Optional<ItemStack> searchTag(ItemStack stack) {
            return Optional.empty();
        }

        @Override
        public Optional<ItemStack> search(Item item) {
            ensureCache();
            return slots.stream()
                    .filter(slot -> slot.count > 0 && sameItem(slot.item, item))
                    .findFirst()
                    .map(slot -> slot.wrapped.copy());
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
            ensureCache();
            var consumed = 0;
            for (var cached : slots) {
                if (consumed >= count || cached.count <= 0 || !sameItem(cached.item, item)) {
                    continue;
                }
                var stack = stackInSlot(cached.handler, cached.slot);
                if (stack == null || isEmptyStack(stack)) {
                    cached.count = 0;
                    continue;
                }
                cached.nativeStack = stack;
                cached.count = stackCount(stack);
                if (cached.count <= 0) {
                    continue;
                }
                var requested = count - consumed;
                var before = stackCount(stack);
                var extracted = extract(cached.handler, cached.slot, requested);
                var afterStack = stackInSlot(cached.handler, cached.slot);
                var after = afterStack == null ? 0 : stackCount(afterStack);
                var changed = Math.max(0, before - after);
                if (changed == 0 && extracted != null) {
                    // Some older handlers return a detached copy while
                    // deferring the slot mutation. Retry through the
                    // mutable stack only when the slot did not change;
                    // never count a returned copy as consumption by itself.
                    changed = forceExtract(cached.handler, cached.slot, requested, stack);
                }
                var actual = Math.min(requested, changed);
                if (actual > 0) {
                    consumed += actual;
                    adjustCount(cached.item, -actual);
                }
                var previousNativeStack = cached.nativeStack;
                var current = stackInSlot(cached.handler, cached.slot);
                cached.nativeStack = current;
                cached.count = current == null ? 0 : stackCount(current);
                if (current != null && current != previousNativeStack && !isEmptyStack(current)
                        && !sameItem(cached.item, wrapStack.apply(current).getItem())) {
                    refreshSlot(cached, current);
                }
            }
            return consumed;
        }

        @Override
        public int getCount(Item item) {
            ensureCache();
            return counts.getOrDefault(item.getId().toString(), 0);
        }

        @Override
        public List<ItemStack> contents() {
            ensureCache();
            var result = new ArrayList<ItemStack>(slots.size());
            for (var slot : slots) {
                if (slot.count > 0) {
                    result.add(slot.wrapped);
                }
            }
            return result;
        }

        private void ensureCache() {
            if (cacheInitialized) {
                return;
            }
            cacheInitialized = true;
            for (var handler : handlers) {
                for (var slot = 0; slot < slotCount(handler); slot++) {
                    var nativeStack = stackInSlot(handler, slot);
                    if (nativeStack == null || isEmptyStack(nativeStack)) {
                        continue;
                    }
                    var wrapped = wrapStack.apply(nativeStack);
                    var cached = new CachedBackpackSlot(handler, slot, nativeStack, wrapped.getItem(), wrapped,
                            stackCount(nativeStack));
                    slots.add(cached);
                    adjustCount(cached.item, cached.count);
                }
            }
        }

        private void refreshSlot(CachedBackpackSlot slot, Object nativeStack) {
            adjustCount(slot.item, -slot.count);
            slot.nativeStack = nativeStack;
            slot.wrapped = wrapStack.apply(nativeStack);
            slot.item = slot.wrapped.getItem();
            slot.count = stackCount(nativeStack);
            adjustCount(slot.item, slot.count);
        }

        private void adjustCount(Item item, int delta) {
            var key = item.getId().toString();
            var next = counts.getOrDefault(key, 0) + delta;
            if (next <= 0) {
                counts.remove(key);
            } else {
                counts.put(key, next);
            }
        }
    }

    private static final class CachedBackpackSlot {
        private final Object handler;
        private final int slot;
        private Object nativeStack;
        private Item item;
        private ItemStack wrapped;
        private int count;

        private CachedBackpackSlot(
                Object handler, int slot, Object nativeStack, Item item, ItemStack wrapped, int count) {
            this.handler = handler;
            this.slot = slot;
            this.nativeStack = nativeStack;
            this.item = item;
            this.wrapped = wrapped;
            this.count = count;
        }
    }

    private static final class ProjectEStorage implements Storage {
        private final Player player;

        private ProjectEStorage(Player player) {
            this.player = player;
        }

        @Override
        public Optional<ItemStack> searchTag(ItemStack stack) {
            return Optional.empty();
        }

        @Override
        public Optional<ItemStack> search(Item item) {
            return getCount(item) > 0 ? Optional.of(item.getDefaultStack()) : Optional.empty();
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
            try {
                var provider = knowledgeProvider();
                var nativeStack = item.getDefaultStack().reference();
                if (provider == null || !Boolean.TRUE.equals(call(provider, "hasKnowledge", nativeStack))) {
                    return 0;
                }
                var value = emcValue(nativeStack);
                if (value <= 0) {
                    return 0;
                }
                var emc = (BigInteger) call(provider, "getEmc");
                var affordable = emc.divide(BigInteger.valueOf(value));
                var used = Math.min((long) count, affordable.min(BigInteger.valueOf(Integer.MAX_VALUE)).longValue());
                if (used > 0) {
                    var remaining = emc.subtract(BigInteger.valueOf(value).multiply(BigInteger.valueOf(used)));
                    call(provider, "setEmc", remaining);
                    // ProjectE keeps the authoritative EMC on the server. A
                    // direct capability mutation must also notify the client,
                    // otherwise the GUI redraws the old value and makes it
                    // look as if no EMC was spent.
                    try {
                        // PlatformReference#reference is generic. Cast the
                        // value to Object explicitly or Java infers Object[]
                        // for the varargs parameter and throws before the
                        // reflective call is made.
                        call(provider, "syncEmc", (Object) player.reference());
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Older ProjectE builds do not expose syncEmc.
                    }
                }
                return (int) used;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("ProjectE integration unavailable while consuming EMC", failure);
                return 0;
            }
        }

        @Override
        public int getCount(Item item) {
            try {
                var provider = knowledgeProvider();
                var nativeStack = item.getDefaultStack().reference();
                if (provider == null || !Boolean.TRUE.equals(call(provider, "hasKnowledge", nativeStack))) {
                    return 0;
                }
                var value = emcValue(nativeStack);
                if (value <= 0) {
                    return 0;
                }
                var emc = (BigInteger) call(provider, "getEmc");
                return emc.divide(BigInteger.valueOf(value)).min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Effortless.LOGGER.debug("ProjectE integration unavailable while reading EMC", failure);
                return 0;
            }
        }

        @Override
        public List<ItemStack> contents() {
            return List.of();
        }

        private Object knowledgeProvider() throws ReflectiveOperationException {
            var capabilities = Class.forName("moze_intel.projecte.api.capabilities.PECapabilities");
            Field field = capabilities.getField("KNOWLEDGE_CAPABILITY");
            var capability = field.get(null);
            var nativePlayer = player.reference();
            // Forge's LazyOptional capability API. Forge 1.20.1 exposes the
            // direction-aware two-argument form on living entities.
            for (var method : allMethods(nativePlayer.getClass())) {
                if (!method.getName().equals("getCapability")) {
                    continue;
                }
                Object lazy = null;
                if (method.getParameterCount() == 2
                        && method.getParameterTypes()[0].isInstance(capability)
                        && !method.getParameterTypes()[1].isPrimitive()) {
                    lazy = method.invoke(nativePlayer, capability, null);
                } else if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(capability)) {
                    lazy = method.invoke(nativePlayer, capability);
                }
                if (lazy != null) {
                    try {
                        // Forge returns a LazyOptional here. NeoForge's
                        // EntityCapability overload returns the capability
                        // instance directly, so it must not be unwrapped.
                        var resolved = call(lazy, "orElse", (Object) null);
                        if (resolved != null) {
                            return resolved;
                        }
                    } catch (NoSuchMethodException directCapability) {
                        return lazy;
                    }
                }
            }
            // NeoForge 1.21's EntityCapability API exposes getCapability(Entity,
            // context). ProjectE's KNOWLEDGE_CAPABILITY uses Void as its context,
            // so null is the correct context value. Older drafts of the
            // NeoForge API used get(...); retain that fallback for compatible
            // development mappings without changing the Forge path above.
            for (var methodName : List.of("getCapability", "get")) {
                try {
                    var resolved = call(capability, methodName, nativePlayer, null);
                    if (resolved != null) {
                        return resolved;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the alternate NeoForge name.
                }
            }
            return null;
        }

        private static long emcValue(Object nativeStack) throws ReflectiveOperationException {
            var proxyClass = Class.forName("moze_intel.projecte.api.proxy.IEMCProxy");
            var proxy = proxyClass.getField("INSTANCE").get(null);
            return ((Number) call(proxy, "getValue", nativeStack)).longValue();
        }
    }

    private static int slotCount(Object handler) {
        try {
            return ((Number) call(handler, "getSlots")).intValue();
        } catch (ReflectiveOperationException ignored) {
            try {
                return ((Number) call(handler, "size")).intValue();
            } catch (ReflectiveOperationException ignoredAgain) {
                return 0;
            }
        }
    }

    private static Object stackInSlot(Object handler, int slot) {
        try {
            return call(handler, "getStackInSlot", slot);
        } catch (ReflectiveOperationException ignored) {
            try {
                return call(handler, "getInternalStack", slot);
            } catch (ReflectiveOperationException ignoredAgain) {
                return null;
            }
        }
    }

    private static Object extract(Object handler, int slot, int amount) {
        try {
            return call(handler, "extractItem", slot, amount, false);
        } catch (ReflectiveOperationException ignored) {
            // NeoForge 26.1.2 uses a transaction-based handler. Its mutable
            // internal stack remains available for compatibility with upgrades.
            var stack = stackInSlot(handler, slot);
            if (stack == null) {
                return null;
            }
            try {
                var copy = call(stack, "copy");
                var extracted = Math.min(amount, stackCount(stack));
                call(copy, "setCount", extracted);
                call(stack, "shrink", extracted);
                try {
                    call(handler, "setStackInSlot", slot, stack);
                } catch (ReflectiveOperationException ignoredSetter) {
                    // Older handlers mutate the returned stack directly.
                }
                return copy;
            } catch (ReflectiveOperationException ignoredAgain) {
                return null;
            }
        }
    }

    private static int forceExtract(Object handler, int slot, int amount, Object stack) {
        try {
            var available = stackCount(stack);
            var extracted = Math.min(amount, available);
            if (extracted <= 0) {
                return 0;
            }
            call(stack, "shrink", extracted);
            try {
                call(handler, "setStackInSlot", slot, stack);
            } catch (ReflectiveOperationException ignored) {
                // Mutable handlers update the slot through the stack itself.
            }
            var after = stackInSlot(handler, slot);
            return Math.min(extracted, Math.max(0, available - (after == null ? 0 : stackCount(after))));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean isSophisticatedBackpack(Object stack) throws ReflectiveOperationException {
        var item = call(stack, "getItem");
        return item != null && item.getClass().getName().contains("sophisticatedbackpacks");
    }

    private static boolean isEmptyStack(Object stack) {
        try {
            return Boolean.TRUE.equals(call(stack, "isEmpty"));
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

    private static boolean sameItem(Item left, Item right) {
        return left != null && right != null && left.getId().equals(right.getId());
    }

    private static Object call(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            throw new NoSuchMethodException("null#" + name);
        }
        var key = methodCacheKey(target.getClass(), name, arguments);
        var method = METHOD_CACHE.get(key);
        if (method == null) {
            for (var candidate : allMethods(target.getClass())) {
                if (methodNameMatches(candidate, name) && compatible(candidate.getParameterTypes(), arguments)) {
                    candidate.setAccessible(true);
                    METHOD_CACHE.putIfAbsent(key, candidate);
                    method = candidate;
                    break;
                }
            }
        }
        if (method != null) {
            return method.invoke(target, arguments);
        }
        throw new NoSuchMethodException(name);
    }

    private static String methodCacheKey(Class<?> type, String name, Object[] arguments) {
        var builder = new StringBuilder(type.getName()).append('#').append(name);
        for (var argument : arguments) {
            builder.append(':').append(argument == null ? "null" : argument.getClass().getName());
        }
        return builder.toString();
    }

    private static List<Method> allMethods(Class<?> type) {
        var methods = new ArrayList<Method>();
        Collections.addAll(methods, type.getMethods());
        for (var current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(methods, current.getDeclaredMethods());
        }
        return methods;
    }

    /** Forge 1.20.1 production jars use SRG names for these vanilla methods. */
    private static boolean methodNameMatches(Method method, String requested) {
        if (method.getName().equals(requested)) return true;
        return switch (requested) {
            case "getInventory" -> method.getName().equals("m_150109_");
            case "getContainerSize" -> method.getName().equals("m_6643_");
            case "getItem" -> method.getName().equals("m_8020_") || method.getName().equals("m_41720_");
            case "getMainHandItem" -> method.getName().equals("m_21205_");
            case "getOffhandItem" -> method.getName().equals("m_21206_");
            case "getCount" -> method.getName().equals("m_41613_");
            case "isEmpty" -> method.getName().equals("m_41619_");
            case "copy" -> method.getName().equals("m_41777_");
            case "shrink" -> method.getName().equals("m_41774_");
            case "level" -> method.getName().equals("m_9236_");
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
        throw new NoSuchMethodException(name);
    }

    private static boolean compatible(Class<?>[] types, Object[] arguments) {
        if (types.length != arguments.length) {
            return false;
        }
        for (var index = 0; index < types.length; index++) {
            if (arguments[index] == null) {
                if (types[index].isPrimitive()) {
                    return false;
                }
            } else if (!box(types[index]).isInstance(arguments[index])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
