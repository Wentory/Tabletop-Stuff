package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.TabletopStuff;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Private, non-positional feedback for the lobby interface. */
final class LobbySounds {
    static void custom(String name){
        //? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID,"lobby."+name);
        *///?} else {
        var id=new net.minecraft.resources.ResourceLocation(TabletopStuff.MOD_ID,"lobby."+name);
        //?}
        play(SoundEvent.createVariableRangeEvent(id),1f);
    }
    static void button(){play(SoundEvents.STONE_BUTTON_CLICK_ON,.4f);}
    static void toggle(){play(SoundEvents.LEVER_CLICK,.4f);}
    static void window(boolean open){custom(open?"in":"out");}
    private static void play(SoundEvent sound,float volume){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,1f,volume));
    }
}
