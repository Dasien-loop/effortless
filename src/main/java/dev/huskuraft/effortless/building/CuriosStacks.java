package dev.huskuraft.effortless.building;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.huskuraft.effortless.Effortless;

/** Optional Curios inventory bridge. It has no hard Curios class references. */
public final class CuriosStacks {
    private static final AtomicBoolean REPORTED_COLLECTION_FAILURE = new AtomicBoolean();
    public record LocatedStack(Object stack, String identifier, int index) {
    }

    private CuriosStacks() {
    }

    public static List<Object> collect(Object player) {
        return collectLocated(player).stream().map(LocatedStack::stack).toList();
    }

    /**
     * Returns Curios stacks together with their slot identity when Curios
     * exposes it. AE2WTLib needs this locator to create a terminal host for a
     * universal terminal stored outside the player's normal inventory.
     */
    public static List<LocatedStack> collectLocated(Object player) {
        var direct = invokeDirectLocatedBridge(player);
        if (!direct.isEmpty()) {
            return direct;
        }
        try {
            var api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var handler = getCuriosInventory(api, player);
            var mapped = collectLocatedFromInventory(handler);
            if (!mapped.isEmpty()) {
                return mapped;
            }
            // Some Curios integrations expose the equipped item handler but
            // do not populate the keyed map until after the inventory screen
            // has opened. The equipped handler is still authoritative for
            // discovering terminals; the AE2 item-link fallback does not
            // require a Curios locator.
            var equipped = collectEquippedLocated(handler);
            if (!equipped.isEmpty()) {
                return equipped;
            }
            var helper = api.getMethod("getCuriosHelper").invoke(null);
            // Prefer the keyed handler map so the exact slot identifier is
            // preserved even when another mod wraps findCurios.
            var helperSlots = collectLocatedFromHandler(helper, player);
            if (!helperSlots.isEmpty()) {
                return helperSlots;
            }
            var method = findTwoArgumentMethod(helper.getClass(), "findCurios", player,
                    java.util.function.Predicate.class);
            if (method == null) {
                return List.of();
            }
            var predicate = (java.util.function.Predicate<Object>) stack -> true;
            var results = method.invoke(helper, player, predicate);
            if (!(results instanceof Iterable<?> iterable)) {
                return collectLocatedFromHandler(helper, player);
            }
            var stacks = new ArrayList<LocatedStack>();
            for (var result : iterable) {
                var stack = result.getClass().getMethod("stack").invoke(result);
                var context = result.getClass().getMethod("slotContext").invoke(result);
                if (stack == null || context == null
                        || isEmptyStack(stack)) {
                    continue;
                }
                var identifier = (String) context.getClass().getMethod("identifier").invoke(context);
                var index = ((Number) context.getClass().getMethod("index").invoke(context)).intValue();
                stacks.add(new LocatedStack(stack, identifier, index));
            }
            if (!stacks.isEmpty()) {
                return stacks;
            }
            var handlerStacks = collectLocatedFromHandler(helper, player);
            if (!handlerStacks.isEmpty()) {
                return handlerStacks;
            }
            return List.of();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            reportFailure("located Curios inventory", exception);
            return List.of();
        }
    }

    /**
     * Reads Curios' keyed handler map directly. This is more robust in packs
     * that replace or wrap the helper's predicate-based findCurios method.
     */
    private static List<LocatedStack> collectLocatedFromHandler(Object helper, Object player)
            throws ReflectiveOperationException {
        var method = findSingleArgumentMethod(helper.getClass(), "getCuriosHandler", player);
        if (method == null) {
            return List.of();
        }
        return collectLocatedFromInventory(unwrap(method.invoke(helper, player)));
    }

    private static Object getCuriosInventory(Class<?> api, Object player) throws ReflectiveOperationException {
        var method = findSingleArgumentMethod(api, "getCuriosInventory", player);
        return method == null ? null : unwrap(method.invoke(null, player));
    }

    private static List<LocatedStack> collectLocatedFromInventory(Object handler)
            throws ReflectiveOperationException {
        if (handler == null) {
            return List.of();
        }
        var curios = invokePublicMethod(handler, "getCurios");
        if (!(curios instanceof java.util.Map<?, ?> map)) {
            return List.of();
        }
        var result = new ArrayList<LocatedStack>();
        for (var entry : map.entrySet()) {
            try {
                var stacksHandler = entry.getValue();
                if (stacksHandler == null) continue;
                var identifier = entry.getKey() instanceof String key ? key : null;
                if (identifier == null) {
                    try {
                        identifier = (String) invokePublicMethod(stacksHandler, "getIdentifier");
                    } catch (NoSuchMethodException ignored) {
                        // The map key is the identifier in older Curios builds.
                    }
                }
                if (identifier == null) continue;
                var stacks = invokePublicMethod(stacksHandler, "getStacks");
                var slots = ((Number) invokePublicMethod(stacks, "getSlots")).intValue();
                for (var index = 0; index < slots; index++) {
                    var stack = invokePublicMethod(stacks, "getStackInSlot", new Class<?>[] {int.class}, index);
                    if (stack != null && !isEmptyStack(stack)) {
                        result.add(new LocatedStack(stack, identifier, index));
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // A third-party slot handler must not hide the remaining slots.
            }
        }
        return result;
    }

    private static List<LocatedStack> collectEquippedLocated(Object handler)
            throws ReflectiveOperationException {
        if (handler == null) {
            return List.of();
        }
        var equipped = unwrap(invokePublicMethod(handler, "getEquippedCurios"));
        if (equipped == null) {
            return List.of();
        }
        var slots = ((Number) invokePublicMethod(equipped, "getSlots")).intValue();
        var result = new ArrayList<LocatedStack>();
        for (var index = 0; index < slots; index++) {
            var stack = invokePublicMethod(equipped, "getStackInSlot", new Class<?>[] {int.class}, index);
            if (stack != null && !isEmptyStack(stack)) {
                result.add(new LocatedStack(stack, null, -1));
            }
        }
        return result;
    }

    private static java.lang.reflect.Method findSingleArgumentMethod(Class<?> owner, String name, Object argument) {
        for (var method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isInstance(argument)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Curios returns package-private capability implementations. Invoking a
     * method obtained from that implementation class fails access checks even
     * though its public interface is callable. Resolve the interface method.
     */
    private static Object invokePublicMethod(Object target, String name, Class<?>[] parameters, Object... arguments)
            throws ReflectiveOperationException {
        var method = findPublicInterfaceMethod(target.getClass(), name, parameters);
        if (method == null) {
            method = target.getClass().getMethod(name, parameters);
        }
        return method.invoke(target, arguments);
    }

    private static Object invokePublicMethod(Object target, String name) throws ReflectiveOperationException {
        return invokePublicMethod(target, name, new Class<?>[0]);
    }

    private static java.lang.reflect.Method findPublicInterfaceMethod(
            Class<?> type, String name, Class<?>[] parameters) {
        for (var parent : type.getInterfaces()) {
            try {
                return parent.getMethod(name, parameters);
            } catch (NoSuchMethodException ignored) {
                var nested = findPublicInterfaceMethod(parent, name, parameters);
                if (nested != null) return nested;
            }
        }
        var superclass = type.getSuperclass();
        return superclass == null ? null : findPublicInterfaceMethod(superclass, name, parameters);
    }

    private static java.lang.reflect.Method findTwoArgumentMethod(
            Class<?> owner, String name, Object firstArgument, Class<?> secondArgument) {
        for (var method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 2
                    && method.getParameterTypes()[0].isInstance(firstArgument)
                    && method.getParameterTypes()[1].isAssignableFrom(secondArgument)) {
                return method;
            }
        }
        return null;
    }

    private static Object unwrap(Object value) throws ReflectiveOperationException {
        if (value == null) return null;
        if (value instanceof java.util.Optional<?> optional) return optional.orElse(null);
        // Forge's LazyOptional is not required to accept null in orElse().
        // resolve() returns a standard Optional and is the same path used by
        // direct Curios integrations such as PortableSource.
        try {
            var resolved = value.getClass().getMethod("resolve").invoke(value);
            if (resolved instanceof java.util.Optional<?> optional) return optional.orElse(null);
        } catch (NoSuchMethodException ignored) {
            // Other optional wrappers expose only orElse(Object).
        }
        try {
            return value.getClass().getMethod("orElse", Object.class).invoke(value, new Object[] {null});
        } catch (NoSuchMethodException ignored) {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<LocatedStack> invokeDirectLocatedBridge(Object player) {
        for (var className : List.of(
                "dev.huskuraft.effortless.forge.integration.ForgeCuriosBridge",
                "dev.huskuraft.effortless.neoforge.integration.NeoForgeCuriosBridge")) {
            try {
                var bridge = Class.forName(className);
                java.lang.reflect.Method method = null;
                for (var candidate : bridge.getMethods()) {
                    if (candidate.getName().equals("collectLocated")
                            && candidate.getParameterCount() == 1
                            && candidate.getParameterTypes()[0].isInstance(player)) {
                        method = candidate;
                        break;
                    }
                }
                if (method != null) {
                    @SuppressWarnings("unchecked")
                    var result = (List<LocatedStack>) method.invoke(null, player);
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // Loader-specific Curios API is optional.
            }
        }
        return List.of();
    }

    private static void reportFailure(String operation, Throwable exception) {
        if (REPORTED_COLLECTION_FAILURE.compareAndSet(false, true)) {
            Effortless.LOGGER.warn("Unable to read {}", operation, exception);
        }
    }

    private static boolean isEmptyStack(Object stack) throws ReflectiveOperationException {
        for (var method : stack.getClass().getMethods()) {
            if ((method.getName().equals("isEmpty") || method.getName().equals("m_41619_"))
                    && method.getParameterCount() == 0) {
                method.setAccessible(true);
                return Boolean.TRUE.equals(method.invoke(stack));
            }
        }
        throw new NoSuchMethodException(stack.getClass().getName() + "#isEmpty");
    }
}
