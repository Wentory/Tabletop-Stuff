package com.wentory.tabletopstuff.mixin;

import com.wentory.tabletopstuff.prototype.TableProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent environmental removal before drops; retain the deck's own state updates. */
@Mixin(Level.class)
public abstract class TableProtectionMixin {
    @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
    private void tabletopStuff$protectDestruction(BlockPos pos,boolean drops,Entity source,int depth,CallbackInfoReturnable<Boolean> ci){
        if(TableProtection.protectedAt((Level)(Object)this,pos))ci.setReturnValue(false);
    }
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void tabletopStuff$protectReplacement(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> ci){
        Level level=(Level)(Object)this;
        if(TableProtection.protectedAt(level,pos)&&!level.getBlockState(pos).is(state.getBlock()))ci.setReturnValue(false);
    }
}
