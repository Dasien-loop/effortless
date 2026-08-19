package dev.huskuraft.effortless.vanilla.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.huskuraft.universal.api.renderer.VertexBuffer;

public record MinecraftVertexBuffer(
        VertexConsumer refs
) implements VertexBuffer {

    @Override
    public MinecraftVertexBuffer vertex(double x, double y, double z) {
        refs.addVertex((float) x, (float) y, (float) z);
        return this;
    }

    @Override
    public MinecraftVertexBuffer color(int red, int green, int blue, int alpha) {
        refs.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public MinecraftVertexBuffer uv(float u, float v) {
        refs.setUv(u, v);
        return this;
    }

    @Override
    public MinecraftVertexBuffer overlayCoords(int u, int v) {
        refs.setUv1(u, v);
        return this;
    }

    @Override
    public MinecraftVertexBuffer uv2(int u, int v) {
        refs.setUv2(u, v);
        return this;
    }

    @Override
    public MinecraftVertexBuffer normal(float x, float y, float z) {
        refs.setNormal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        // no-op
    }

    @Override
    public void defaultColor(int defaultR, int defaultG, int defaultB, int defaultA) {
        // VertexConsumer no longer stores a default color in 1.21.
    }

    @Override
    public void unsetDefaultColor() {
        // VertexConsumer no longer stores a default color in 1.21.
    }

}
