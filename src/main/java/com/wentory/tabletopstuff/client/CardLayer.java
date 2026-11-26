package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public final class CardLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public CardLayer(PlayerRenderer renderer) { super(renderer); }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
        if (PrototypeClient.session(player) != null && !player.isInvisible()) PlayerPoseRenderer.cards(getParentModel(), stack, buffers, light, player,age);
    }
}
