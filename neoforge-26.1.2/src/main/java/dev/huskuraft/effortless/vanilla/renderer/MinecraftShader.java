package dev.huskuraft.effortless.vanilla.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.Uniform;
import dev.huskuraft.universal.api.renderer.VertexFormat;
import dev.huskuraft.effortless.vanilla.core.MinecraftResourceLocation;

public record MinecraftShader(RenderPipeline refs) implements Shader {

    @Override
    public ResourceLocation getResource() {
        return new MinecraftResourceLocation(refs.getLocation());
    }

    @Override
    public VertexFormat getVertexFormat() {
        return () -> refs.getVertexFormat();
    }

    @Override
    public Uniform getUniform(String param) {
        return new MinecraftUniform(param);
    }
}
