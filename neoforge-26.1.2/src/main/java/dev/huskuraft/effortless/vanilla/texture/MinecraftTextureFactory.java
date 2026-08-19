package dev.huskuraft.effortless.vanilla.texture;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.texture.SimpleTexture;
import dev.huskuraft.universal.api.texture.SimpleTextureSprite;
import dev.huskuraft.universal.api.texture.SpriteScaling;
import dev.huskuraft.universal.api.texture.Texture;
import dev.huskuraft.universal.api.texture.TextureFactory;
import dev.huskuraft.universal.api.texture.TextureSprite;
import dev.huskuraft.effortless.vanilla.core.MinecraftResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.TextureAtlas;

@AutoService(TextureFactory.class)
public final class MinecraftTextureFactory implements TextureFactory {

    @Override
    public Texture getBlockAtlasTexture() {
        return new SimpleTexture(new MinecraftResourceLocation(TextureAtlas.LOCATION_BLOCKS));
    }

    @Override
    public TextureSprite getBackgroundTextureSprite() {
        return null;
    }

    @Override
    public TextureSprite getButtonTextureSprite(boolean enabled, boolean focused) {
        var path = !enabled ? "widget/button_disabled" : focused ? "widget/button_highlighted" : "widget/button";
        return createTextureSprite(Identifier.withDefaultNamespace(path));
    }

    @Override
    public TextureSprite getDemoBackgroundTextureSprite() {
        return createTextureSprite(Identifier.withDefaultNamespace("textures/gui/demo_background.png"), null, 248, 166, 0, 0, 256, 256, new SpriteScaling.NineSlice(248, 166, 6));
    }

    public TextureSprite createTextureSprite(Identifier texture, Identifier name, int width, int height, int x, int y, int textureWidth, int textureHeight, SpriteScaling scaling) {
        return new SimpleTextureSprite(MinecraftResourceLocation.ofNullable(texture), MinecraftResourceLocation.ofNullable(name), width, height, x, y, textureWidth, textureHeight, scaling);
    }

    public TextureSprite createTextureSprite(Identifier name) {
        var location = new MinecraftResourceLocation(name);
        return new SimpleTextureSprite(location, location, 1, 1, 0, 0, 1, 1, new SpriteScaling.Stretch());
    }

    public TextureSprite createTextureSprite(TextureAtlasSprite sprite) {
        return new SimpleTextureSprite(
                new MinecraftResourceLocation(sprite.atlasLocation()),
                new MinecraftResourceLocation(sprite.contents().name()),
                sprite.contents().width(),
                sprite.contents().height(),
                sprite.getX(),
                sprite.getY(),
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1(),
                getSpriteScaling(sprite)
        );
    }

    public SpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return new SpriteScaling.Stretch();
    }

}
