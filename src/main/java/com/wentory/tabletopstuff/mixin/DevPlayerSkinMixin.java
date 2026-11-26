package com.wentory.tabletopstuff.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.1 {
/*import net.minecraft.client.resources.PlayerSkin;
*///?}

/** Local development profile only; never changes another participant's skin. */
@Mixin(AbstractClientPlayer.class)
public abstract class DevPlayerSkinMixin {
    @Unique private static final boolean tabletopStuff$enabled = Boolean.getBoolean("tabletop_stuff.devSkin");
    //? if >=1.21.1 {
    /*@Unique private static final ResourceLocation tabletopStuff$texture = ResourceLocation.fromNamespaceAndPath("tabletop_stuff", "textures/dev/wentory.png");
    @Unique private PlayerSkin tabletopStuff$original;
    @Unique private PlayerSkin tabletopStuff$skin;
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void tabletopStuff$skin(CallbackInfoReturnable<PlayerSkin> ci) {
        if (!tabletopStuff$local()) return;
        PlayerSkin original = ci.getReturnValue();
        if (tabletopStuff$original != original) {
            tabletopStuff$original = original;
            tabletopStuff$skin = new PlayerSkin(tabletopStuff$texture, null, original.capeTexture(), original.elytraTexture(), PlayerSkin.Model.WIDE, false);
        }
        ci.setReturnValue(tabletopStuff$skin);
    }
    *///?} else {
    @Unique private static final ResourceLocation tabletopStuff$texture = new ResourceLocation("tabletop_stuff", "textures/dev/wentory.png");
    @Inject(method = "getSkinTextureLocation", at = @At("RETURN"), cancellable = true)
    private void tabletopStuff$skin(CallbackInfoReturnable<ResourceLocation> ci) {
        if (tabletopStuff$local()) ci.setReturnValue(tabletopStuff$texture);
    }
    @Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
    private void tabletopStuff$wide(CallbackInfoReturnable<String> ci) {
        if (tabletopStuff$local()) ci.setReturnValue("default");
    }
    //?}
    @Unique private boolean tabletopStuff$local() {
        return tabletopStuff$enabled && (Object)this == Minecraft.getInstance().player;
    }
}
