package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.TabletopStuff;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid = TabletopStuff.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.*;
@EventBusSubscriber(modid = TabletopStuff.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class ClientEvents {
    private record Body(float yaw, float previousYaw) {}
    private static final Map<UUID, Body> BODIES = new HashMap<>();
    //? if >=1.21.1 {
    /*@SubscribeEvent
    public static void menuScreens(RegisterMenuScreensEvent event){
        event.register(com.wentory.tabletopstuff.ModContent.WORKSHOP_MENU.get(),WorkshopScreen::new);
    }
    *///?}

    @SubscribeEvent
    public static void blockRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.wentory.tabletopstuff.ModContent.LOBBY_ENTITY.get(), LobbyRenderer::new);
        event.registerEntityRenderer(com.wentory.tabletopstuff.ModContent.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(com.wentory.tabletopstuff.ModContent.TEST_PLAYER.get(), TestPlayerRenderer::new);
    }

    @SubscribeEvent
    public static void textures(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new CardAtlas());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            //? if <1.21.1 {
            net.minecraft.client.gui.screens.MenuScreens.register(com.wentory.tabletopstuff.ModContent.WORKSHOP_MENU.get(),WorkshopScreen::new);
            //?}
            PrototypeNetwork.clientReceiver = PrototypeClient::receive;
            PrototypeNetwork.lobbyReceiver = LobbyScreen::receive;
            PrototypeNetwork.preparationReceiver = PreparationClient::receive;
            com.wentory.tabletopstuff.prototype.PrototypeSessions.clientLocked = PrototypeClient::active;
            //? if >=1.21.1 {
            /*var bus = NeoForge.EVENT_BUS;
            bus.addListener((RenderGuiLayerEvent.Pre e) -> {
                if (TabletopHud.intercept(e.getName(), e.getGuiGraphics(),
                    () -> e.getLayer().render(e.getGuiGraphics(), e.getPartialTick()))) e.setCanceled(true);
            });
            *///?} else {
            var bus = MinecraftForge.EVENT_BUS;
            bus.addListener((RenderGuiOverlayEvent.Pre e) -> {
                if (TabletopHud.intercept(e.getOverlay().id(), e.getGuiGraphics(),
                    () -> e.getOverlay().overlay().render((net.minecraftforge.client.gui.overlay.ForgeGui) Minecraft.getInstance().gui,
                        e.getGuiGraphics(), e.getPartialTick(), e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight()))) e.setCanceled(true);
            });
            //?}
            bus.addListener((RenderGuiEvent.Pre e) -> {
                if(Minecraft.getInstance().screen instanceof WorkshopScreen)e.setCanceled(true);
                else TabletopHud.update();
            });
            bus.addListener((RenderGuiEvent.Post e) -> {
                if(!(TabletopChatScreen.active() instanceof PrototypeScreen)){
                    e.getGuiGraphics().flush();PreparationRenderer.overlay();
                }
            });
            bus.addListener(ClientEvents::camera);
            bus.addListener(ClientEvents::hand);
            bus.addListener((RenderHighlightEvent.Block e) -> {
                if (PrototypeClient.controlling() || TabletopChatScreen.active() instanceof LobbyScreen) e.setCanceled(true);
            });
            bus.addListener(ClientEvents::world);
            bus.addListener(ClientEvents::beforePlayer);
            bus.addListener(ClientEvents::afterPlayer);
            bus.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { PrototypeClient.clear(); BODIES.clear(); TabletopHud.reset(); LobbyCamera.clear(); });
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new CardLayer(renderer));
        }
    }

    private static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (TabletopChatScreen.active() instanceof PrototypeScreen screen) {
            if(!TabletopChatScreen.open())screen.updateLook();
            event.setYaw(screen.viewYaw());
            event.setPitch(screen.viewPitch());
        }
    }

    private static void hand(RenderHandEvent event) {
        if (PrototypeClient.controlling() || TabletopChatScreen.active() instanceof LobbyScreen) event.setCanceled(true);
    }

    private static void world(RenderLevelStageEvent event) {
        //? if >=1.21.1 {
        /*if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
            && TabletopChatScreen.active() instanceof LobbyScreen) {
            var menuPose = new com.mojang.blaze3d.vertex.PoseStack();
            menuPose.mulPose(event.getModelViewMatrix());
            LobbyWorldMenu.capture(menuPose,event.getProjectionMatrix());
        }
        *///?}
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        //? if <1.21.1 {
        // Forge passes the actual camera pose here; AFTER_LEVEL has a separate stack.
        if (TabletopChatScreen.active() instanceof LobbyScreen)
            LobbyWorldMenu.capture(event.getPoseStack(),event.getProjectionMatrix());
        //?}
        //? if >=1.21.1 {
        /*float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        *///?} else {
        float partialTick = event.getPartialTick();
        //?}
        PlayerPoseRenderer.firstPerson(event.getPoseStack(), partialTick);
        PreparationRenderer.render(event.getPoseStack(), event.getProjectionMatrix());
    }

    private static void beforePlayer(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        var session = PrototypeClient.session(player);
        if (session == null) return;
        BODIES.put(player.getUUID(), new Body(player.yBodyRot, player.yBodyRotO));
        player.yBodyRot = session.bodyYaw();
        player.yBodyRotO = session.bodyYaw();
    }

    private static void afterPlayer(RenderPlayerEvent.Post event) {
        PlayerPoseRenderer.restore(event.getRenderer().getModel());
        var player = event.getEntity();
        Body body = BODIES.remove(player.getUUID());
        if (body != null) { player.yBodyRot = body.yaw; player.yBodyRotO = body.previousYaw; }
    }
}
