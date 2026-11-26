package com.wentory.tabletopstuff.prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.wentory.tabletopstuff.ModContent;
import com.wentory.tabletopstuff.TabletopStuff;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TestPlayers {
    private static final Map<UUID, List<TestPlayer>> GROUPS = new HashMap<>();

    public static List<TestPlayer> spawn(ServerPlayer owner, BlockPos pos, List<LobbyPlaces.Place> remaining) {
        return spawn(owner,pos,remaining,List.of());
    }
    public static List<TestPlayer> spawn(ServerPlayer owner,BlockPos pos,List<LobbyPlaces.Place> remaining,List<UUID> bots){
        remove(owner.getUUID());
        List<TestPlayer> group = new ArrayList<>();
        var skins = new ArrayList<>(List.of(TestPlayer.BotSkin.values()));
        if (!bots.isEmpty()) Collections.shuffle(skins, new Random(owner.level().getRandom().nextLong()));
        // These places come from the same checked list used for actual participants.
        for (int i = 0; i < Math.min(3, remaining.size()); i++) {
            TestPlayer test = new TestPlayer(ModContent.TEST_PLAYER.get(), owner.level());
            if (!bots.isEmpty()) {
                test.setUUID(bots.get(i));
                test.setBotSkin(skins.get(i));
            }
            test.place(owner.getUUID(), pos, remaining.get(i));
            test.setCustomName(Component.translatable(bots.isEmpty()?"entity.tabletop_stuff.test_player":"entity.tabletop_stuff.null_bot",i+1));
            test.setCustomNameVisible(true);
            if (!owner.serverLevel().addFreshEntity(test)) { test.discard(); continue; }
            if (remaining.get(i).seated()) {
                TableSeat seat = new TableSeat(owner.level());
                seat.setPos(remaining.get(i).feet());
                if (owner.serverLevel().addFreshEntity(seat)) test.startRiding(seat, true);
            }
            group.add(test);
        }
        GROUPS.put(owner.getUUID(), group);
        TabletopStuff.LOGGER.debug("Solo test: spawned {} test players, {} blocked places", group.size(), 3-group.size());
        return List.copyOf(group);
    }
    public static void remove(UUID owner) {
        List<TestPlayer> group = GROUPS.remove(owner);
        if (group != null) group.forEach(TestPlayer::removeFromTable);
    }
    public static void clear() {
        GROUPS.values().forEach(group -> group.forEach(TestPlayer::removeFromTable));
        GROUPS.clear();
    }
}
