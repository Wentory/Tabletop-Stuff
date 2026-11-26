package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.tabletopstuff.prototype.LobbyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Uses the installed carpet's own model and texture, including resource pack replacements. */
public final class LobbyRenderer implements BlockEntityRenderer<LobbyBlockEntity> {
    public LobbyRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(LobbyBlockEntity entity, float partialTick, PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
        if (!entity.covering().isAir()) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(entity.covering(), stack, buffers, light, overlay);
        }
    }
}
