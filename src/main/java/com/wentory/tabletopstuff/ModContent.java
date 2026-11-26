package com.wentory.tabletopstuff;

import com.wentory.tabletopstuff.prototype.LobbyBlock;
import com.wentory.tabletopstuff.prototype.LobbyBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.wentory.tabletopstuff.prototype.TableSeat;
import com.wentory.tabletopstuff.prototype.TestPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
//?}

public final class ModContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, TabletopStuff.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TabletopStuff.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,TabletopStuff.MOD_ID);
    public static final Supplier<net.minecraft.world.inventory.MenuType<com.wentory.tabletopstuff.workshop.WorkshopMenu>> WORKSHOP_MENU=MENUS.register("games_workshop",
        ()->new net.minecraft.world.inventory.MenuType<>(com.wentory.tabletopstuff.workshop.WorkshopMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<Block> WORKSHOP=BLOCKS.register("games_workshop",()->new com.wentory.tabletopstuff.workshop.WorkshopBlock(workshopProperties()));
    public static final Supplier<Item> WORKSHOP_ITEM=ITEMS.register("games_workshop",()->new BlockItem(WORKSHOP.get(),new Item.Properties()));
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TabletopStuff.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TabletopStuff.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TabletopStuff.MOD_ID);
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,TabletopStuff.MOD_ID);
    public static final Supplier<net.minecraft.sounds.SoundEvent> DISCARD_COLLECT=SOUNDS.register("cards.discard_collect",()->{
        //? if >=1.21.1 {
        /*var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID,"cards.discard_collect");
        *///?} else {
        var id=new net.minecraft.resources.ResourceLocation(TabletopStuff.MOD_ID,"cards.discard_collect");
        //?}
        return net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
    });
    public static final Supplier<EntityType<TableSeat>> SEAT = ENTITIES.register("table_seat", () -> EntityType.Builder.<TableSeat>of(TableSeat::new, MobCategory.MISC)
        .sized(0.01f, 0.01f).clientTrackingRange(8).updateInterval(20).noSave().build("tabletop_stuff:table_seat"));
    public static final Supplier<EntityType<TestPlayer>> TEST_PLAYER = ENTITIES.register("test_player", () -> EntityType.Builder.<TestPlayer>of(TestPlayer::new, MobCategory.MISC)
        .sized(0.6f, 1.8f)
        //? if >=1.21.1 {
        /*.eyeHeight(1.62f)
        *///?}
        .clientTrackingRange(8).updateInterval(3).noSave().build("tabletop_stuff:test_player"));
    public static final Supplier<Block> LOBBY = BLOCKS.register("lobby", () -> new LobbyBlock(lobbyProperties()));
    public static final Supplier<Item> LOBBY_ITEM = ITEMS.register("lobby", () -> new BlockItem(LOBBY.get(), new Item.Properties()));
    public static final Supplier<CreativeModeTab> TAB = TABS.register("tabletop_stuff", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.tabletop_stuff"))
        .icon(() -> LOBBY_ITEM.get().getDefaultInstance())
        .displayItems((parameters, output) -> {output.accept(LOBBY_ITEM.get());output.accept(WORKSHOP_ITEM.get());}).build());
    public static final Supplier<BlockEntityType<LobbyBlockEntity>> LOBBY_ENTITY = BLOCK_ENTITIES.register("lobby", () -> BlockEntityType.Builder.of(LobbyBlockEntity::new, LOBBY.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        SOUNDS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(TEST_PLAYER.get(), net.minecraft.world.entity.Mob.createMobAttributes().build()));
    }

    private static BlockBehaviour.Properties workshopProperties(){
        //? if >=1.21.1 {
        /*return BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).noOcclusion();
        *///?} else {
        return BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE).noOcclusion();
        //?}
    }
    private static BlockBehaviour.Properties lobbyProperties() {
        //? if >=1.21.1 {
        /*return BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT);
        *///?} else {
        return BlockBehaviour.Properties.copy(Blocks.FLOWER_POT);
        //?}
    }
}
