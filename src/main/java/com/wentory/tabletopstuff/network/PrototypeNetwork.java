package com.wentory.tabletopstuff.network;

import com.wentory.tabletopstuff.TabletopStuff;
import com.wentory.tabletopstuff.prototype.PrototypeSessions;
import com.wentory.tabletopstuff.prototype.SessionUpdate;
import com.wentory.tabletopstuff.prototype.LobbyUpdate;
import com.wentory.tabletopstuff.prototype.Lobbies;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.1 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
*///?} else {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?}

public final class PrototypeNetwork {
    // Installed by client setup; common/server initialization never loads client classes.
    public static Consumer<SessionUpdate> clientReceiver = update -> {};
    public static Consumer<LobbyUpdate> lobbyReceiver = update -> {};
    public static Consumer<com.wentory.tabletopstuff.prototype.PreparationUpdate> preparationReceiver = update -> {};

    //? if >=1.21.1 {
    /*private record State(SessionUpdate update) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "visual_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
            (buf, value) -> value.update.write(buf), buf -> new State(SessionUpdate.read(buf)));
        public Type<State> type() { return TYPE; }
    }
    private record Leave() implements CustomPacketPayload {
        static final Type<Leave> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "leave_visual"));
        static final StreamCodec<RegistryFriendlyByteBuf, Leave> CODEC = StreamCodec.unit(new Leave());
        public Type<Leave> type() { return TYPE; }
    }
    private record AdjustHand(int delta) implements CustomPacketPayload {
        static final Type<AdjustHand> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "adjust_hand"));
        static final StreamCodec<RegistryFriendlyByteBuf, AdjustHand> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeByte(value.delta), buf -> new AdjustHand(buf.readByte()));
        public Type<AdjustHand> type() { return TYPE; }
    }
    private record LobbyState(LobbyUpdate update) implements CustomPacketPayload {
        static final Type<LobbyState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "lobby_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, LobbyState> CODEC = StreamCodec.of(
            (buf, value) -> value.update.write(buf), buf -> new LobbyState(LobbyUpdate.read(buf)));
        public Type<LobbyState> type() { return TYPE; }
    }
    private record PreparationState(com.wentory.tabletopstuff.prototype.PreparationUpdate update) implements CustomPacketPayload {
        static final Type<PreparationState> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID,"preparation"));
        static final StreamCodec<RegistryFriendlyByteBuf,PreparationState> CODEC=StreamCodec.of((b,p)->p.update.write(b),b->new PreparationState(com.wentory.tabletopstuff.prototype.PreparationUpdate.read(b)));
        public Type<PreparationState> type(){return TYPE;}
    }
    private record LobbyAction(int action, int value, java.util.UUID target) implements CustomPacketPayload {
        static final Type<LobbyAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "lobby_action"));
        static final StreamCodec<RegistryFriendlyByteBuf, LobbyAction> CODEC = StreamCodec.of(
            (buf, packet) -> { buf.writeByte(packet.action); buf.writeByte(packet.value); buf.writeUUID(packet.target); }, buf -> new LobbyAction(buf.readByte(), buf.readByte(), buf.readUUID()));
        public Type<LobbyAction> type() { return TYPE; }
    }
    private record HideCards(boolean hidden) implements CustomPacketPayload {
        static final Type<HideCards> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TabletopStuff.MOD_ID, "hide_cards"));
        static final StreamCodec<RegistryFriendlyByteBuf, HideCards> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.hidden), buf -> new HideCards(buf.readBoolean()));
        public Type<HideCards> type() { return TYPE; }
    }
    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("23");
            registrar.playToClient(PreparationState.TYPE,PreparationState.CODEC,(packet,context)->preparationReceiver.accept(packet.update));
            registrar.playToServer(HideCards.TYPE, HideCards.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player) PrototypeSessions.hideCards(player, packet.hidden);
            });
            registrar.playToClient(LobbyState.TYPE, LobbyState.CODEC, (packet, context) -> lobbyReceiver.accept(packet.update));
            registrar.playToServer(LobbyAction.TYPE, LobbyAction.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player) Lobbies.action(player, packet.action, packet.value, packet.target);
            });
            registrar.playToClient(State.TYPE, State.CODEC, (packet, context) -> clientReceiver.accept(packet.update));
            registrar.playToServer(Leave.TYPE, Leave.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player) PrototypeSessions.leave(player);
            });
            registrar.playToServer(AdjustHand.TYPE, AdjustHand.CODEC, (packet, context) -> {
                if (context.player() instanceof ServerPlayer player) PrototypeSessions.adjustHand(player, packet.delta);
            });
        });
    }
    public static void send(ServerPlayer player, SessionUpdate update) { PacketDistributor.sendToPlayer(player, new State(update)); }
    public static void broadcast(ServerPlayer player, SessionUpdate update) { PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new State(update)); }
    public static void leave() { PacketDistributor.sendToServer(new Leave()); }
    public static void adjustHand(int delta) { PacketDistributor.sendToServer(new AdjustHand(delta)); }
    public static void sendPreparation(ServerPlayer player,com.wentory.tabletopstuff.prototype.PreparationUpdate update){PacketDistributor.sendToPlayer(player,new PreparationState(update));}
    public static void sendLobby(ServerPlayer player, LobbyUpdate update) { PacketDistributor.sendToPlayer(player, new LobbyState(update)); }
    public static void lobbyAction(int action, int value) { PacketDistributor.sendToServer(new LobbyAction(action, value, new java.util.UUID(0,0))); }
    public static void lobbyAction(int action, int value, java.util.UUID target) { PacketDistributor.sendToServer(new LobbyAction(action,value,target)); }
    public static void hideCards(boolean hidden) { PacketDistributor.sendToServer(new HideCards(hidden)); }
    *///?} else {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TabletopStuff.MOD_ID, "prototype"), () -> "23", "23"::equals, "23"::equals);
    private record Leave() {}
    private record AdjustHand(int delta) {}
    private record LobbyAction(int action, int value, java.util.UUID target) {}
    private record HideCards(boolean hidden) {}
    public static void register(IEventBus bus) {
        CHANNEL.messageBuilder(com.wentory.tabletopstuff.prototype.PreparationUpdate.class,6,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(com.wentory.tabletopstuff.prototype.PreparationUpdate::write).decoder(com.wentory.tabletopstuff.prototype.PreparationUpdate::read)
            .consumerMainThread((packet,context)->preparationReceiver.accept(packet)).add();
        CHANNEL.messageBuilder(HideCards.class, 5, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buf) -> buf.writeBoolean(packet.hidden)).decoder(buf -> new HideCards(buf.readBoolean()))
            .consumerMainThread((packet, context) -> {
                ServerPlayer player = context.get().getSender();
                if (player != null) PrototypeSessions.hideCards(player, packet.hidden);
            }).add();
        CHANNEL.messageBuilder(LobbyUpdate.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(LobbyUpdate::write).decoder(LobbyUpdate::read)
            .consumerMainThread((packet, context) -> lobbyReceiver.accept(packet)).add();
        CHANNEL.messageBuilder(LobbyAction.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buf) -> { buf.writeByte(packet.action); buf.writeByte(packet.value); buf.writeUUID(packet.target); })
            .decoder(buf -> new LobbyAction(buf.readByte(), buf.readByte(), buf.readUUID()))
            .consumerMainThread((packet, context) -> {
                ServerPlayer player = context.get().getSender();
                if (player != null) Lobbies.action(player, packet.action, packet.value, packet.target);
            }).add();
        CHANNEL.messageBuilder(SessionUpdate.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SessionUpdate::write).decoder(SessionUpdate::read)
            .consumerMainThread((packet, context) -> clientReceiver.accept(packet)).add();
        CHANNEL.messageBuilder(Leave.class, 1, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buf) -> {}).decoder(buf -> new Leave())
            .consumerMainThread((packet, context) -> {
                ServerPlayer player = context.get().getSender();
                if (player != null) PrototypeSessions.leave(player);
            }).add();
        CHANNEL.messageBuilder(AdjustHand.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buf) -> buf.writeByte(packet.delta)).decoder(buf -> new AdjustHand(buf.readByte()))
            .consumerMainThread((packet, context) -> {
                ServerPlayer player = context.get().getSender();
                if (player != null) PrototypeSessions.adjustHand(player, packet.delta);
            }).add();
    }
    public static void send(ServerPlayer player, SessionUpdate update) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update); }
    public static void broadcast(ServerPlayer player, SessionUpdate update) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), update); }
    public static void leave() { CHANNEL.sendToServer(new Leave()); }
    public static void adjustHand(int delta) { CHANNEL.sendToServer(new AdjustHand(delta)); }
    public static void sendPreparation(ServerPlayer player,com.wentory.tabletopstuff.prototype.PreparationUpdate update){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),update);}
    public static void sendLobby(ServerPlayer player, LobbyUpdate update) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), update); }
    public static void lobbyAction(int action, int value) { CHANNEL.sendToServer(new LobbyAction(action, value, new java.util.UUID(0,0))); }
    public static void lobbyAction(int action, int value, java.util.UUID target) { CHANNEL.sendToServer(new LobbyAction(action,value,target)); }
    public static void hideCards(boolean hidden) { CHANNEL.sendToServer(new HideCards(hidden)); }
    //?}
}

