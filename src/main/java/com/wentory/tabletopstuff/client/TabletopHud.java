package com.wentory.tabletopstuff.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class TabletopHud {
    private static TabletopHudState state = new TabletopHudState();

    public static void update() {
        var player = Minecraft.getInstance().player;
        if (player == null) { reset(); return; }
        state.update(PrototypeClient.controlling() || TabletopChatScreen.active() instanceof LobbyScreen, player.getHealth(), player.getFoodData().getFoodLevel(), System.nanoTime());
    }

    public static void reset() { state = new TabletopHudState(); }

    /** Only vanilla layers are intercepted. Mod HUD integrations are deliberately deferred. */
    public static boolean intercept(ResourceLocation id, GuiGraphics graphics, Runnable drawVanilla) {
        if (!id.getNamespace().equals("minecraft")) return false;
        String layer = id.getPath();
        if (TabletopChatScreen.active() instanceof LobbyScreen && layer.equals("crosshair")) return true;
        boolean playing = PrototypeClient.controlling() || TabletopChatScreen.active() instanceof LobbyScreen;
        if (!playing && !(layer.equals("hotbar") && state.visibility(layer) < 1)) return false;
        if (layer.equals("chat") || layer.equals("chat_panel")) return false;
        float visibility = state.visibility(layer);
        if (visibility > 0) {
            // Invoke the original layer, retaining vanilla hearts, status effects and texture packs.
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, TabletopHudState.slide(visibility, graphics.guiHeight()), 0);
                drawVanilla.run();
            } finally { graphics.pose().popPose(); }
        }
        return true;
    }
}
