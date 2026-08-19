package dev.huskuraft.effortless.vanilla.renderer;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.huskuraft.universal.api.core.BlockEntity;
import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.gui.Typeface;
import dev.huskuraft.universal.api.renderer.BufferSource;
import dev.huskuraft.universal.api.renderer.MatrixStack;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.effortless.vanilla.core.MinecraftConvertor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class MinecraftRenderer extends Renderer {

    private static final RandomSource RAND = RandomSource.create();
    private final Minecraft minecraftClient;
    private final PoseStack minecraftMatrixStack;
    private final MultiBufferSource.BufferSource minecraftBufferSource;
    private final GuiGraphicsExtractor minecraftRendererProvider;
    private final SubmitNodeCollector submitNodeCollector;
    private final CameraRenderState cameraRenderState;

    public MinecraftRenderer(PoseStack minecraftMatrixStack) {
        this.minecraftClient = Minecraft.getInstance();
        this.minecraftMatrixStack = minecraftMatrixStack;
        this.minecraftBufferSource = minecraftClient.renderBuffers().bufferSource();
        this.minecraftRendererProvider = null;
        this.submitNodeCollector = null;
        this.cameraRenderState = null;
    }

    public MinecraftRenderer(PoseStack minecraftMatrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        this.minecraftClient = Minecraft.getInstance();
        this.minecraftMatrixStack = minecraftMatrixStack;
        this.minecraftBufferSource = minecraftClient.renderBuffers().bufferSource();
        this.minecraftRendererProvider = null;
        this.submitNodeCollector = submitNodeCollector;
        this.cameraRenderState = cameraRenderState;
    }

    public MinecraftRenderer(GuiGraphicsExtractor minecraftRendererProvider) {
        this.minecraftClient = Minecraft.getInstance();
        this.minecraftMatrixStack = new PoseStack();
        this.minecraftBufferSource = minecraftClient.renderBuffers().bufferSource();
        this.minecraftRendererProvider = minecraftRendererProvider;
        this.submitNodeCollector = null;
        this.cameraRenderState = null;
    }

    @Override
    public MatrixStack matrixStack() {
        return new MinecraftMatrixStack(minecraftMatrixStack);
    }

    @Override
    protected void enableScissor(int x, int y, int width, int height) {
        if (minecraftRendererProvider != null) {
            minecraftRendererProvider.enableScissor(x, y, x + width, y + height);
        }
    }

    @Override
    protected void disableScissor() {
        if (minecraftRendererProvider != null) {
            minecraftRendererProvider.disableScissor();
        }
    }

    @Override
    public void setRsShaderColor(float red, float green, float blue, float alpha) {
//        minecraftRendererProvider.flushIfManaged();
        // Shader color is part of pipeline state in 26.1.2.
    }

    @Override
    protected boolean renderRectInternal(RenderLayer renderLayer, int x1, int y1, int x2, int y2, int color, int z) {
        if (minecraftRendererProvider == null) {
            return false;
        }
        var first = transform(x1, y1);
        var second = transform(x2, y2);
        minecraftRendererProvider.fill(Math.min(first[0], second[0]), Math.min(first[1], second[1]), Math.max(first[0], second[0]), Math.max(first[1], second[1]), color);
        return true;
    }

    @Override
    protected boolean renderGradientRectInternal(RenderLayer renderLayer, int x1, int y1, int x2, int y2, int color1, int color2, int z) {
        if (minecraftRendererProvider == null) {
            return false;
        }
        var first = transform(x1, y1);
        var second = transform(x2, y2);
        minecraftRendererProvider.fillGradient(Math.min(first[0], second[0]), Math.min(first[1], second[1]), Math.max(first[0], second[0]), Math.max(first[1], second[1]), color1, color2);
        return true;
    }

    @Override
    protected boolean renderQuadInternal(RenderLayer renderLayer, int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int offset, int color) {
        if (minecraftRendererProvider == null) {
            return false;
        }
        var points = new int[][]{
                transform(x1, y1),
                transform(x2, y2),
                transform(x3, y3),
                transform(x4, y4)
        };
        var minX = java.util.Arrays.stream(points).mapToInt(point -> point[0]).min().orElse(0);
        var maxX = java.util.Arrays.stream(points).mapToInt(point -> point[0]).max().orElse(0);
        var minY = java.util.Arrays.stream(points).mapToInt(point -> point[1]).min().orElse(0);
        var maxY = java.util.Arrays.stream(points).mapToInt(point -> point[1]).max().orElse(0);
        minecraftRendererProvider.submitGuiElementRenderState(new QuadRenderState(
                points,
                color,
                new ScreenRectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY)),
                minecraftRendererProvider.peekScissorStack()
        ));
        return true;
    }

    @Override
    protected boolean renderTextureInternal(dev.huskuraft.universal.api.core.ResourceLocation location, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        if (minecraftRendererProvider == null) {
            return false;
        }
        var first = transform(x1, y1);
        var second = transform(x2, y2);
        // This overload takes the bottom-right coordinates, not width/height.
        // Passing the latter clipped every icon whose origin was away from the
        // top-left corner of the screen.
        minecraftRendererProvider.blit(
                location.reference(),
                Math.min(first[0], second[0]),
                Math.min(first[1], second[1]),
                Math.max(first[0], second[0]),
                Math.max(first[1], second[1]),
                minU,
                maxU,
                minV,
                maxV
        );
        return true;
    }

    private int[] transform(float x, float y) {
        var position = minecraftMatrixStack.last().pose().transformPosition(x, y, 0, new org.joml.Vector3f());
        return new int[]{Math.round(position.x), Math.round(position.y)};
    }

    private record QuadRenderState(
            int[][] points,
            int color,
            ScreenRectangle bounds,
            ScreenRectangle scissorArea
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(com.mojang.blaze3d.vertex.VertexConsumer consumer) {
            for (var point : points) {
                consumer.addVertex(point[0], point[1], 0).setColor(color);
            }
        }

        @Override
        public com.mojang.blaze3d.pipeline.RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }

    @Override
    public BufferSource bufferSource() {
        return new MinecraftBufferSource(minecraftBufferSource);
    }

    @Override
    public void flush() {
        minecraftBufferSource.endBatch();
    }

    @Override
    protected int renderTextInternal(Typeface typeface, Text text, int x, int y, int color, int backgroundColor, boolean shadow, boolean seeThrough, int lightMap) {
        var minecraftTypeface = (Font) typeface.reference();
        var minecraftText = (Component) text.reference();
        if (minecraftRendererProvider != null) {
            var position = transform(x, y);
            minecraftRendererProvider.text(minecraftTypeface, minecraftText, position[0], position[1], color, shadow);
            return minecraftTypeface.width(minecraftText);
        }
        minecraftTypeface.drawInBatch(minecraftText,
                x,
                y,
                color,
                shadow,
                minecraftMatrixStack.last().pose(),
                minecraftBufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                backgroundColor,
                lightMap);
        return minecraftTypeface.width(minecraftText);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
////        RenderSystem.getModelViewStack().pushPose();
////        RenderSystem.getModelViewStack().mulPoseMatrix(minecraftMatrixStack.last().pose());
////        RenderSystem.applyModelViewMatrix();
        if (minecraftRendererProvider != null) {
            var position = transform(x, y);
            minecraftRendererProvider.item(stack.reference(), position[0], position[1]);
        }
////        RenderSystem.getModelViewStack().popPose();
////        RenderSystem.applyModelViewMatrix();
    }

    @Override
    public void renderTooltip(Typeface typeface, List<Text> list, int x, int y) {
        if (minecraftRendererProvider != null) {
            minecraftRendererProvider.setComponentTooltipForNextFrame(typeface.reference(), list.stream().map(text -> (Component) text.reference()).toList(), x, y);
        }
    }

    @Override
    public void renderBlockState(RenderLayer renderLayer, World world, BlockPosition blockPosition, BlockState blockState) {
        var minecraftBlockState = (net.minecraft.world.level.block.state.BlockState) blockState.reference();
        if (submitNodeCollector == null) return;
        var state = new net.minecraft.client.renderer.block.BlockModelRenderState();
        minecraftClient.getBlockModelResolver().update(
                state,
                minecraftBlockState,
                net.minecraft.client.renderer.block.model.BlockDisplayContext.create()
        );
        state.submit(minecraftMatrixStack, submitNodeCollector, 0x00F000F0, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    public void renderBlockEntity(RenderLayer renderLayer, World world, BlockPosition blockPosition, BlockEntity blockEntity) {
        var minecraftBlockEntityRenderDispatcher = minecraftClient.getBlockEntityRenderDispatcher();
        var minecraftBlockEntity = (net.minecraft.world.level.block.entity.BlockEntity) blockEntity.reference();

        minecraftBlockEntity.setLevel(world.reference());
        if (submitNodeCollector == null || cameraRenderState == null) return;
        var state = minecraftBlockEntityRenderDispatcher.tryExtractRenderState(minecraftBlockEntity, 0f, null);
        if (state != null) {
            minecraftBlockEntityRenderDispatcher.submit(state, minecraftMatrixStack, submitNodeCollector, cameraRenderState);
        }
    }


}
