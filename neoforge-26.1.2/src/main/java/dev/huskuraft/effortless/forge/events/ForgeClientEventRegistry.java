package dev.huskuraft.effortless.forge.events;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.core.InteractionType;
import dev.huskuraft.universal.api.events.ClientEventRegistry;
import dev.huskuraft.universal.api.events.lifecycle.ClientTick;
import dev.huskuraft.universal.api.input.InputKey;
import dev.huskuraft.effortless.vanilla.core.MinecraftConvertor;
import dev.huskuraft.effortless.vanilla.platform.MinecraftClient;
import dev.huskuraft.effortless.vanilla.renderer.MinecraftRenderer;
import dev.huskuraft.effortless.vanilla.renderer.MinecraftShader;
import dev.huskuraft.effortless.forge.platform.ForgeInitializer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

@AutoService(ClientEventRegistry.class)
public class ForgeClientEventRegistry extends ClientEventRegistry {

    public ForgeClientEventRegistry() {
        ForgeInitializer.getModEventBus().addListener(this::onClientSetup);
        ForgeInitializer.getModEventBus().addListener(this::onRegisterKeyMappings);
        ForgeInitializer.getModEventBus().addListener(this::onReloadShader);

        NeoForge.EVENT_BUS.register(this);
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        getClientStartEvent().invoker().onClientStart(new MinecraftClient(Minecraft.getInstance()));
    }

    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        getRegisterKeysEvent().invoker().onRegisterKeys(key -> {
            event.register(key.getKeyBinding().reference());
        });
    }

    public void onReloadShader(RegisterRenderPipelinesEvent event) {
        getRegisterShaderEvent().invoker().onRegisterShader((resource, format, consumer) -> {
            var location = resource.<net.minecraft.resources.Identifier>reference();
            var pipeline = RenderPipelines.SOLID_BLOCK.toBuilder()
                    .withLocation(location)
                    .withVertexFormat(format.reference(), com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                    .build();
            event.registerPipeline(pipeline);
            consumer.accept(new MinecraftShader(pipeline));
        });
    }

    @SubscribeEvent
    public void onClientTickPre(ClientTickEvent.Pre event) {
        getClientTickEvent().invoker().onClientTick(new MinecraftClient(Minecraft.getInstance()), ClientTick.Phase.START);
    }

    @SubscribeEvent
    public void onClientTickPost(ClientTickEvent.Post event) {
        getClientTickEvent().invoker().onClientTick(new MinecraftClient(Minecraft.getInstance()), ClientTick.Phase.END);
    }

    @SubscribeEvent
    public void onRenderLevelStage(SubmitCustomGeometryEvent event) {
        var renderer = new MinecraftRenderer(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                event.getLevelRenderState().cameraRenderState
        );
        var partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        getRenderWorldEvent().invoker().onRenderWorld(renderer, partialTick);
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        getRenderGuiEvent().invoker().onRenderGui(new MinecraftRenderer(event.getGuiGraphics()), event.getPartialTick().getRealtimeDeltaTicks());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        getKeyInputEvent().invoker().onKeyInput(new InputKey(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers()));
    }

    @SubscribeEvent
    public void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        var type = event.isAttack() ? InteractionType.ATTACK : event.isUseItem() ? InteractionType.USE_ITEM : InteractionType.UNKNOWN;
        var hand = MinecraftConvertor.fromPlatformInteractionHand(event.getHand());
        if (getInteractionInputEvent().invoker().onInteractionInput(type, hand).interruptsFurtherEvaluation()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

}
