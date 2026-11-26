package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import java.util.IdentityHashMap;
import java.util.Map;

/** A private overlay material for world-positioned UI; never tests or writes world depth. */
public final class LobbyRenderBuffers {
    private static final Map<RenderType,RenderType> TYPES = new IdentityHashMap<>();
    public static final MultiBufferSource UI = source -> Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(
        TYPES.computeIfAbsent(source, LobbyRenderBuffers::overlay));

    private static RenderType overlay(RenderType source) {
        return new RenderType("tabletop_lobby_overlay", source.format(), source.mode(), source.bufferSize(), false, false,
            () -> {
                // Shader loaders may replace the original text material's state.
                // Apply our overlay policy after its actual shader/texture setup.
                source.setupRenderState();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                source.clearRenderState();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            }) {};
    }
}
