package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.mixin.ButtonAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

/** Vanilla pause menu over a suspended game screen; returning does not leave the table. */
public final class TabletopPauseScreen extends PauseScreen {
    private static TabletopPauseScreen current;
    private final PrototypeScreen parent;
    private TabletopPauseScreen(PrototypeScreen parent){super(true);this.parent=parent;}
    public static void open(PrototypeScreen parent){parent.suspendChat();current=new TabletopPauseScreen(parent);Minecraft.getInstance().setScreen(current);}
    public static Screen suspended(){return current==null?null:current.parent;}
    @Override protected void init() {
        super.init();
        var returnLabel = Component.translatable("menu.returnToGame");
        for (var child : children()) {
            if (child instanceof Button button && button.getMessage().equals(returnLabel)) {
                // The vanilla callback grabs the world mouse and closes the screen.
                ((ButtonAccessor) button).tabletopStuff$setOnPress(ignored -> onClose());
                break;
            }
        }
    }
    public static void closeFromServer(){
        if(current==null)return;var parent=current.parent;current=null;parent.discardChat();Minecraft.getInstance().setScreen(null);
    }
    public static void clear(){current=null;}
    public Screen parent(){return parent;}
    @Override public void onClose(){
        if(current!=this)return;
        current=null;
        minecraft.setScreen(parent);
    }
    @Override public void removed(){}
}
