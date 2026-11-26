package com.wentory.tabletopstuff.workshop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
*///?}

public final class WorkshopBlock extends HorizontalDirectionalBlock {
    //? if >=1.21.1 {
    /*private static final MapCodec<WorkshopBlock> CODEC=simpleCodec(WorkshopBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    *///?}
    public WorkshopBlock(Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    private InteractionResult open(Level level,BlockPos pos,Player player){
        if(!level.isClientSide)player.openMenu(new SimpleMenuProvider(
            (id,inventory,owner)->new WorkshopMenu(id,inventory,ContainerLevelAccess.create(level,pos)),
            Component.translatable("block.tabletop_stuff.games_workshop")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //? if >=1.21.1 {
    /*@Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return open(level,pos,player);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?} else {
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return open(level,pos,player);}
    //?}
}
