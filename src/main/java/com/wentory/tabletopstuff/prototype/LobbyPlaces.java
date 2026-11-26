package com.wentory.tabletopstuff.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Each place is 1.2 blocks from the deck centre, checked from deck Y-1 upwards. */
public final class LobbyPlaces {
    public record Place(int index, Vec3 feet, float yaw, boolean seated) {}

    public static List<Place> find(ServerLevel level, BlockPos deck, Set<UUID> participants) {
        List<Place> places = new ArrayList<>();
        var viewer = participants.stream().map(id -> level.getServer().getPlayerList().getPlayer(id))
            .filter(java.util.Objects::nonNull).filter(p -> p.serverLevel() == level).findFirst().orElse(null);
        if (viewer == null) return List.of();
        Vec3 target = Vec3.atCenterOf(deck).add(0, -0.4, 0);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos base = deck.relative(side).below();
            if (!level.hasChunkAt(base) || !level.getWorldBorder().isWithinBounds(base)) continue;
            var shape = level.getBlockState(base).getCollisionShape(level, base);
            boolean seated = !shape.isEmpty();
            double y = seated ? base.getY() + shape.max(Direction.Axis.Y) : base.getY();
            Vec3 feet = new Vec3(base.getX() + 0.5 + side.getStepX()*0.2,
                seated?y-TableSeat.HIP_HEIGHT:y, base.getZ() + 0.5 + side.getStepZ()*0.2);
            // Keep the 1x1 block column fixed; only the participant's position is offset.
            AABB clearance = new AABB(base.getX(), y + 0.001, base.getZ(), base.getX()+1,
                Math.max(base.getY()+3, y+1.8), base.getZ()+1);
            if (!level.noCollision(clearance)) continue;
            AABB body = new AABB(feet.x-0.3, feet.y, feet.z-0.3, feet.x+0.3, feet.y+1.8, feet.z+0.3);
            if (!level.getEntities((net.minecraft.world.entity.Entity)null, body,
                e -> !participants.contains(e.getUUID()) && !e.isSpectator() && (e instanceof LivingEntity || e.canBeCollidedWith())).isEmpty()) continue;
            if (!level.getBlockCollisions(null, new AABB(feet.x-0.29, y-0.08, feet.z-0.29, feet.x+0.29, y, feet.z+0.29)).iterator().hasNext()) continue;
            // Vanilla passenger pose does not shorten the player's collision box or eye height.
            Vec3 eye = feet.add(0, 1.62, 0);
            // The deck itself is the target; trace to just above its surface.
            Vec3 sight = new Vec3(target.x, deck.getY()+0.22, target.z);
            if (level.clip(new ClipContext(eye, sight, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType() != HitResult.Type.MISS) continue;
            float yaw = (float)Math.toDegrees(Math.atan2(target.z-feet.z, target.x-feet.x))-90;
            int index = switch (side) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> throw new IllegalArgumentException(); };
            places.add(new Place(index, feet, yaw, seated));
        }
        // A low ceiling over the deck also makes the floating menu and play surface unusable.
        if (!level.noCollision(new AABB(deck.getX(), deck.getY()+0.25, deck.getZ(), deck.getX()+1, deck.getY()+2, deck.getZ()+1))) return List.of();
        return List.copyOf(places);
    }
}
