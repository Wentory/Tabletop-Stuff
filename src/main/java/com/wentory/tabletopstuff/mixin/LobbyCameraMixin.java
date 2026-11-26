package com.wentory.tabletopstuff.mixin;

import com.wentory.tabletopstuff.client.LobbyCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class LobbyCameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void tabletopStuff$lobbyCamera(BlockGetter level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
        Camera camera = (Camera)(Object)this;
        var view = LobbyCamera.apply(camera.getPosition(), camera.getYRot(), camera.getXRot());
        if (view != null) {
            setPosition(view.position().x, view.position().y, view.position().z);
            setRotation(view.yaw(), view.pitch());
        }
    }
}
