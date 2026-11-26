package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wentory.tabletopstuff.prototype.VisualPose;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import com.wentory.tabletopstuff.prototype.PreparationUpdate;

public final class PlayerPoseRenderer {
    private record Palm(float x, float y, float z) {}
    private static final Map<ModelPart, Palm> PALMS = new WeakHashMap<>();

    private static Palm palm(ModelPart arm) {
        return PALMS.computeIfAbsent(arm, part -> {
            Palm[] result = new Palm[1];
            part.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (result[0] == null) result[0] = new Palm((cube.minX + cube.maxX) / 2,
                    cube.maxY, (cube.minZ + cube.maxZ) / 2);
            });
            return result[0];
        });
    }

    private record Saved(PartPose left, PartPose right, PartPose leftSleeve, PartPose rightSleeve,
        PartPose head, PartPose hat, PartPose body, PartPose jacket) {
        Saved(PlayerModel<?> model) { this(model.leftArm.storePose(), model.rightArm.storePose(), model.leftSleeve.storePose(), model.rightSleeve.storePose(),
            model.head.storePose(),model.hat.storePose(),model.body.storePose(),model.jacket.storePose()); }
        void restore(PlayerModel<?> model) {
            model.leftArm.loadPose(left); model.rightArm.loadPose(right);
            model.leftSleeve.loadPose(leftSleeve); model.rightSleeve.loadPose(rightSleeve);
            model.head.loadPose(head);model.hat.loadPose(hat);model.body.loadPose(body);model.jacket.loadPose(jacket);
        }
    }
    private static final Map<PlayerModel<?>, Saved> SAVED = new IdentityHashMap<>();

    public static void afterSetup(PlayerModel<?> model, LivingEntity entity, float age) {
        if (PrototypeClient.session(entity) == null && !(entity instanceof com.wentory.tabletopstuff.prototype.TestPlayer)) return;
        SAVED.put(model, new Saved(model));
        pose(model, age, entity);
    }

    public static void restore(PlayerModel<?> model) {
        Saved saved = SAVED.remove(model);
        if (saved != null) saved.restore(model);
    }

    private static void arm(ModelPart part, VisualPose.Arm shown, VisualPose.Arm hidden, float amount) {
        part.loadPose(part.getInitialPose());
        part.x += Mth.lerp(amount, shown.offsetX(), hidden.offsetX());
        part.y += Mth.lerp(amount, shown.offsetY(), hidden.offsetY());
        part.z += Mth.lerp(amount, shown.offsetZ(), hidden.offsetZ());
        part.xRot = Mth.lerp(amount, shown.pitch(), hidden.pitch());
        part.yRot = Mth.lerp(amount, shown.yaw(), hidden.yaw());
        part.zRot = Mth.lerp(amount, shown.roll(), hidden.roll());
    }

    private static void pose(PlayerModel<?> model, float age, LivingEntity entity) {
        if(PreparationClient.dealerHolding(entity)||PreparationRenderer.resting(entity)){
            model.leftArm.loadPose(model.leftArm.getInitialPose());model.rightArm.loadPose(model.rightArm.getInitialPose());
            PreparationRenderer.pose(model,entity);breathe(model,age,entity);model.leftSleeve.copyFrom(model.leftArm);model.rightSleeve.copyFrom(model.rightArm);return;
        }
        var preparation=PreparationClient.of(entity);
        var actor=preparation==null?null:preparation.actor(entity.getUUID());
        var revealPose=actor==null?null:ShowCardsMotion.sample(preparation,actor);
        float hidden = PrototypeClient.hiddenAmount(entity);
        if(revealPose!=null)hidden*=1-revealPose.shown();
        arm(model.leftArm, VisualPose.LEFT, VisualPose.HIDDEN_LEFT, hidden);
        arm(model.rightArm, VisualPose.RIGHT, VisualPose.HIDDEN_RIGHT, hidden);
        if(actor!=null){
            float drop=16*PreparationMotion.floorHandOffset(preparation,actor);
            model.leftArm.y+=drop;
            model.rightArm.y+=drop;
            if(revealPose!=null)revealPose.apply(model.leftArm);
        }
        PreparationRenderer.pose(model,entity);
        breathe(model,age,entity);
        model.leftSleeve.copyFrom(model.leftArm);model.rightSleeve.copyFrom(model.rightArm);
    }

    static float breathingOffset(float age,LivingEntity entity){
        float focus=entity==Minecraft.getInstance().player?1-.8f*CardHover.focus():1;
        return (float)Math.sin(age/20.0*(Math.PI*2/4.8))*.06f*focus;
    }
    private static void breathe(PlayerModel<?> model,float age,LivingEntity entity){
        var prep=PreparationClient.of(entity);
        boolean idle=PreparationRenderer.resting(entity)&&(prep==null||prep.take(entity.getUUID())>=1);
        if(idle){
            float roll=(float)Math.cos(age*.09f)*.05f+.05f,pitch=(float)Math.sin(age*.067f)*.05f;
            model.rightArm.zRot+=roll;model.leftArm.zRot-=roll;model.rightArm.xRot+=pitch;model.leftArm.xRot-=pitch;
        }else{float bob=breathingOffset(age,entity);model.leftArm.y+=bob;model.rightArm.y+=bob;}
    }

    /** Draw the actual player's arm parts in world space, not a second full player/entity. */
    public static void firstPerson(PoseStack stack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || !PrototypeClient.controlling()) return;
        LivingEntity selected=Minecraft.getInstance().player;
        if(selected==null)return;
        net.minecraft.client.renderer.entity.EntityRenderer<?> selectedRenderer=mc.getEntityRenderDispatcher().getRenderer(selected);
        PlayerModel<?> model;
        ResourceLocation skin;
        if(selected instanceof AbstractClientPlayer player&&selectedRenderer instanceof PlayerRenderer renderer){
            model=renderer.getModel();
            //? if >=1.21.1 {
            /*skin=player.getSkin().texture();
            *///?} else {
            skin=player.getSkinTextureLocation();
            //?}
        }else return;
        Saved saved = new Saved(model);
        var session = PrototypeClient.session(selected);
        if (session == null) return;
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(selected, partialTick);
        stack.pushPose();
        try {
            stack.translate(Mth.lerp(partialTick, selected.xo, selected.getX()) - camera.x,
                Mth.lerp(partialTick, selected.yo, selected.getY()) - camera.y,
                Mth.lerp(partialTick, selected.zo, selected.getZ()) - camera.z);
            stack.mulPose(Axis.YP.rotationDegrees(180 - session.bodyYaw()));
            stack.scale(-0.9375f, -0.9375f, 0.9375f);
            stack.translate(0, -1.501, 0);
            pose(model, selected.tickCount + partialTick, selected);
            var armBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
            drawPart(model.leftArm, stack, armBuffer, light);
            drawPart(model.rightArm, stack, armBuffer, light);
            if (!(selected instanceof AbstractClientPlayer p)||p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE)) drawPart(model.leftSleeve, stack, armBuffer, light);
            if (!(selected instanceof AbstractClientPlayer p)||p.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE)) drawPart(model.rightSleeve, stack, armBuffer, light);
            cards(model, stack, buffers, light, selected,selected.tickCount+partialTick);
        } finally {
            saved.restore(model);
            stack.popPose();
        }
    }

    private static void drawPart(ModelPart part, PoseStack stack, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light) {
        boolean visible = part.visible;
        try {
            part.visible = true;
            //? if >=1.21.1 {
            /*part.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, -1);
            *///?} else {
            part.render(stack, buffer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            //?}
        } finally { part.visible = visible; }
    }

    public static void cards(PlayerModel<?> model, PoseStack stack, MultiBufferSource buffers, int light, LivingEntity player,float age) {
        Palm actual=palm(model.leftArm);
        if(actual!=null)PreparationRenderer.held(model,stack,buffers,light,player,actual.x(),actual.y(),actual.z(),breathingOffset(age,player));
        var preparation=PreparationClient.of(player);
        if(actual!=null&&preparation!=null&&preparation.play(player.getUUID())!=null){PreparationRenderer.playHand(model,stack,buffers,light,player,actual.x(),actual.y(),actual.z(),preparation);return;}
        if(PreparationClient.dealerHolding(player)||preparation!=null&&preparation.take(player.getUUID())<1||PrototypeClient.hand(player).isEmpty())return;
        stack.pushPose();
        try {
            ModelPart arm = model.leftArm;
            Palm palm = palm(arm);
            if (palm == null) return;
            var actor=preparation==null?null:preparation.actor(player.getUUID());
            var revealPose=actor==null?null:ShowCardsMotion.sample(preparation,actor);
            boolean showing=revealPose!=null;
            arm.translateAndRotate(stack);
            var grip = VisualPose.GRIP;
            var hiddenGrip = VisualPose.HIDDEN_GRIP;
            float hidden = PrototypeClient.hiddenAmount(player);
            if(showing)hidden*=1-revealPose.shown();
            stack.translate((palm.x() + Mth.lerp(hidden, grip.x(), hiddenGrip.x())) / 16,
                (palm.y() + Mth.lerp(hidden, grip.y(), hiddenGrip.y())) / 16,
                (palm.z() + Mth.lerp(hidden, grip.z(), hiddenGrip.z())) / 16);
            float roll=Mth.lerp(hidden,grip.roll(),hiddenGrip.roll());
            float yaw=Mth.lerp(hidden,grip.yaw(),hiddenGrip.yaw());
            float pitch=Mth.lerp(hidden,grip.pitch(),hiddenGrip.pitch());
            if(showing){
                roll+=ShowCardsMotion.radians(revealPose.gripRotation().z);
                yaw-=ShowCardsMotion.radians(revealPose.gripRotation().y);
                pitch-=ShowCardsMotion.radians(revealPose.gripRotation().x);
            }
            stack.mulPose(new Quaternionf().rotationZYX(roll,yaw,pitch));
            var hand=PrototypeClient.hand(player);
            int[] faces=PreparationClient.faces(player);
            if(showing){
                    faces=preparation.update.rules().revealed();
                hand=com.wentory.tabletopstuff.prototype.HandLayout.arrange(faces.length);
            }
            stack.scale(1f / 16, 1f / 16, 1f / 16);
            Minecraft mc = Minecraft.getInstance();
            boolean interactive = !showing && player == Minecraft.getInstance().player && mc.options.getCameraType().isFirstPerson() && PrototypeClient.canSelectCards();
            float opacity = player == Minecraft.getInstance().player ? Mth.lerp(hidden, 1f, 0.2f) : 1f;
            CardRenderer.render(stack,buffers,light,hand,interactive,opacity,faces);
        } finally { stack.popPose(); }
    }
}
