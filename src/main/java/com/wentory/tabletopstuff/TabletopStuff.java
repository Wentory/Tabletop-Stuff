package com.wentory.tabletopstuff;

//? if >=1.21.1 {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
*///?} else {
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?}

@Mod(TabletopStuff.MOD_ID)
public final class TabletopStuff {
    public static final String MOD_ID = "tabletop_stuff";
    public static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("com.wentory.tabletopstuff");

    //? if >=1.21.1 {
    /*public TabletopStuff(IEventBus bus,net.neoforged.fml.ModContainer container) {
    *///?} else {
    public TabletopStuff() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
    //?}
        if (Boolean.getBoolean("tabletop_stuff.debug")) {
            org.apache.logging.log4j.core.config.Configurator.setLevel(LOGGER.getName(), org.apache.logging.log4j.Level.DEBUG);
            LOGGER.info("Tabletop Stuff debug logging enabled; detailed messages are in logs/debug.log");
        }
        ModContent.register(bus);
        //? if >=1.21.1 {
        /*GameConfig.register(container);
        PlayerStyleConfig.register(container);
        *///?} else {
        GameConfig.register();
        PlayerStyleConfig.register();
        //?}
        com.wentory.tabletopstuff.prototype.CardSounds.register(bus);
        com.wentory.tabletopstuff.network.PrototypeNetwork.register(bus);
        com.wentory.tabletopstuff.prototype.PrototypeSessions.register();
        com.wentory.tabletopstuff.prototype.TableProtection.register();
    }
}
