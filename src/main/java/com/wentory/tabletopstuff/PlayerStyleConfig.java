package com.wentory.tabletopstuff;

//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
*///?} else {
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
//?}

/** Client config is shared across worlds and servers in this Minecraft installation. */
public final class PlayerStyleConfig {
    //? if >=1.21.1 {
    /*private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<String> PLAY=BUILDER.comment("Gameplay style. Only default is available.").define("playStyle","default",v->"default".equals(v));
    private static final ModConfigSpec.ConfigValue<String> DEAL=BUILDER.comment("Dealing and shuffling style. Only default is available.").define("dealStyle","default",v->"default".equals(v));
    private static final ModConfigSpec.IntValue LOBBY_CAPACITY=BUILDER.defineInRange("lobby.capacity",0,0,8);
    private static final ModConfigSpec.BooleanValue LOBBY_PRIVATE=BUILDER.define("lobby.inviteOnly",false);
    private static final ModConfigSpec.IntValue LOBBY_RULES=BUILDER.defineInRange("lobby.houseRules",0,0,63);
    private static final ModConfigSpec.IntValue FIRST_DEALER=BUILDER.defineInRange("lobby.firstDealer",0,0,1);
    private static final ModConfigSpec.IntValue NEXT_DEALER=BUILDER.defineInRange("lobby.nextDealer",0,0,2);
    public static final ModConfigSpec SPEC=BUILDER.build();
    public static void register(ModContainer container){container.registerConfig(ModConfig.Type.CLIENT,SPEC);}
    *///?} else {
    private static final ForgeConfigSpec.Builder BUILDER=new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.ConfigValue<String> PLAY=BUILDER.comment("Gameplay style. Only default is available.").define("playStyle","default",v->"default".equals(v));
    private static final ForgeConfigSpec.ConfigValue<String> DEAL=BUILDER.comment("Dealing and shuffling style. Only default is available.").define("dealStyle","default",v->"default".equals(v));
    private static final ForgeConfigSpec.IntValue LOBBY_CAPACITY=BUILDER.defineInRange("lobby.capacity",0,0,8);
    private static final ForgeConfigSpec.BooleanValue LOBBY_PRIVATE=BUILDER.define("lobby.inviteOnly",false);
    private static final ForgeConfigSpec.IntValue LOBBY_RULES=BUILDER.defineInRange("lobby.houseRules",0,0,63);
    private static final ForgeConfigSpec.IntValue FIRST_DEALER=BUILDER.defineInRange("lobby.firstDealer",0,0,1);
    private static final ForgeConfigSpec.IntValue NEXT_DEALER=BUILDER.defineInRange("lobby.nextDealer",0,0,2);
    public static final ForgeConfigSpec SPEC=BUILDER.build();
    public static void register(){net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,SPEC);}
    //?}
    public static String playStyle(){return PLAY.get();}
    public static String dealStyle(){return DEAL.get();}
    public static int lobbyCapacity(){return LOBBY_CAPACITY.get();}
    public static boolean lobbyPrivate(){return LOBBY_PRIVATE.get();}
    public static int lobbyRules(){return LOBBY_RULES.get()&31;}
    public static int firstDealer(){return FIRST_DEALER.get();}
    public static int nextDealer(){return NEXT_DEALER.get();}

    public static void saveLobbyCapacity(int value){LOBBY_CAPACITY.set(value);LOBBY_CAPACITY.save();}
    public static void saveLobbyPrivate(boolean value){LOBBY_PRIVATE.set(value);LOBBY_PRIVATE.save();}
    public static void saveLobbyRules(int value){LOBBY_RULES.set(value&31);LOBBY_RULES.save();}
    public static void saveFirstDealer(int value){FIRST_DEALER.set(value);FIRST_DEALER.save();}
    public static void saveNextDealer(int value){NEXT_DEALER.set(value);NEXT_DEALER.save();}
}
