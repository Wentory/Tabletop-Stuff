package com.wentory.tabletopstuff.prototype;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Temporary player-shaped test participant, using the same placement and mount as a player. */
public final class TestPlayer extends PathfinderMob {
    public enum BotSkin {
        STEVE(false), ALEX(true), ARI(true), EFE(true), KAI(true),
        MAKENA(false), NOOR(false), SUNNY(false), ZURI(false);

        private static final BotSkin[] ALL = values();
        private final boolean slim;

        BotSkin(boolean slim) { this.slim = slim; }
        public boolean slim() { return slim; }
    }

    private static final EntityDataAccessor<Integer> BOT_SKIN =
        SynchedEntityData.defineId(TestPlayer.class, EntityDataSerializers.INT);

    private UUID owner;
    private BlockPos deck;
    private Vec3 anchor;
    private float facing;

    public TestPlayer(EntityType<? extends TestPlayer> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
        setPersistenceRequired();
    }

    //? if >=1.21.1 {
    /*@Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOT_SKIN, -1);
    }
    *///?} else {
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BOT_SKIN, -1);
    }
    //?}

    @Override public boolean canBeSeenAsEnemy() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public void kill() {}

    public void setBotSkin(BotSkin skin) { entityData.set(BOT_SKIN, skin.ordinal()); }

    public BotSkin botSkin() {
        int index = entityData.get(BOT_SKIN);
        return index >= 0 && index < BotSkin.ALL.length ? BotSkin.ALL[index] : null;
    }

    public void place(UUID owner, BlockPos deck, LobbyPlaces.Place place) {
        this.owner = owner; this.deck = deck.immutable(); anchor = place.feet(); facing = place.yaw();
        moveTo(anchor.x, anchor.y, anchor.z, facing, 22);
        yBodyRot = facing; yBodyRotO = facing;
        setYHeadRot(facing); yHeadRotO = facing;
    }
    //? if <1.21.1 {
    @Override protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose,net.minecraft.world.entity.EntityDimensions dimensions){return 1.62f;}
    //?}

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !PreparationGames.active(owner)
            || deck == null || !level.getBlockState(deck).is(com.wentory.tabletopstuff.ModContent.LOBBY.get())) {
            removeFromTable(); return;
        }
        setDeltaMovement(Vec3.ZERO);
        setPos(anchor);
        fallDistance = 0;
        setYRot(facing); setYHeadRot(facing); yBodyRot = facing;
        setXRot(22);
    }

    public void removeFromTable() {
        if (getVehicle() instanceof TableSeat seat) { stopRiding(); seat.discard(); }
        discard();
    }
}
