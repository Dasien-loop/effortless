package dev.huskuraft.effortless.vanilla.renderer;

import java.util.Optional;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.RenderStateFactory;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.Shaders;
import dev.huskuraft.universal.api.renderer.VertexFormat;
import dev.huskuraft.universal.api.renderer.VertexFormats;
import dev.huskuraft.universal.api.renderer.programs.CompositeRenderState;
import dev.huskuraft.universal.api.renderer.programs.RenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

@AutoService(RenderStateFactory.class)
public final class MinecraftRenderStateFactory implements RenderStateFactory {

    record LayerReference(RenderType renderType, Runnable setup, Runnable clear) {
    }

    private record StateValue(Object value, Runnable setup, Runnable clear) {
        StateValue(Object value) {
            this(value, () -> {}, () -> {});
        }
    }

    private record CompositeValue(
            StateValue texture,
            StateValue shader,
            StateValue transparency,
            StateValue depthTest,
            StateValue cull,
            StateValue lightmap,
            StateValue overlay,
            StateValue layering,
            StateValue output,
            StateValue texturing,
            StateValue writeMask,
            boolean affectOutline
    ) {
    }

    static RenderType renderType(Object reference) {
        return reference instanceof LayerReference layer ? layer.renderType() : (RenderType) reference;
    }

    static void setup(Object reference) {
        if (reference instanceof LayerReference layer) layer.setup().run();
    }

    static void clear(Object reference) {
        if (reference instanceof LayerReference layer) layer.clear().run();
    }

    private static StateValue state(RenderState state) {
        return state == null ? new StateValue(null) : (StateValue) state.refs();
    }

    @Override
    public RenderLayer createCompositeRenderLayer(String name,
                                                  VertexFormat vertexFormat,
                                                  VertexFormat.Mode vertexFormatMode,
                                                  int bufferSize,
                                                  boolean affectsCrumbling,
                                                  boolean sortOnUpload,
                                                  CompositeRenderState state) {
        var composite = (CompositeValue) state.refs();
        var pipeline = composite.shader().value() instanceof RenderPipeline value
                ? value
                : defaultPipeline(vertexFormatMode.reference());
        var pipelineBuilder = pipeline.toBuilder()
                .withLocation(Identifier.fromNamespaceAndPath("effortless", name))
                .withVertexFormat(vertexFormat.reference(), vertexFormatMode.reference());

        var transparency = (RenderState.TransparencyState.Type) composite.transparency().value();
        var writeMask = (boolean[]) composite.writeMask().value();
        int colorMask = writeMask == null || writeMask[0] ? ColorTargetState.WRITE_COLOR : ColorTargetState.WRITE_NONE;
        pipelineBuilder.withColorTargetState(new ColorTargetState(blend(transparency), colorMask));

        var depthFunction = (Integer) composite.depthTest().value();
        var compareOp = compareOp(depthFunction == null ? 515 : depthFunction);
        boolean writeDepth = writeMask == null || writeMask[1];
        pipelineBuilder.withDepthStencilState(new DepthStencilState(compareOp, writeDepth));

        var cull = (Boolean) composite.cull().value();
        if (cull != null) pipelineBuilder.withCull(cull);

        var renderSetup = RenderSetup.builder(pipelineBuilder.build())
                .bufferSize(bufferSize);
        if (affectsCrumbling) renderSetup.affectsCrumbling();
        if (sortOnUpload) renderSetup.sortOnUpload();
        if (Boolean.TRUE.equals(composite.lightmap().value())) renderSetup.useLightmap();
        if (Boolean.TRUE.equals(composite.overlay().value())) renderSetup.useOverlay();

        var texture = (RenderState.TextureState.Texture) composite.texture().value();
        if (texture != null) {
            renderSetup.withTexture("Sampler0", texture.location().reference());
        }

        var layering = (RenderState.LayeringState.Type) composite.layering().value();
        if (layering == RenderState.LayeringState.Type.VIEW_OFFSET_Z) {
            renderSetup.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING);
        }

        var output = (RenderState.OutputState.Target) composite.output().value();
        renderSetup.setOutputTarget(outputTarget(output));
        renderSetup.setOutline(composite.affectOutline()
                ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE
                : RenderSetup.OutlineProperty.NONE);

        var renderType = RenderType.create(name, renderSetup.createRenderSetup());
        return () -> new LayerReference(
                renderType,
                composite.texturing().setup(),
                composite.texturing().clear()
        );
    }

    @Override
    public CompositeRenderState createCompositeState(RenderState.TextureState textureState,
                                                     RenderState.ShaderState shaderState,
                                                     RenderState.TransparencyState transparencyState,
                                                     RenderState.DepthTestState depthTestState,
                                                     RenderState.CullState cullState,
                                                     RenderState.LightmapState lightmapState,
                                                     RenderState.OverlayState overlayState,
                                                     RenderState.LayeringState layeringState,
                                                     RenderState.OutputState outputState,
                                                     RenderState.TexturingState texturingState,
                                                     RenderState.WriteMaskState writeMaskState,
                                                     RenderState.LineState lineState,
                                                     RenderState.ColorLogicState colorLogicState,
                                                     boolean affectOutline) {
        var value = new CompositeValue(
                state(textureState),
                state(shaderState),
                state(transparencyState),
                state(depthTestState),
                state(cullState),
                state(lightmapState),
                state(overlayState),
                state(layeringState),
                state(outputState),
                state(texturingState),
                state(writeMaskState),
                affectOutline
        );
        return () -> value;
    }

    @Override
    public RenderState createRenderState(String name, Runnable setupState, Runnable clearState) {
        return () -> new StateValue(name, setupState, clearState);
    }

    @Override
    public RenderState.TextureState createTextureState(String name, RenderState.TextureState.Texture texture) {
        return () -> new StateValue(texture);
    }

    @Override
    public RenderState.ShaderState createShaderState(String name, Shader shader) {
        return () -> new StateValue(shader == null ? null : shader.reference());
    }

    @Override
    public RenderState.TransparencyState createTransparencyState(String name, RenderState.TransparencyState.Type type) {
        return () -> new StateValue(type);
    }

    @Override
    public RenderState.DepthTestState createDepthTestState(String name, int function) {
        return () -> new StateValue(function);
    }

    @Override
    public RenderState.CullState createCullState(String name, boolean cull) {
        return () -> new StateValue(cull);
    }

    @Override
    public RenderState.LightmapState createLightmapState(String name, boolean lightmap) {
        return () -> new StateValue(lightmap);
    }

    @Override
    public RenderState.OverlayState createOverlayState(String name, boolean overlay) {
        return () -> new StateValue(overlay);
    }

    @Override
    public RenderState.LayeringState createLayeringState(String name, RenderState.LayeringState.Type type) {
        return () -> new StateValue(type);
    }

    @Override
    public RenderState.OutputState createOutputState(String name, RenderState.OutputState.Target target) {
        return () -> new StateValue(target);
    }

    @Override
    public RenderState.TexturingState createTexturingState(String name, Runnable setupState, Runnable clearState) {
        return () -> new StateValue(name, setupState, clearState);
    }

    @Override
    public RenderState.OffsetTexturingState createOffsetTexturingState(String name, float offsetX, float offsetY) {
        return () -> new StateValue(name);
    }

    @Override
    public RenderState.WriteMaskState createWriteMaskState(String name, boolean writeColor, boolean writeDepth) {
        return () -> new StateValue(new boolean[]{writeColor, writeDepth});
    }

    @Override
    public RenderState.LineState createLineState(String name, Double width) {
        return () -> new StateValue(width);
    }

    @Override
    public RenderState.ColorLogicState createColorLogicState(String name, RenderState.ColorLogicState.Op op) {
        return () -> new StateValue(op);
    }

    @Override
    public Shader getShader(Shaders shaders) {
        var pipeline = switch (shaders) {
            case POSITION_COLOR_LIGHTMAP -> RenderPipelines.DEBUG_FILLED_BOX;
            case POSITION, POSITION_COLOR, POSITION_COLOR_TEX -> RenderPipelines.DEBUG_FILLED_BOX;
            case POSITION_TEX -> RenderPipelines.GUI_TEXTURED;
            case POSITION_COLOR_TEX_LIGHTMAP -> RenderPipelines.SOLID_BLOCK;
            case SOLID -> RenderPipelines.SOLID_BLOCK;
            case CUTOUT_MIPPED, CUTOUT -> RenderPipelines.CUTOUT_BLOCK;
            case TRANSLUCENT, TRANSLUCENT_MOVING_BLOCK, TRANSLUCENT_NO_CRUMBLING -> RenderPipelines.TRANSLUCENT_BLOCK;
            case ARMOR_CUTOUT_NO_CULL -> RenderPipelines.ARMOR_CUTOUT_NO_CULL;
            case ENTITY_SOLID -> RenderPipelines.ENTITY_SOLID;
            case ENTITY_CUTOUT, ENTITY_CUTOUT_NO_CULL -> RenderPipelines.ENTITY_CUTOUT;
            case ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> RenderPipelines.ENTITY_CUTOUT_Z_OFFSET;
            case ITEM_ENTITY_TRANSLUCENT_CULL, ENTITY_TRANSLUCENT_CULL -> RenderPipelines.ENTITY_TRANSLUCENT_CULL;
            case ENTITY_TRANSLUCENT -> RenderPipelines.ENTITY_TRANSLUCENT;
            case ENTITY_TRANSLUCENT_EMISSIVE -> RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
            case ENTITY_SMOOTH_CUTOUT -> RenderPipelines.ENTITY_CUTOUT;
            case BEACON_BEAM -> RenderPipelines.BEACON_BEAM_TRANSLUCENT;
            case ENTITY_DECAL, ENTITY_NO_OUTLINE -> RenderPipelines.ENTITY_CUTOUT;
            case ENTITY_SHADOW -> RenderPipelines.ENTITY_SHADOW;
            case ENTITY_ALPHA -> RenderPipelines.ENTITY_TRANSLUCENT;
            case EYES -> RenderPipelines.EYES;
            case ENERGY_SWIRL -> RenderPipelines.ENERGY_SWIRL;
            case LEASH -> RenderPipelines.LEASH;
            case WATER_MASK -> RenderPipelines.WATER_MASK;
            case OUTLINE -> RenderPipelines.OUTLINE_NO_CULL;
            case ARMOR_GLINT, ARMOR_ENTITY_GLINT, GLINT_TRANSLUCENT, GLINT, GLINT_DIRECT, ENTITY_GLINT, ENTITY_GLINT_DIRECT -> RenderPipelines.GLINT;
            case CRUMBLING -> RenderPipelines.CRUMBLING;
            case TEXT -> RenderPipelines.TEXT;
            case TEXT_BACKGROUND -> RenderPipelines.TEXT_BACKGROUND;
            case TEXT_INTENSITY -> RenderPipelines.TEXT_INTENSITY;
            case TEXT_SEE_THROUGH -> RenderPipelines.TEXT_SEE_THROUGH;
            case TEXT_BACKGROUND_SEE_THROUGH -> RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH;
            case TEXT_INTENSITY_SEE_THROUGH -> RenderPipelines.TEXT_INTENSITY_SEE_THROUGH;
            case LIGHTNING -> RenderPipelines.LIGHTNING;
            case TRIPWIRE -> RenderPipelines.TRANSLUCENT_BLOCK;
            case END_PORTAL -> RenderPipelines.END_PORTAL;
            case END_GATEWAY -> RenderPipelines.END_GATEWAY;
            case LINES -> RenderPipelines.LINES;
            case GUI, GUI_OVERLAY, GUI_GHOST_RECIPE_OVERLAY -> RenderPipelines.GUI;
            case GUI_TEXT_HIGHLIGHT -> RenderPipelines.GUI_TEXT_HIGHLIGHT;
        };
        return new MinecraftShader(pipeline);
    }

    @Override
    public VertexFormat getVertexFormat(VertexFormats formats) {
        var format = switch (formats) {
            case BLIT_SCREEN -> DefaultVertexFormat.POSITION_TEX;
            case BLOCK -> DefaultVertexFormat.BLOCK;
            case NEW_ENTITY -> DefaultVertexFormat.ENTITY;
            case PARTICLE -> DefaultVertexFormat.PARTICLE;
            case POSITION -> DefaultVertexFormat.POSITION;
            case POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_COLOR_NORMAL -> DefaultVertexFormat.POSITION_COLOR_NORMAL;
            case POSITION_COLOR_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_LIGHTMAP;
            case POSITION_TEX -> DefaultVertexFormat.POSITION_TEX;
            case POSITION_COLOR_TEX -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_TEX_COLOR -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_COLOR_TEX_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
            case POSITION_TEX_LIGHTMAP_COLOR -> DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR;
            case POSITION_TEX_COLOR_NORMAL -> DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        };
        return () -> format;
    }

    @Override
    public VertexFormat.Mode getVertexFormatMode(VertexFormats.Modes modes) {
        var mode = switch (modes) {
            case LINES -> com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES;
            case LINE_STRIP -> com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINE_STRIP;
            case DEBUG_LINES -> com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINES;
            case DEBUG_LINE_STRIP -> com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINE_STRIP;
            case TRIANGLES -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES;
            case TRIANGLE_STRIP -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN;
            case QUADS -> com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS;
        };
        return () -> mode;
    }

    private static RenderPipeline defaultPipeline(com.mojang.blaze3d.vertex.VertexFormat.Mode mode) {
        return mode == com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES
                || mode == com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINES
                || mode == com.mojang.blaze3d.vertex.VertexFormat.Mode.DEBUG_LINE_STRIP
                ? RenderPipelines.LINES
                : RenderPipelines.DEBUG_FILLED_BOX;
    }

    private static Optional<BlendFunction> blend(RenderState.TransparencyState.Type type) {
        if (type == null || type == RenderState.TransparencyState.Type.NO) return Optional.empty();
        return Optional.of(switch (type) {
            case NO -> throw new IllegalStateException();
            case ADDITIVE -> BlendFunction.ADDITIVE;
            case LIGHTNING -> BlendFunction.LIGHTNING;
            case GLINT -> BlendFunction.GLINT;
            case CRUMBLING, TRANSLUCENT -> BlendFunction.TRANSLUCENT;
        });
    }

    private static CompareOp compareOp(int function) {
        return switch (function) {
            case 512 -> CompareOp.NEVER_PASS;
            case 513 -> CompareOp.LESS_THAN;
            case 514 -> CompareOp.EQUAL;
            case 515 -> CompareOp.LESS_THAN_OR_EQUAL;
            case 516 -> CompareOp.GREATER_THAN;
            case 517 -> CompareOp.NOT_EQUAL;
            case 518 -> CompareOp.GREATER_THAN_OR_EQUAL;
            case 519 -> CompareOp.ALWAYS_PASS;
            default -> CompareOp.LESS_THAN_OR_EQUAL;
        };
    }

    private static OutputTarget outputTarget(RenderState.OutputState.Target target) {
        if (target == null) return OutputTarget.MAIN_TARGET;
        return switch (target) {
            case OUTLINE -> OutputTarget.OUTLINE_TARGET;
            case WEATHER -> OutputTarget.WEATHER_TARGET;
            case ITEM_ENTITY -> OutputTarget.ITEM_ENTITY_TARGET;
            case NO, TRANSLUCENT, PARTICLES, CLOUDS -> OutputTarget.MAIN_TARGET;
        };
    }
}
