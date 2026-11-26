package com.wentory.tabletopstuff.mixin;

import com.wentory.tabletopstuff.client.PrototypeClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void tabletopStuff$hideToasts(GuiGraphics graphics, CallbackInfo callback) {
        if (PrototypeClient.controlling() || net.minecraft.client.Minecraft.getInstance().screen instanceof com.wentory.tabletopstuff.client.LobbyScreen) callback.cancel();
    }
}
