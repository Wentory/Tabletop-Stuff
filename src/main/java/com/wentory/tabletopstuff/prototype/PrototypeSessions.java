package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.ModContent;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?} else {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
//?}

/** Ephemeral server-owned visual sessions, not NULL lobbies or saved games. */
public final class PrototypeSessions {
    private record Session(BlockPos block, Vec3 origin, float yaw, int cardCount, long lastAdjustment, boolean hidden) {}
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    public static java.util.function.Predicate<UUID> clientLocked = id -> false;
    public static boolean locked(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof TestPlayer) return true;
        return entity != null && (entity.level().isClientSide ? clientLocked.test(entity.getUUID()) : ACTIVE.containsKey(entity.getUUID()));
    }

    public static void register() {
        //? if >=1.21.1 {
        /*var bus = NeoForge.EVENT_BUS;
        bus.addListener((PlayerTickEvent.Post event) -> tick(event.getEntity()));
        *///?} else {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(event.player);
        });
        //?}
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) { Lobbies.leave(player); leave(player); }
        });
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) { Lobbies.leave(player); if(!PreparationGames.away(player.getUUID()))leave(player); }
        });
        bus.addListener((PlayerEvent.StartTracking event) -> {
            if(event.getEntity() instanceof ServerPlayer viewer)PreparationGames.tracked(viewer,event.getTarget());
            if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
                Session session = ACTIVE.get(target.getUUID());
                // Also clear stale state when a previously active player returns into view.
                PrototypeNetwork.send(viewer, session != null ? update(target, session, true)
                    : new SessionUpdate(target.getUUID(), false, BlockPos.ZERO, 0, 0));
            }
        });
        bus.addListener((ServerStoppedEvent event) -> { PreparationGames.clear(); TestPlayers.clear(); ACTIVE.clear(); Lobbies.clear(); });
    }

    public static void enter(ServerPlayer player, BlockPos pos) {
        if (!player.isAlive() || player.isSpectator() || !player.level().getBlockState(pos).is(ModContent.LOBBY.get())) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 36) return;
        Vec3 direction = Vec3.atCenterOf(pos).subtract(player.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        Session session = new Session(pos.immutable(), player.position(), yaw, 3, Long.MIN_VALUE, false);
        ACTIVE.put(player.getUUID(), session);
        PrototypeNetwork.broadcast(player, update(player, session, true));
    }

    public static void enterAt(ServerPlayer player, BlockPos pos, LobbyPlaces.Place place) {
        player.teleportTo(place.feet().x, place.feet().y, place.feet().z);
        player.setYRot(place.yaw()); player.setYHeadRot(place.yaw());
        Session session = new Session(pos.immutable(), place.feet(), place.yaw(), PreparationGames.active(player.getUUID())?0:3, Long.MIN_VALUE, false);
        ACTIVE.put(player.getUUID(), session);
        if (place.seated()) {
            TableSeat seat = new TableSeat(player.level());
            seat.setPos(place.feet());
            player.serverLevel().addFreshEntity(seat);
            player.startRiding(seat, true);
        }
        player.setDeltaMovement(Vec3.ZERO);
        syncPosition(player);
        PrototypeNetwork.broadcast(player, update(player, session, true));
    }

    /** Mounting suppresses the tracker's regular position updates. Publish the
     * final teleport explicitly so observers cannot retain the pre-seat position. */
    private static void syncPosition(ServerPlayer player){
        player.serverLevel().getChunkSource().broadcast(player,
            new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(player));
    }

    public static boolean tableInUse(ServerPlayer viewer, BlockPos pos) {
        if(PreparationGames.tableInUse(viewer.serverLevel(),pos))return true;
        for (var entry : ACTIVE.entrySet()) {
            ServerPlayer player = viewer.serverLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.serverLevel() == viewer.serverLevel() && entry.getValue().block.equals(pos)) return true;
        }
        return false;
    }

    public static void leave(ServerPlayer player) {
        PreparationGames.leave(player);
        TestPlayers.remove(player.getUUID());
        releaseForAway(player);
    }
    public static void releaseForAway(ServerPlayer player){
        Session session = ACTIVE.remove(player.getUUID());
        if (player.getVehicle() instanceof TableSeat seat) {
            double x=seat.getX(),y=seat.getY()+TableSeat.HIP_HEIGHT,z=seat.getZ();
            player.stopRiding();seat.discard();if(player.isAlive()){player.teleportTo(x,y,z);syncPosition(player);}
        }
        if (session != null) PrototypeNetwork.broadcast(player, update(player, session, false));
    }

    private static SessionUpdate update(ServerPlayer player, Session session, boolean active) {
        return new SessionUpdate(player.getUUID(), active, session.block, session.yaw, session.cardCount, session.hidden);
    }

    /** Debug controls only; future game rules will own the actual hand contents. */
    public static void adjustHand(ServerPlayer player, int delta) {
        if(PreparationGames.active(player.getUUID()))return;
        if (delta != 1 && delta != -1) return;
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || !player.isAlive()) return;
        long tick = player.level().getGameTime();
        if (session.lastAdjustment == tick) return;
        if (delta < 0 && session.cardCount == 0 || delta > 0 && session.cardCount == Integer.MAX_VALUE) return;
        Session changed = new Session(session.block, session.origin, session.yaw, session.cardCount + delta, tick, session.hidden);
        ACTIVE.put(player.getUUID(), changed);
        PrototypeNetwork.broadcast(player, update(player, changed, true));
    }

    public static void hideCards(ServerPlayer player, boolean hidden) {
        if(PreparationGames.hideHand(player,hidden))return;
        Session session = ACTIVE.get(player.getUUID());
        if (session == null || !player.isAlive() || session.hidden == hidden) return;
        Session changed = new Session(session.block, session.origin, session.yaw, session.cardCount, session.lastAdjustment, hidden);
        ACTIVE.put(player.getUUID(), changed);
        PrototypeNetwork.broadcast(player, update(player, changed, true));
    }

    private static void tick(net.minecraft.world.entity.player.Player entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        Lobbies.tick(player);
        PreparationGames.tick(player);
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) return;
        if(!player.isAlive()){if(!PreparationGames.died(player))leave(player);return;}
        if (!player.level().getBlockState(session.block).is(ModContent.LOBBY.get())) { leave(player); return; }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        if (player.position().distanceToSqr(session.origin) > 0.0001) {
            player.teleportTo(session.origin.x, session.origin.y, session.origin.z);
            syncPosition(player);
        }
    }

    public static void beginPreparation(ServerPlayer player,BlockPos pos){
        Session session=new Session(pos.immutable(),player.position(),player.getYRot(),0,Long.MIN_VALUE,false);
        ACTIVE.put(player.getUUID(),session);PrototypeNetwork.broadcast(player,update(player,session,true));
    }
}
