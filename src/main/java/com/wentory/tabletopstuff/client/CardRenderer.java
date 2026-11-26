package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wentory.tabletopstuff.prototype.VisualPose;
import com.wentory.tabletopstuff.prototype.HandLayout;
import com.wentory.tabletopstuff.prototype.HoverMotion;
import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Two-sided thin cards. Face/back UVs are independent, ready for the future atlas. */
public final class CardRenderer {
    private static final List<HandLayout.Card> SINGLE=List.of(new HandLayout.Card(0,-HandLayout.GRIP_INSET,0,0,0,0));
    public static void single(PoseStack stack,MultiBufferSource buffers,int light,int face,float opacity){render(stack,buffers,light,SINGLE,false,opacity,new int[]{face});}
    private static final Matrix4f VIEW_INVERSE = new Matrix4f();
    private static final Vector3f VIEWER = new Vector3f();
    public static void render(PoseStack stack, MultiBufferSource buffers, int light, List<HandLayout.Card> cards, boolean interactive, float opacity,int[] faces) {
        if (interactive) CardHover.update(stack, cards,faces);
        VertexConsumer vertices = buffers.getBuffer(opacity < 0.9999f ? CardAtlas.TRANSLUCENT_MATERIAL : CardAtlas.MATERIAL);
        int alpha = Math.round(Math.max(0, Math.min(1, opacity)) * 255);
        int faceColor = (alpha << 24) | 0xFFFFFF;
        int edgeColor = (alpha << 24) | 0xF6F1DF;
        float edgeU = CardAtlas.u0(CardAtlas.EDGE_SLOT);
        float edgeV = CardAtlas.v0(CardAtlas.EDGE_SLOT);
        boolean reverse = false;
        if (opacity < 0.9999f && !cards.isEmpty()) {
            VIEW_INVERSE.set(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
            if (Math.abs(VIEW_INVERSE.determinant()) > 1e-12f) {
                VIEW_INVERSE.invert().transformPosition(VIEWER.set(0, 0, 0));
                reverse = VIEWER.z < (cards.get(0).z() + cards.get(cards.size()-1).z()) / 2;
            }
        }
        for (int i = 0; i < cards.size(); i++) {
            var card = cards.get(reverse ? cards.size()-1-i : i);
            stack.pushPose();
            stack.translate(card.x(), card.y(), card.z());
            stack.mulPose(Axis.ZP.rotation(card.roll()));
            stack.translate(0, HandLayout.GRIP_INSET - (interactive ? CardHover.offset(card.index()) : 0), 0);
            float half = VisualPose.CARD_WIDTH / 2;
            float height = VisualPose.CARD_HEIGHT;
            float halfThickness = VisualPose.CARD_THICKNESS / 2;
            int slot = faces==null?CardAtlas.frontSlot(card.index()):faces[card.index()];
            if(slot<0)slot=CardAtlas.BACK_SLOT;
            texturedQuad(stack.last(), vertices, -half, -height, half, 0, halfThickness, faceColor, light, false,
                CardAtlas.u0(slot), CardAtlas.v0(slot), CardAtlas.u1(slot), CardAtlas.v1(slot));
            texturedQuad(stack.last(), vertices, -half, -height, half, 0, -halfThickness, faceColor, light, true,
                CardAtlas.u0(CardAtlas.BACK_SLOT), CardAtlas.v0(CardAtlas.BACK_SLOT), CardAtlas.u1(CardAtlas.BACK_SLOT), CardAtlas.v1(CardAtlas.BACK_SLOT));
            if (interactive && (CardHover.hovered() == card.index() || CardHover.selected() == card.index())) {
                outline(stack.last(), vertices, half, height, halfThickness, HoverMotion.outlineColor(PrototypeClient.canPlayCard(card.index())));
            }
            vertex(stack.last(), vertices, -half, -height, -halfThickness, edgeU, edgeV, edgeColor, light, -1, 0, 0);
            vertex(stack.last(), vertices, -half, -height, halfThickness, edgeU, edgeV, edgeColor, light, -1, 0, 0);
            vertex(stack.last(), vertices, -half, 0, halfThickness, edgeU, edgeV, edgeColor, light, -1, 0, 0);
            vertex(stack.last(), vertices, -half, 0, -halfThickness, edgeU, edgeV, edgeColor, light, -1, 0, 0);
            vertex(stack.last(), vertices, half, -height, halfThickness, edgeU, edgeV, edgeColor, light, 1, 0, 0);
            vertex(stack.last(), vertices, half, -height, -halfThickness, edgeU, edgeV, edgeColor, light, 1, 0, 0);
            vertex(stack.last(), vertices, half, 0, -halfThickness, edgeU, edgeV, edgeColor, light, 1, 0, 0);
            vertex(stack.last(), vertices, half, 0, halfThickness, edgeU, edgeV, edgeColor, light, 1, 0, 0);
            stack.popPose();
        }
    }

    private static void outline(PoseStack.Pose pose, VertexConsumer vertices, float half, float height, float depth, int color) {
        float border = VisualPose.CARD_WIDTH / 64 * 2f;
        float z = depth + VisualPose.CARD_THICKNESS * 0.05f;
        float u = CardAtlas.u0(CardAtlas.EDGE_SLOT), v = CardAtlas.v0(CardAtlas.EDGE_SLOT);
        int light = 0xF000F0;
        texturedQuad(pose, vertices, -half-border, -height-border, half+border, -height, z, color, light, false, u,v,u,v);
        texturedQuad(pose, vertices, -half-border, 0, half+border, border, z, color, light, false, u,v,u,v);
        texturedQuad(pose, vertices, -half-border, -height, -half, 0, z, color, light, false, u,v,u,v);
        texturedQuad(pose, vertices, half, -height, half+border, 0, z, color, light, false, u,v,u,v);
    }

    private static void texturedQuad(PoseStack.Pose pose, VertexConsumer vertices, float x0, float y0, float x1, float y1, float z, int color, int light, boolean back,
            float u0, float v0, float u1, float v1) {
        float normal = back ? -1 : 1;
        // The owner's front view reverses the player model X axis; keep artwork readable.
        float leftU = back ? u0 : u1;
        float rightU = back ? u1 : u0;
        vertex(pose, vertices, x0, y0, z, leftU, v0, color, light, 0, 0, normal);
        if (back) vertex(pose, vertices, x0, y1, z, leftU, v1, color, light, 0, 0, normal);
        else vertex(pose, vertices, x1, y0, z, rightU, v0, color, light, 0, 0, normal);
        vertex(pose, vertices, x1, y1, z, rightU, v1, color, light, 0, 0, normal);
        if (back) vertex(pose, vertices, x1, y0, z, rightU, v0, color, light, 0, 0, normal);
        else vertex(pose, vertices, x0, y1, z, leftU, v1, color, light, 0, 0, normal);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float z, float u, float v, int color, int light, float nx, float ny, float nz) {
        //? if >=1.21.1 {
        /*vertices.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        *///?} else {
        vertices.vertex(pose.pose(), x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
            .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        //?}
    }
}
