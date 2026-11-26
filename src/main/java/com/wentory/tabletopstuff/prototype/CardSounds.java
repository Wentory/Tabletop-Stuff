package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.TabletopStuff;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?}

/** Separate events for physical contacts; OGG channel layouts are preserved. */
public final class CardSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,TabletopStuff.MOD_ID);
    public static final Supplier<SoundEvent> SHUFFLE_TAKE=sound("shuffle_take"),SHUFFLE_INSERT=sound("shuffle_insert"),
        CARD_TAKE=sound("card_take"),CARD_LAND=sound("card_land"),HAND_PICKUP=sound("hand_pickup"),
        FAN_TAKE=sound("fan_take"),DECK_PLACE=sound("deck_place"),CARD_TO_HAND=sound("card_to_hand");
    private static Supplier<SoundEvent> sound(String name){return SOUNDS.register("cards."+name,()->{
        //? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID,"cards."+name);
        *///?} else {
        var id=new net.minecraft.resources.ResourceLocation(TabletopStuff.MOD_ID,"cards."+name);
        //?}
        return SoundEvent.createVariableRangeEvent(id);
    });}
    public static void register(IEventBus bus){SOUNDS.register(bus);}
}
