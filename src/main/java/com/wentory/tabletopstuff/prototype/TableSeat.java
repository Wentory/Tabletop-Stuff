package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
//? if <1.21.1 {
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
//?}

/** Invisible mount, only alive while its participant is sitting at a table. */
public final class TableSeat extends Entity {
    // Vanilla player hips are at model Y=12; the renderer origin is at 1.501,
    // with the standard 0.9375 player scale. The hips, not the standing feet,
    // must touch the supporting surface in the passenger pose.
    public static final double HIP_HEIGHT=(1.501-12.0/16)*0.9375;
    public TableSeat(EntityType<? extends TableSeat> type, Level level) { super(type, level); noPhysics = true; }
    public TableSeat(Level level) { this(ModContent.SEAT.get(), level); }
    //? if >=1.21.1 {
    /*@Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    *///?} else {
    @Override protected void defineSynchedData() {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    //?}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (hasPassenger(passenger)) move.accept(passenger, getX(), getY(), getZ());
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !PrototypeSessions.locked(getFirstPassenger()))) discard();
    }
}
