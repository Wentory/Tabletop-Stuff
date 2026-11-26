package com.wentory.tabletopstuff.mixin;

import com.wentory.tabletopstuff.client.PlayerPoseRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void tabletopStuff$pose(LivingEntity player, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, CallbackInfo callback) {
        PlayerPoseRenderer.afterSetup((PlayerModel<?>) (Object) this, player, age);
    }
}
