package dev.huskuraft.effortless.vanilla.gui;

import dev.huskuraft.universal.api.gui.Screen;
import dev.huskuraft.effortless.vanilla.renderer.MinecraftRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class MinecraftProxyScreen extends net.minecraft.client.gui.screens.Screen {

    private final Screen proxy;

    public MinecraftProxyScreen(Screen screen) {
        super(screen.getScreenTitle().reference());
        this.proxy = screen;
    }

    public Screen getProxy() {
        return proxy;
    }

    @Override
    public void tick() {
        proxy.onTick();
    }

    @Override
    protected void init() {
        proxy.init(width, height);
    }

    @Override
    protected void rebuildWidgets() {
        proxy.init(width, height);
    }

    @Override
    public void removed() {
        proxy.onDestroy();
    }

    @Override
    public void onClose() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor minecraftRenderer, int i, int j, float f) {
        var renderer = new MinecraftRenderer(minecraftRenderer);
        proxy.render(renderer, i, j, f);
        proxy.renderOverlay(renderer, i, j, f);
    }

    @Override
    public boolean isPauseScreen() {
        return proxy.isPauseGame();
    }

    @Override
    public void mouseMoved(double d, double e) {
        proxy.onMouseMoved(d, e);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return proxy.onMouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return proxy.onMouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return proxy.onMouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return proxy.onMouseScrolled(d, e, f, g);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return proxy.onKeyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return proxy.onKeyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return proxy.onCharTyped((char) event.codepoint(), 0);
    }

    //    @Override
    @Deprecated
    public boolean changeFocus(boolean bl) {
        return proxy.onFocusMove(bl);
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        return proxy.isMouseOver(d, e);
    }

    @Override
    public Component getTitle() {
        return proxy.getScreenTitle().reference();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MinecraftProxyScreen keyBinding && proxy.equals(keyBinding.proxy);
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }
}
