package dev.huskuraft.effortless.vanilla.renderer;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.Uniform;
import dev.huskuraft.universal.api.renderer.VertexFormat;

public record MinecraftShader(
        net.minecraft.client.renderer.ShaderInstance refs
) implements Shader {

    @Override
    public ResourceLocation getResource() {
        return ResourceLocation.vanilla(refs.getName());
    }

    public VertexFormat getVertexFormat() {
        return () -> refs.getVertexFormat();
    }

    public Uniform getUniform(String param) {
        return new MinecraftUniform(refs.getUniform(param));
    }
}
