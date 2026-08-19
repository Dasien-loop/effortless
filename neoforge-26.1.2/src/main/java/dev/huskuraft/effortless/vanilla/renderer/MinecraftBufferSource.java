package dev.huskuraft.effortless.vanilla.renderer;

import dev.huskuraft.universal.api.renderer.BufferSource;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.VertexBuffer;
import net.minecraft.client.renderer.MultiBufferSource;

public record MinecraftBufferSource(
        MultiBufferSource.BufferSource refs
) implements BufferSource {

    @Override
    public VertexBuffer getBuffer(RenderLayer renderLayer) {
        var reference = renderLayer.reference();
        MinecraftRenderStateFactory.setup(reference);
        return new MinecraftVertexBuffer(refs.getBuffer(MinecraftRenderStateFactory.renderType(reference)));
    }

    @Override
    public void end() {
        refs.endBatch();
    }

}
