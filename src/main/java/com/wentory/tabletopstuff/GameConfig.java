package com.wentory.tabletopstuff;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
*///?} else {
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
//?}

public final class GameConfig {
    //? if >=1.21.1 {
    /*private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    private static final ModConfigSpec.IntValue AWAY=BUILDER.comment("Minutes before an away player leaves the game. 0 disables the timer.").defineInRange("awayTimeoutMinutes",5,0,1440);
    public static final ModConfigSpec SPEC=BUILDER.build();
    public static void register(ModContainer container){container.registerConfig(ModConfig.Type.SERVER,SPEC);}
    *///?} else {
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.IntValue AWAY=BUILDER.comment("Minutes before an away player leaves the game. 0 disables the timer.").defineInRange("awayTimeoutMinutes",5,0,1440);
    public static final ForgeConfigSpec SPEC=BUILDER.build();
    public static void register(){net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,SPEC);}
    //?}
    public static int awayMinutes(){return AWAY.get();}
}
