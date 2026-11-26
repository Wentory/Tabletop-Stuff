package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.tabletopstuff.prototype.TestPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

public final class TestPlayerRenderer extends HumanoidMobRenderer<TestPlayer, PlayerModel<TestPlayer>> {
    private final PlayerModel<TestPlayer> slimModel;
    private final PlayerModel<TestPlayer> wideModel;

    public TestPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        slimModel = getModel();
        wideModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        addLayer(new RenderLayer<TestPlayer, PlayerModel<TestPlayer>>(this) {
            @Override public void render(PoseStack stack, MultiBufferSource buffers, int light, TestPlayer player,
                float limbSwing, float limbAmount, float partialTick, float age, float headYaw, float headPitch) {
                PlayerPoseRenderer.cards(getParentModel(), stack, buffers, light, player,age);
            }
        });
    }
    PlayerModel<TestPlayer> modelFor(TestPlayer player) {
        var skin = player.botSkin();
        return skin == null || skin.slim() ? slimModel : wideModel;
    }

    @Override public ResourceLocation getTextureLocation(TestPlayer player) {
        var skin = player.botSkin();
        String namespace = skin == null ? "tabletop_stuff" : "minecraft";
        String path = skin == null ? "textures/dev/amonchik.png" :
            "textures/entity/player/" + (skin.slim() ? "slim/" : "wide/")
                + skin.name().toLowerCase(Locale.ROOT) + ".png";
        //? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }
    @Override protected void scale(TestPlayer player, PoseStack stack, float partialTick) { stack.scale(0.9375f, 0.9375f, 0.9375f); }
    @Override public void render(TestPlayer player, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        float body=player.yBodyRot,previousBody=player.yBodyRotO;
        var session=PrototypeClient.session(player);
        if(session!=null){player.yBodyRot=session.bodyYaw();player.yBodyRotO=session.bodyYaw();}
        model = modelFor(player);
        try { super.render(player, yaw, partialTick, stack, buffers, light); }
        finally { PlayerPoseRenderer.restore(getModel());player.yBodyRot=body;player.yBodyRotO=previousBody; }
    }
}
