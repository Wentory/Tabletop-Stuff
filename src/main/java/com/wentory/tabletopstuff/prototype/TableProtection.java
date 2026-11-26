package com.wentory.tabletopstuff.prototype;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
*///?} else {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
//?}

public final class TableProtection {
    public static boolean protectedAt(net.minecraft.world.level.Level level,BlockPos pos){
        return level instanceof ServerLevel server&&PreparationGames.protectedBlock(server,pos);
    }
    public static void register(){
        //? if >=1.21.1 {
        /*var bus=NeoForge.EVENT_BUS;
        *///?} else {
        var bus=MinecraftForge.EVENT_BUS;
        //?}
        bus.addListener((BlockEvent.BreakEvent event)->{
            if(event.getLevel() instanceof ServerLevel level&&protectedAt(level,event.getPos()))event.setCanceled(true);
        });
        bus.addListener((ExplosionEvent.Detonate event)->event.getAffectedBlocks().removeIf(pos->protectedAt(event.getLevel(),pos)));
        bus.addListener((PistonEvent.Pre event)->{
            if(!(event.getLevel() instanceof ServerLevel level))return;
            if(protectedAt(level,event.getPos())||protectedAt(level,event.getFaceOffsetPos())){event.setCanceled(true);return;}
            var resolver=event.getStructureHelper();if(resolver==null)return;
            resolver.resolve();
            var movement=event.getPistonMoveType()==PistonEvent.PistonMoveType.EXTEND?event.getDirection():event.getDirection().getOpposite();
            if(resolver.getToDestroy().stream().anyMatch(pos->protectedAt(level,pos))||resolver.getToPush().stream().anyMatch(pos->protectedAt(level,pos)||protectedAt(level,pos.relative(movement))))event.setCanceled(true);
        });
    }
    private TableProtection(){}
}
