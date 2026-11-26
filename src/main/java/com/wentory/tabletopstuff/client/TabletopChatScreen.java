package com.wentory.tabletopstuff.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/** Vanilla chat suspended over an existing tabletop screen, preserving its session. */
public final class TabletopChatScreen extends ChatScreen {
    private final Screen parent;
    private boolean returning;
    private String openingCharacter;
    public TabletopChatScreen(Screen parent,String initial){super(initial);this.parent=parent;}
    public static Screen active(){
        Screen screen=Minecraft.getInstance().screen;
        if(TabletopPauseScreen.suspended()!=null)return TabletopPauseScreen.suspended();
        return screen instanceof TabletopChatScreen chat?chat.parent:screen instanceof TabletopPauseScreen pause?pause.parent():screen;
    }
    public static boolean open(){return Minecraft.getInstance().screen instanceof TabletopChatScreen||TabletopPauseScreen.suspended()!=null;}
    public static boolean open(Screen parent,int key,int scan){
        var mc=Minecraft.getInstance();String initial;
        if(mc.options.keyChat.matches(key,scan))initial="";
        else if(mc.options.keyCommand.matches(key,scan))initial="/";
        else return false;
        if(parent instanceof PrototypeScreen game)game.suspendChat();
        if(parent instanceof LobbyScreen lobby)lobby.suspendChat();
        var chat=new TabletopChatScreen(parent,initial);
        chat.openingCharacter=GLFW.glfwGetKeyName(key,scan);
        mc.setScreen(chat);return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        openingCharacter=null;
        if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        // Vanilla handles history, completion and sending. Enter closes directly
        // through Minecraft.setScreen(null), so restore the suspended parent afterward.
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){
            returning=true;boolean handled=super.keyPressed(key,scan,modifiers);
            if(minecraft.screen==null)minecraft.setScreen(parent);
            returning=false;return handled;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char character,int modifiers){
        String opening=openingCharacter;openingCharacter=null;
        // The character event may use a different keyboard layout than the key binding.
        if(opening!=null)return true;
        return super.charTyped(character,modifiers);
    }
    @Override public void onClose(){returning=true;minecraft.setScreen(parent);returning=false;}
    @Override public void removed(){
        super.removed();
        if(!returning){
            if(parent instanceof PrototypeScreen game)game.discardChat();
            if(parent instanceof LobbyScreen lobby)lobby.discardChat();
        }
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partialTick){parent.render(graphics,x,y,partialTick);super.render(graphics,x,y,partialTick);}
    //? if >=1.21.1 {
    /*@Override public void renderBackground(GuiGraphics graphics,int x,int y,float partialTick){}
    @Override protected void renderBlurredBackground(float partialTick){}
    *///?}
}
