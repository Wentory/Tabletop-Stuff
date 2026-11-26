package com.wentory.tabletopstuff.client;

import net.minecraft.client.Minecraft;

import com.wentory.tabletopstuff.network.PrototypeNetwork;
import com.wentory.tabletopstuff.prototype.SessionUpdate;
import com.wentory.tabletopstuff.prototype.VisualPose;
import com.wentory.tabletopstuff.prototype.DampedValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Transparent, non-pausing input context: vanilla owns cursor release/capture and focus. */
public final class PrototypeScreen extends Screen {
    private SessionUpdate session;
    private final CameraType previousCamera;
    private final float initialPitch;
    private final float initialYaw;
    private boolean serverClosed,lastDown,chatOpen,f1Down;
    private final DampedValue yaw = new DampedValue();
    private final DampedValue pitch = new DampedValue();
    private float breath;
    private final long started = System.nanoTime();
    private long previousFrame,lastHintAt;
    private long controlsAt;
    private float hideReveal,awayReveal,passReveal,cancelReveal,hideHover,awayHover,passHover,cancelHover,hiddenBlend;

    public PrototypeScreen(SessionUpdate session) {
        super(Component.translatable("block.tabletop_stuff.lobby"));
        this.session = session;
        previousCamera = Minecraft.getInstance().options.getCameraType();
        var player=Minecraft.getInstance().player;initialPitch=player==null?22:player.getXRot();initialYaw=player==null?session.bodyYaw():player.getYRot();
    }

    @Override
    protected void init() {
        if(chatOpen){chatOpen=false;previousFrame=0;return;}
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUse.setDown(false);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(returnFade()){
            graphics.flush();PreparationRenderer.clear();
            float fade=PreparationClient.fade();if(fade>0)graphics.fill(0,0,width,height,((int)(fade*255)<<24));
            return;
        }
        graphics.flush();PreparationRenderer.overlay();
        if(minecraft.options.hideGui){
            float fade=PreparationClient.fade();if(fade>0)graphics.fill(0,0,width,height,((int)(fade*255)<<24));
            return;
        }
        renderControls(graphics,mouseX,mouseY);
        var preparation=PreparationClient.local();
        if(preparation!=null&&preparation.update.rules().started()){if(lastHintAt==0)lastHintAt=System.nanoTime();if(System.nanoTime()-lastHintAt<5_000_000_000L)graphics.drawCenteredString(font,Component.translatable("tabletop_stuff.null.space_hint"),width/2,height-78,0xC8CBC5);}
        String tip=PreparationRenderer.tooltip();
        if(!tip.isEmpty()&&!pointerOnControls()){
            float scale=.85f;
            float x=Math.max(4,Math.min(mouseX+5,width-font.width(tip)*scale-4)),y=Math.min(mouseY+6,height-10);
            graphics.pose().pushPose();graphics.pose().translate(x,y,0);graphics.pose().scale(scale,scale,1);
            graphics.fill(-4,-3,font.width(tip)+4,12,0xBB171D20);
            graphics.drawString(font,tip,0,0,PreparationRenderer.tooltipColor(),false);graphics.pose().popPose();
        }
        float fade=PreparationClient.fade();if(fade>0)graphics.fill(0,0,width,height,((int)(fade*255)<<24));
    }

    private int hideWidth(){return Math.max(font.width(Component.translatable("tabletop_stuff.cards.hide")),font.width(Component.translatable("tabletop_stuff.cards.show")))+32;}
    private boolean controlHit(double x,double y,int left,int top,int w){return !minecraft.options.hideGui&&x>=left&&x<left+w&&y>=top&&y<top+22;}
    private static float approach(float value,boolean target,float speed){return value+((target?1:0)-value)*speed;}
    private static int tint(int rgb,float opacity){return (Math.max(opacity>0?4:0,Math.min(255,Math.round(opacity*255)))<<24)|rgb;}
    private void renderControls(GuiGraphics g,int mouseX,int mouseY){
        long now=System.nanoTime();float dt=controlsAt==0?0:Math.min(.1f,(now-controlsAt)/1_000_000_000f);controlsAt=now;
        float speed=1-(float)Math.exp(-14*dt);
        hideReveal=approach(hideReveal,PreparationClient.hasHand(),speed);
        awayReveal=approach(awayReveal,canStepAway(),speed);passReveal=approach(passReveal,canPass(),speed);
        cancelReveal=approach(cancelReveal,canCancelVote(),speed);cancelHover=approach(cancelHover,onCancel(mouseX,mouseY),speed);
        hideHover=approach(hideHover,onHideText(mouseX,mouseY),speed);
        awayHover=approach(awayHover,onAway(mouseX,mouseY),speed);passHover=approach(passHover,onPassText(mouseX,mouseY),speed);
        hiddenBlend=approach(hiddenBlend,PrototypeClient.hidden(),speed);
        int w=hideWidth(),left=(width-w)/2,top=height-32;
        if(hideReveal>.01f){
            controlBackground(g,left,top,w,hideReveal,hideHover);
            float scale=1+.025f*hideHover;
            g.pose().pushPose();g.pose().translate(width/2f,top+7+(1-hideReveal)*3,0);g.pose().scale(scale,scale,1);
            controlText(g,Component.translatable("tabletop_stuff.cards.hide"),hideReveal*(1-hiddenBlend),hideHover);
            controlText(g,Component.translatable("tabletop_stuff.cards.show"),hideReveal*hiddenBlend,hideHover);
            g.pose().popPose();
            // Small pixel chevron changes direction with the hand's shown/hidden state.
            int color=tint(hideHover>.1f?0xE9CD81:0xBAC2BF,hideReveal*.85f),cy=top+10;
            for(int i=0;i<3;i++){int yy=cy+(PrototypeClient.hidden()?-i:i);g.fill(left+8+i,yy,left+9+i,yy+1,color);g.fill(left+12-i,yy,left+13-i,yy+1,color);}
        }
        simpleControl(g,awayLabel(),width-font.width(awayLabel())-36,top,font.width(awayLabel())+24,awayReveal,awayHover);
        simpleControl(g,cancelLabel(),width-font.width(cancelLabel())-36,height-60,font.width(cancelLabel())+24,cancelReveal,cancelHover);
        int pw=font.width(passLabel())+24;
        simpleControl(g,passLabel(),(width-pw)/2,height-60,pw,passReveal,passHover);
    }
    private void controlBackground(GuiGraphics g,int x,int y,int w,float reveal,float hover){
        if(reveal<=.01f)return;
        int fill=tint(0x171D20,reveal*(.63f+.12f*hover));
        g.fill(x+1,y,x+w-1,y+22,fill);g.fill(x,y+1,x+w,y+21,fill);
        g.fill(x+2,y,x+w-2,y+1,tint(0xB8C5BE,reveal*(.12f+.12f*hover)));
        g.fill(x+8,y+21,x+w-8,y+22,tint(0xE9CD81,reveal*hover*.65f));
    }
    private void controlText(GuiGraphics g,Component text,float opacity,float hover){
        if(opacity<=.01f)return;
        int r=Math.round(219+14*hover),green=Math.round(225-20*hover),b=Math.round(220-91*hover);
        g.drawString(font,text,-font.width(text)/2,0,tint(r<<16|green<<8|b,opacity),false);
    }
    private void simpleControl(GuiGraphics g,Component text,int x,int y,int w,float reveal,float hover){
        if(reveal<=.01f)return;
        controlBackground(g,x,y,w,reveal,hover);
        g.pose().pushPose();g.pose().translate(x+w/2f,y+7+(1-reveal)*3,0);g.pose().scale(1+.025f*hover,1+.025f*hover,1);
        controlText(g,text,reveal,hover);g.pose().popPose();
    }

    //? if >=1.21.1 {
    /*@Override public void renderBackground(GuiGraphics graphics, int x, int y, float partialTick) {}
    @Override protected void renderBlurredBackground(float partialTick) {}
    *///?}

    private Component hideLabel() {
        return Component.translatable(PrototypeClient.hidden() ? "tabletop_stuff.cards.show" : "tabletop_stuff.cards.hide");
    }
    private Component awayLabel(){return Component.translatable("tabletop_stuff.game.step_away");}
    private Component cancelLabel(){return Component.translatable("tabletop_stuff.vote.start");}
    private boolean canCancelVote(){
        var prep=PreparationClient.local();var player=Minecraft.getInstance().player;if(prep==null||player==null||prep.update.phase()==0||prep.update.vote().active())return false;
        var actor=prep.actor(player.getUUID());return actor!=null&&actor.awayState()==com.wentory.tabletopstuff.prototype.AwayState.ACTIVE;
    }
    private boolean onCancel(double x,double y){return canCancelVote()&&controlHit(x,y,width-font.width(cancelLabel())-36,height-60,font.width(cancelLabel())+24);}
    private boolean canStepAway(){
        var prep=PreparationClient.local();var player=minecraft.player;
        if(prep==null||player==null||prep.update.phase()!=4||!prep.update.rules().started())return false;
        var actor=prep.actor(player.getUUID());return actor!=null&&actor.awayState()==com.wentory.tabletopstuff.prototype.AwayState.ACTIVE
            &&!prep.update.rules().chooser().equals(player.getUUID())&&prep.take(player.getUUID())>=1&&prep.play(player.getUUID())==null;
    }
    private boolean onAway(double x,double y){return canStepAway()&&controlHit(x,y,width-font.width(awayLabel())-36,height-32,font.width(awayLabel())+24);}

    private Component passLabel(){return Component.translatable("tabletop_stuff.null.keep");}
    private boolean canPass(){
        var preparation=PreparationClient.local();var player=Minecraft.getInstance().player;
        return preparation!=null&&player!=null&&preparation.actor(player.getUUID())!=null&&preparation.actor(player.getUUID()).canPass();
    }
    private boolean onPassText(double x,double y){
        if(!canPass())return false;int w=font.width(passLabel())+24;
        return controlHit(x,y,(width-w)/2,height-60,w);
    }
    public boolean pointerOnControls() {
        var window = minecraft.getWindow();
        double x=minecraft.mouseHandler.xpos()*width/window.getScreenWidth(),y=minecraft.mouseHandler.ypos()*height/window.getScreenHeight();
        return onHideText(x,y)||onPassText(x,y)||onAway(x,y)||onCancel(x,y);
    }

    private boolean onHideText(double x, double y) {
        if(!PreparationClient.hasHand())return false;
        int w=hideWidth();return controlHit(x,y,(width-w)/2,height-32,w);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        var preparation=PreparationClient.local();
        if(preparation!=null&&minecraft.player!=null&&preparation.actor(minecraft.player.getUUID()).awayState()!=com.wentory.tabletopstuff.prototype.AwayState.ACTIVE)return true;
        if(button==0&&onCancel(x,y)){CardHover.clear();PrototypeNetwork.lobbyAction(com.wentory.tabletopstuff.prototype.PreparationUpdate.START_CANCEL_VOTE,0);return true;}
        if(button==0&&onAway(x,y)){CardHover.clear();PrototypeNetwork.lobbyAction(com.wentory.tabletopstuff.prototype.PreparationUpdate.STEP_AWAY,0);return true;}
        if(button==0&&onPassText(x,y)){CardHover.clear();PrototypeNetwork.lobbyAction(com.wentory.tabletopstuff.prototype.PreparationUpdate.PASS,0);return true;}
        if (PrototypeClient.hidden()) {
            boolean interact=button==0&&!onHideText(x,y)&&PreparationRenderer.hasTarget();
            PrototypeClient.hideCards(false);
            if(interact)PreparationRenderer.click();
            return true;
        }
        if (button == 0 && onHideText(x, y)) {
            PrototypeClient.hideCards(true);
            return true;
        }
        if(button==0&&PreparationClient.local()!=null){
            if(PreparationClient.local().update.phase()==4&&PrototypeClient.canSelectCards()&&CardHover.hovered()>=0){if(PrototypeClient.canPlayCard(CardHover.hovered()))CardHover.selectHovered();return true;}
            PreparationRenderer.click();return true;
        }
        return false;
    }

    private boolean returnFade(){var prep=PreparationClient.local();return prep!=null&&minecraft.player!=null&&prep.actor(minecraft.player.getUUID()).awayState()==com.wentory.tabletopstuff.prototype.AwayState.RETURN_FADE;}
    public float viewYaw() { if(returnFade())return initialYaw;return session.bodyYaw() + yaw.value(); }
    public float viewPitch(){
        var prep=PreparationClient.local();
        if(returnFade())return initialPitch;
        float center=prep!=null&&prep.update.phase()==0?initialPitch:22;
        return center+pitch.value()+breath;
    }

    public void updateSession(SessionUpdate update){
        if(session.bodyYaw()!=update.bodyYaw()){yaw.reset();pitch.reset();}
        session=update;
    }


    public void updateLook() {
        
        if (minecraft.player == null) return;
        var prep=PreparationClient.local();if(prep!=null&&prep.update.phase()==0)return;
        long now = System.nanoTime();
        double elapsed = previousFrame == 0 ? 0 : Math.min((now - previousFrame) / 1_000_000_000.0, 0.1);
        previousFrame = now;
        var window = minecraft.getWindow();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        float yawLimit = firstPerson ? VisualPose.MAX_YAW : 100;
        float pitchLimit = firstPerson ? VisualPose.MAX_PITCH : 35;
        float targetYaw = VisualPose.cursorOffset(minecraft.mouseHandler.xpos(), window.getScreenWidth(), yawLimit);
        float targetPitch = VisualPose.cursorOffset(minecraft.mouseHandler.ypos(), window.getScreenHeight(), pitchLimit);
        yaw.update(targetYaw, elapsed, 12, yawLimit);
        pitch.update(targetPitch, elapsed, 12, pitchLimit);
        double phase = (now - started) / 1_000_000_000.0 * (Math.PI * 2 / 4.8);
        breath = firstPerson ? (float) Math.sin(phase) * 0.35f * (1 - 0.6f * CardHover.focus()) : 0;
        if (firstPerson) {
            var selected=Minecraft.getInstance().player;selected.setYRot(viewYaw());selected.setXRot(viewPitch());selected.setYHeadRot(viewYaw());
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if(key==GLFW.GLFW_KEY_F1){if(!f1Down){f1Down=true;minecraft.options.hideGui=!minecraft.options.hideGui;}return true;}
        if(TabletopChatScreen.open(this,key,scanCode))return true;
        var preparation=PreparationClient.local();
        if(key!=GLFW.GLFW_KEY_ESCAPE&&preparation!=null&&minecraft.player!=null&&preparation.actor(minecraft.player.getUUID()).awayState()!=com.wentory.tabletopstuff.prototype.AwayState.ACTIVE)return true;
        if(key==GLFW.GLFW_KEY_SPACE){if(!lastDown){lastDown=true;PrototypeNetwork.lobbyAction(com.wentory.tabletopstuff.prototype.PreparationUpdate.LAST,0);}return true;}
        if (key == GLFW.GLFW_KEY_ESCAPE) { TabletopPauseScreen.open(this); return true; }
        if (key == GLFW.GLFW_KEY_KP_ADD || key == GLFW.GLFW_KEY_KP_SUBTRACT) {
            if (PrototypeClient.hidden()) return true;
            PrototypeNetwork.adjustHand(key == GLFW.GLFW_KEY_KP_ADD ? 1 : -1);
            return true;
        }
        if (minecraft.options.keyTogglePerspective.matches(key, scanCode)) {
            CardHover.clear();
            minecraft.options.setCameraType(minecraft.options.getCameraType().cycle());
            yaw.reset();
            pitch.reset();
            breath = 0;
            return true;
        }
        return true;
    }

    @Override public boolean keyReleased(int key,int scan,int modifiers){if(key==GLFW.GLFW_KEY_F1){f1Down=false;return true;}if(key==GLFW.GLFW_KEY_SPACE){lastDown=false;return true;}return super.keyReleased(key,scan,modifiers);}

    @Override
    public void onClose() { minecraft.setScreen(null); }

    public void closeFromServer() {
        serverClosed = true;
        if(TabletopPauseScreen.suspended()==this){TabletopPauseScreen.closeFromServer();return;}
        var prep=PreparationClient.local();
        if(prep!=null&&minecraft.player!=null&&prep.actor(minecraft.player.getUUID())!=null&&prep.actor(minecraft.player.getUUID()).awayState()==com.wentory.tabletopstuff.prototype.AwayState.AWAY){minecraft.setScreen(new AwayFadeScreen());return;}
        onClose();
    }

    @Override
    public void removed() {
        if(chatOpen)return;
        
        CardHover.clear();
        PrototypeClient.forgetLocal();
        minecraft.options.setCameraType(previousCamera);
        if (!serverClosed && minecraft.getConnection() != null) PrototypeNetwork.leave();
    }
    public void suspendChat(){chatOpen=true;lastDown=false;f1Down=false;CardHover.clear();}
    public void discardChat(){chatOpen=false;removed();}
}
