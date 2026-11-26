package com.wentory.tabletopstuff.mixin;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Button.class)
public interface ButtonAccessor {
    @Mutable
    @Accessor("onPress")
    void tabletopStuff$setOnPress(Button.OnPress onPress);
}
