package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
*///?}

/** Persistent covering; no ticker or per-frame network traffic. */
public final class LobbyBlockEntity extends BlockEntity {
    private BlockState covering = Blocks.AIR.defaultBlockState();

    public LobbyBlockEntity(BlockPos pos, BlockState state) { super(ModContent.LOBBY_ENTITY.get(), pos, state); }
    public BlockState covering() { return covering; }

    public void setCovering(Block block) {
        covering = block.defaultBlockState();
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.setBlock(worldPosition, state.setValue(LobbyBlock.COVERED, !covering.isAir()), 3);
            level.sendBlockUpdated(worldPosition, state, getBlockState(), 3);
        }
    }

    private void writeCovering(CompoundTag tag) {
        if (!covering.isAir()) tag.putString("Covering", BuiltInRegistries.BLOCK.getKey(covering.getBlock()).toString());
    }

    private void readCovering(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Covering"));
        Block block = id == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(id);
        covering = LobbyBlock.isCovering(block) ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    //? if >=1.21.1 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCovering(tag);
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCovering(tag);
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCovering(tag);
        return tag;
    }
    *///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeCovering(tag);
    }
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readCovering(tag);
    }
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeCovering(tag);
        return tag;
    }
    //?}

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
