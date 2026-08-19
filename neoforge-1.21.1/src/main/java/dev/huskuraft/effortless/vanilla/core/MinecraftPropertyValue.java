package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.universal.api.core.PropertyValue;

public record MinecraftPropertyValue(
        Comparable<?> refs
) implements PropertyValue {

}
