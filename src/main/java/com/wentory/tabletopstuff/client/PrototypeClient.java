package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.network.PrototypeNetwork;
import com.wentory.tabletopstuff.prototype.SessionUpdate;
import com.wentory.tabletopstuff.prototype.HandLayout;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class PrototypeClient {
    private static final Map<UUID, SessionUpdate> ACTIVE = new HashMap<>();
    private static final Map<UUID, List<HandLayout.Card>> HANDS = new HashMap<>();
    private static final List<HandLayout.Card> TEST_HAND = HandLayout.arrange(3);
    private static final Map<UUID, com.wentory.tabletopstuff.prototype.CardPoseBlend> HIDDEN = new HashMap<>();
    public static float hiddenAmount(Entity player) {
        var blend = HIDDEN.get(player.getUUID());
        return blend == null ? 0 : blend.value(System.nanoTime());
    }
    public static boolean hidden() {
        var player = Minecraft.getInstance().player;
        var state = player == null ? null : session(player);
        return state != null && state.hidden();
    }
    public static boolean canSelectCards() {
        var player = Minecraft.getInstance().player;
        var prep=PreparationClient.of(player);if(prep!=null&&(prep.play(player.getUUID())!=null||prep.take(player.getUUID())<1))return false;
        return player != null && Minecraft.getInstance().screen instanceof PrototypeScreen screen
            && !screen.pointerOnControls() && !hidden() && hiddenAmount(player) <= 0.001f;
    }
    public static void hideCards(boolean hidden) {
        var player = Minecraft.getInstance().player;
        var state = player == null ? null : session(player);
        if (state == null || state.hidden() == hidden) return;
        if(PreparationClient.of(player)!=null){preparedHidden(player.getUUID(),hidden);CardHover.clearHover();PrototypeNetwork.hideCards(hidden);return;}
        // Immediate local response; the server confirms and publishes the pose to observers.
        receive(new SessionUpdate(state.player(), state.active(), state.lobby(), state.bodyYaw(), state.cardCount(), hidden));
        CardHover.clear();
        PrototypeNetwork.hideCards(hidden);
    }

    public static void preparedHidden(UUID id,boolean hidden){
        HIDDEN.computeIfAbsent(id,key->new com.wentory.tabletopstuff.prototype.CardPoseBlend(hidden)).target(hidden,System.nanoTime());
    }
    public static SessionUpdate session(Entity player) {
        if(player==null)return null;
        var prepared=PreparationClient.of(player);
        if(prepared!=null&&prepared.update.phase()>0){var actor=prepared.actor(player.getUUID());if(actor.awayState()==com.wentory.tabletopstuff.prototype.AwayState.AWAY||actor.awayState()==com.wentory.tabletopstuff.prototype.AwayState.RETIRED)return null;return new SessionUpdate(actor.id(),true,prepared.update.table(),actor.yaw(),actor.faces().length,actor.hidden());}
        return ACTIVE.get(player.getUUID());
    }
    public static boolean active(UUID id) { return ACTIVE.containsKey(id); }
    public static List<HandLayout.Card> hand(Entity player) {
        var prepared=PreparationClient.hand(player);if(prepared!=null)return prepared;
        if (player instanceof com.wentory.tabletopstuff.prototype.TestPlayer) return TEST_HAND;
        return HANDS.getOrDefault(player.getUUID(), List.of());
    }

    public static void receive(SessionUpdate update) {
        SessionUpdate previous = ACTIVE.get(update.player());
        if (update.active()) {
            if (update.cardCount() < 0) return;
            ACTIVE.put(update.player(), update);
            HIDDEN.computeIfAbsent(update.player(), id -> new com.wentory.tabletopstuff.prototype.CardPoseBlend(update.hidden()))
                .target(update.hidden(), System.nanoTime());
            if (previous == null || previous.cardCount() != update.cardCount()) HANDS.put(update.player(), HandLayout.arrange(update.cardCount()));
        } else {
            ACTIVE.remove(update.player());
            HANDS.remove(update.player());
            HIDDEN.remove(update.player());
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(update.player())) return;
        if (update.active()) {
            if (!(TabletopChatScreen.active() instanceof PrototypeScreen)) mc.setScreen(new PrototypeScreen(update));
            else ((PrototypeScreen)TabletopChatScreen.active()).updateSession(update);
        }
        else if (TabletopChatScreen.active() instanceof PrototypeScreen screen) screen.closeFromServer();
    }

    public static void forgetLocal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) { ACTIVE.remove(mc.player.getUUID()); HANDS.remove(mc.player.getUUID()); HIDDEN.remove(mc.player.getUUID()); }
    }

    public static void clear() { TabletopPauseScreen.clear();ACTIVE.clear(); HANDS.clear(); HIDDEN.clear(); CardHover.clear(); PreparationClient.clear(); }

    public static boolean controlling() { return TabletopChatScreen.active() instanceof PrototypeScreen; }

    public static boolean canPlayCard(int index) {
        var player=Minecraft.getInstance().player;var game=PreparationClient.of(player);if(game==null||game.actor(player.getUUID())==null)return false;
        for(int allowed:game.actor(player.getUUID()).playable())if(allowed==index)return true;return false;
    }
}
