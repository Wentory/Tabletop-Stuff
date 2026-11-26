package com.wentory.tabletopstuff.prototype;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.1 {
/*import net.minecraft.world.ItemInteractionResult;
import com.mojang.serialization.MapCodec;
*///?}

/** Deck-shaped lobby trigger for the visual prototype. */
public final class LobbyBlock extends BaseEntityBlock {
    public static final BooleanProperty COVERED = BooleanProperty.create("covered");
    public static final BooleanProperty DECK_PRESENT = BooleanProperty.create("deck_present");
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 2, 14.4);
    private static final VoxelShape COVERED_SHAPE = Block.box(1.6, 0, 1.6, 14.4, 3, 14.4);

    public LobbyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COVERED, false).setValue(DECK_PRESENT,true));
    }

    //? if >=1.21.1 {
    /*public static final MapCodec<LobbyBlock> CODEC = simpleCodec(LobbyBlock::new);
    @Override
    public MapCodec<LobbyBlock> codec() { return CODEC; }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(COVERED,DECK_PRESENT); }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LobbyBlockEntity(pos, state); }
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static boolean isCovering(Block block) {
        return block instanceof CarpetBlock || block == Blocks.MOSS_CARPET || block.defaultBlockState().is(BlockTags.WOOL_CARPETS);
    }

    public static double surfaceHeight(BlockState state) { return state.getValue(COVERED) ? 1.0 / 16 : 0; }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return side == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    private boolean layCovering(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BlockItem item) || !isCovering(item.getBlock())) return false;
        if (!(level.getBlockEntity(pos) instanceof LobbyBlockEntity entity)) return false;
        if (!level.isClientSide && entity.covering().getBlock() != item.getBlock()) {
            ItemStack previous = new ItemStack(entity.covering().getBlock());
            entity.setCovering(item.getBlock());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
            }
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof LobbyBlockEntity entity) {
            if (!entity.covering().isAir()) Block.popResource(level, pos, new ItemStack(entity.covering().getBlock()));
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return state.getValue(COVERED) ? COVERED_SHAPE : SHAPE; }

    private InteractionResult enter(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) Lobbies.open(serverPlayer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    //? if >=1.21.1 {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return enter(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (layCovering(level, pos, player, hand)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        enter(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?} else {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (layCovering(level, pos, player, hand)) return InteractionResult.sidedSuccess(level.isClientSide);
        return enter(level, pos, player);
    }
    //?}
}
