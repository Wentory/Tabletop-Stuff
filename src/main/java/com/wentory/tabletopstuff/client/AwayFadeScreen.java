package com.wentory.tabletopstuff.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Short fade back into survival after the server releases the participant. */
final class AwayFadeScreen extends Screen {
    private final long started=System.nanoTime();
    AwayFadeScreen(){super(Component.empty());}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics graphics,int x,int y,float partialTick){
        float fade=1-(System.nanoTime()-started)/200_000_000f;
        if(fade<=0){minecraft.setScreen(null);return;}
        graphics.fill(0,0,width,height,Math.round(fade*255)<<24);
    }
    //? if >=1.21.1 {
    /*@Override public void renderBackground(GuiGraphics graphics,int x,int y,float partialTick){}
    @Override protected void renderBlurredBackground(float partialTick){}
    *///?}
}
