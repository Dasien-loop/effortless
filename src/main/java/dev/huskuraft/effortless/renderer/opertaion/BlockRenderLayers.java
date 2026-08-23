package dev.huskuraft.effortless.renderer.opertaion;

import java.util.HashMap;
import java.util.Map;

import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.RenderLayers;
import dev.huskuraft.universal.api.renderer.Shaders;
import dev.huskuraft.universal.api.renderer.VertexFormats;
import dev.huskuraft.universal.api.renderer.programs.RenderState;

public abstract class BlockRenderLayers extends RenderLayers {

    // TODO: 16/1/24 use func
    public static final RenderLayer EF_PLANES = RenderLayer.createComposite("ef_planes",
            VertexFormats.POSITION_COLOR,
            VertexFormats.Modes.QUADS,
            256,
            RenderState.builder()
                    .setLineState(RenderLayers.NO_WIDTH)
                    .setShaderState(RenderLayers.POSITION_COLOR_SHADER_STATE)
                    .setLayeringState(RenderLayers.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderLayers.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderLayers.COLOR_WRITE)
                    .setCullState(RenderLayers.NO_CULL)
                    .create(false));
    protected static final Map<String, RenderLayer> BLOCK_RENDER_TYPES = new HashMap<>();
    protected static final RenderLayer EF_LINES = RenderLayer.createComposite("ef_lines",
            VertexFormats.POSITION_COLOR_NORMAL,
            VertexFormats.Modes.DEBUG_LINES,
            256,
            RenderState.builder()
                    .setLineState(RenderLayers.NO_WIDTH)
                    .setShaderState(RenderLayers.LINES_SHADER_STATE)
                    .setLayeringState(RenderLayers.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderLayers.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderLayers.COLOR_DEPTH_WRITE)
                    .setCullState(RenderLayers.NO_CULL)
                    .create(false));

    public static RenderLayer lines() {
        return EF_LINES;
    }

    public static RenderLayer planes() {
        return EF_PLANES;
    }

    public static RenderLayer block(int color) {
        return BLOCK_RENDER_TYPES.computeIfAbsent(Integer.toString(color), k -> {
            var texture = RenderState.TexturingState.create("block_texturing_" + k, () -> {
                var modulator = Shaders.TRANSLUCENT_MOVING_BLOCK.getUniform("ColorModulator");
                if (modulator != null) {
                    modulator.set((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
                }
            }, () -> {
                var modulator = Shaders.TRANSLUCENT_MOVING_BLOCK.getUniform("ColorModulator");
                if (modulator != null) {
                    modulator.set(1f, 1f, 1f, 1f);
                }
            });
            var renderState = RenderState.builder()
                    .setShaderState(RenderLayers.TRANSLUCENT_MOVING_BLOCK_SHADER_STATE)
                    .setTexturingState(texture)
                    .setTransparencyState(RenderLayers.TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(RenderLayers.BLOCK_SHEET_MIPPED_TEXTURE)
                    .setLightmapState(RenderLayers.LIGHTMAP)
                    .setCullState(RenderLayers.NO_CULL)
                    .create(false);
            return RenderLayer.createComposite("ef_block_previews_" + k, VertexFormats.BLOCK, VertexFormats.Modes.QUADS, 2097152, true, false, renderState);
        });
    }
}



