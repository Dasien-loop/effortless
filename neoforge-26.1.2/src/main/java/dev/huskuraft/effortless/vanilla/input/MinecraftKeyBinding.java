package dev.huskuraft.effortless.vanilla.input;

import dev.huskuraft.universal.api.input.Key;
import dev.huskuraft.universal.api.input.KeyBinding;
import net.minecraft.client.KeyMapping;

public record MinecraftKeyBinding(
        KeyMapping refs
) implements KeyBinding {

    @Override
    public String getName() {
        return refs.getName();
    }

    @Override
    public String getCategory() {
        return refs.getCategory().id().toString();
    }

    @Override
    public Key getDefaultKey() {
        return new MinecraftKey(refs.getDefaultKey());
    }

    @Override
    public Key getKey() {
        return new MinecraftKey(refs.getKey());
    }

    @Override
    public boolean consumeClick() {
        return refs.consumeClick();
    }

    @Override
    public boolean isDown() {
        return refs.isDown();
    }

}
