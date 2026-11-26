package com.wentory.tabletopstuff.mixin;

import com.wentory.tabletopstuff.prototype.PrototypeSessions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TableParticipantMixin {
    /** Keep vanilla entity collision: only discard the participant's own impulse. */
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void tabletopStuff$anchored(double x, double y, double z, CallbackInfo ci) {
        if (PrototypeSessions.locked((Entity)(Object)this)) ci.cancel();
    }
}
