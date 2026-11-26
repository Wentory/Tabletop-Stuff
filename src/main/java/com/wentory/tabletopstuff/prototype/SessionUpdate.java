package com.wentory.tabletopstuff.prototype;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SessionUpdate(UUID player, boolean active, BlockPos lobby, float bodyYaw, int cardCount, boolean hidden) {
    public SessionUpdate(UUID player, boolean active, BlockPos lobby, float bodyYaw, int cardCount) {
        this(player, active, lobby, bodyYaw, cardCount, false);
    }
    public static SessionUpdate read(FriendlyByteBuf buf) {
        return new SessionUpdate(buf.readUUID(), buf.readBoolean(), buf.readBlockPos(), buf.readFloat(), buf.readVarInt(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeBoolean(active);
        buf.writeBlockPos(lobby);
        buf.writeFloat(bodyYaw);
        buf.writeVarInt(cardCount);
        buf.writeBoolean(hidden);
    }
}
