package dev.huskuraft.effortless.vanilla.renderer;

import dev.huskuraft.universal.api.renderer.Uniform;

public final class MinecraftUniform implements Uniform {

    private final String name;

    public MinecraftUniform(String name) {
        this.name = name;
    }

    @Override
    public Object refs() {
        return this;
    }

    @Override public void set(float x) {}
    @Override public void set(float x, float y) {}
    @Override public void set(float x, float y, float z) {}

    @Override
    public void set(float x, float y, float z, float w) {
        // Custom uniforms are supplied by RenderPipeline state in 26.1.2.
    }

    @Override public void setSafe(float x, float y, float z, float w) { set(x, y, z, w); }
    @Override public void setSafe(int x, int y, int z, int w) {}
    @Override public void set(int x) {}
    @Override public void set(int x, int y) {}
    @Override public void set(int x, int y, int z) {}
    @Override public void set(int x, int y, int z, int w) {}
    @Override public void set(float[] values) {
        if (values.length >= 4) set(values[0], values[1], values[2], values[3]);
    }
    @Override public void setMatrix22(float m00, float m01, float m10, float m11) {}
    @Override public void setMatrix23(float m00, float m01, float m02, float m10, float m11, float m12) {}
    @Override public void setMatrix24(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {}
    @Override public void setMatrix32(float m00, float m01, float m10, float m11, float m20, float m21) {}
    @Override public void setMatrix33(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {}
    @Override public void setMatrix34(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {}
    @Override public void setMatrix42(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13) {}
    @Override public void setMatrix43(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23) {}
    @Override public void setMatrix44(float m00, float m01, float m02, float m03, float m10, float m11, float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31, float m32, float m33) {}
}
