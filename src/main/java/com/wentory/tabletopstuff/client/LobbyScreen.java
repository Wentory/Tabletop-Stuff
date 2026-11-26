package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.PlayerStyleConfig;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import com.wentory.tabletopstuff.prototype.Lobbies;
import com.wentory.tabletopstuff.prototype.LobbyUpdate;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Input only: all visuals are private, world-space and anchored when this screen opens. */
public final class LobbyScreen extends Screen {
    private LobbyUpdate state;
    private final CameraType previousCamera;
    private boolean serverClosed,chatOpen;
    public boolean invitations, rulesOpen,playerSettings,dealerOpen;
    public int playersScroll, nearbyScroll, rulesScroll;
    public LobbyScreen(LobbyUpdate state){
        super(Component.translatable("tabletop_stuff.lobby.title"));this.state=state;
        previousCamera=Minecraft.getInstance().options.getCameraType();
        LobbyCamera.begin(state.pos());LobbyWorldMenu.reset();
        com.wentory.tabletopstuff.TabletopStuff.LOGGER.debug("Opening sectional lobby: pos={}, capacity={}, available={}",state.pos(),state.capacity(),state.available());
    }
    public LobbyUpdate state(){return state;}
    public boolean owner(){return minecraft.player!=null&&state.owner().equals(minecraft.player.getUUID());}
    public static void receive(LobbyUpdate update){
        Minecraft mc=Minecraft.getInstance();
        if(update.open()){
            if(TabletopChatScreen.active() instanceof LobbyScreen screen&&screen.state.pos().equals(update.pos())){
                screen.playChanges(update);
                screen.state=update;
            }
            else {
                var screen=new LobbyScreen(update);
                mc.setScreen(screen);
                if(screen.owner())screen.restoreSettings();
            }
        }else if(TabletopChatScreen.active() instanceof LobbyScreen screen){screen.serverClosed=true;mc.setScreen(null);}
    }
    private void playChanges(LobbyUpdate update){
        boolean joined=update.players().stream().anyMatch(member->state.players().stream().noneMatch(old->old.id().equals(member.id())));
        boolean left=state.players().stream().anyMatch(member->update.players().stream().noneMatch(next->next.id().equals(member.id())));
        boolean seated=update.players().stream().anyMatch(member->member.seat()>=0&&state.players().stream()
            .anyMatch(old->old.id().equals(member.id())&&old.seat()!=member.seat()));
        if(joined)LobbySounds.custom("joined");
        if(left)LobbySounds.custom("left");
        if(seated)LobbySounds.custom("taked_seat");
    }
    private void restoreSettings(){
        long preferences=PlayerStyleConfig.lobbyCapacity()|(PlayerStyleConfig.lobbyPrivate()?16:0)
            |(PlayerStyleConfig.lobbyRules()<<5)|(PlayerStyleConfig.firstDealer()<<10)|(PlayerStyleConfig.nextDealer()<<11);
        PrototypeNetwork.lobbyAction(Lobbies.RESTORE_SETTINGS,0,new UUID(0,preferences));
    }
    void sendAction(int action,int value,UUID target){
        switch(action){
            case Lobbies.BOT_DIFFICULTY,Lobbies.CAPACITY,Lobbies.ACCESS,Lobbies.RULE,Lobbies.FIRST_DEALER,Lobbies.NEXT_DEALER -> LobbySounds.toggle();
            case Lobbies.SEAT -> {}
            default -> LobbySounds.button();
        }
        if(owner()){
            switch(action){
                case Lobbies.CAPACITY -> PlayerStyleConfig.saveLobbyCapacity(value);
                case Lobbies.ACCESS -> PlayerStyleConfig.saveLobbyPrivate(value!=0);
                case Lobbies.RULE -> PlayerStyleConfig.saveLobbyRules(com.wentory.tabletopstuff.nullgame.NullHouseRule.toggle(PlayerStyleConfig.lobbyRules(),value));
                case Lobbies.FIRST_DEALER -> PlayerStyleConfig.saveFirstDealer(value);
                case Lobbies.NEXT_DEALER -> PlayerStyleConfig.saveNextDealer(value);
            }
        }
        PrototypeNetwork.lobbyAction(action,value,target);
    }
    @Override protected void init(){
        if(chatOpen){chatOpen=false;return;}
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        for(var key:new net.minecraft.client.KeyMapping[]{minecraft.options.keyUp,minecraft.options.keyDown,minecraft.options.keyLeft,
            minecraft.options.keyRight,minecraft.options.keyJump,minecraft.options.keyShift,minecraft.options.keyAttack,minecraft.options.keyUse})key.setDown(false);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        graphics.flush();LobbyWorldMenu.renderOverlay();
    }
    //? if >=1.21.1 {
    /*@Override public void renderBackground(GuiGraphics graphics,int x,int y,float partialTick){}
    @Override protected void renderBlurredBackground(float partialTick){}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){LobbyWorldMenu.scroll(this,dy);return true;}
    *///?} else {
    @Override public boolean mouseScrolled(double x,double y,double dy){LobbyWorldMenu.scroll(this,dy);return true;}
    //?}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0)LobbyWorldMenu.activate(this);return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(TabletopChatScreen.open(this,key,scan))return true;if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)onClose();return true;}
    @Override public void onClose(){minecraft.setScreen(null);}
    @Override public void removed(){
        if(chatOpen)return;
        LobbyCamera.end();minecraft.options.setCameraType(previousCamera);
        if(!serverClosed&&minecraft.getConnection()!=null)PrototypeNetwork.lobbyAction(0,0);
    }
    public void suspendChat(){chatOpen=true;}
    public void discardChat(){chatOpen=false;removed();}
}
