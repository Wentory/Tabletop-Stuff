package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import com.wentory.tabletopstuff.prototype.Lobbies;
import com.wentory.tabletopstuff.prototype.LobbyUpdate;
import com.wentory.tabletopstuff.nullgame.NullHouseRule;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** All geometry, text, skins and hit targets live on one fixed world plane. */
public final class LobbyWorldMenu {
    private static final Matrix4f INVERSE=new Matrix4f();
    private static final Matrix4f WORLD_VIEW=new Matrix4f(),WORLD_PROJECTION=new Matrix4f();
    private static boolean captured;
    private static boolean showingDealer;
    private static final Vector3f NEAR=new Vector3f(),FAR=new Vector3f();
    private static final Map<String,ResourceLocation> RESOURCES=new HashMap<>();
    private static final UUID NONE=new UUID(0,0);
    private static final int WHITE=0xF2EBDD,MUTED=0xB7B5AC,AMBER=0xF1C56B,GREEN=0x78BB75,RED=0xCA6969;
    private static float mouseX,mouseY,alpha=1,inviteReveal,rulesReveal,exitHover,playHover;
    private static long lastFrame;
    private static int action=-99,value;
    private static UUID target=NONE;
    public static void reset(){lastFrame=0;inviteReveal=rulesReveal=exitHover=playHover=0;action=-99;captured=false;showingDealer=false;}

    public static void capture(PoseStack view,Matrix4f projection){
        WORLD_VIEW.set(view.last().pose());WORLD_PROJECTION.set(projection);captured=true;
    }
    /** Draw after world post-processing, while retaining the exact world projection. */
    public static void renderOverlay(){
        if(!captured)return;
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        buffers.endBatch();
        RenderSystem.backupProjectionMatrix();
        var modelView=RenderSystem.getModelViewStack();
        //? if >=1.21.1 {
        /*modelView.pushMatrix();modelView.identity();
        *///?} else {
        modelView.pushPose();modelView.setIdentity();
        //?}
        RenderSystem.applyModelViewMatrix();
        try{
            RenderSystem.setProjectionMatrix(WORLD_PROJECTION,VertexSorting.DISTANCE_TO_ORIGIN);
            PoseStack stack=new PoseStack();stack.last().pose().set(WORLD_VIEW);
            render(stack);buffers.endBatch();
        }finally{
            //? if >=1.21.1 {
            /*modelView.popMatrix();
            *///?} else {
            modelView.popPose();
            //?}
            RenderSystem.applyModelViewMatrix();RenderSystem.restoreProjectionMatrix();
        }
    }

    public static void activate(LobbyScreen screen){
        if(LobbyCamera.progress()<.95f)return;
        switch(action){
            case -1 -> {if(screen.owner()){screen.invitations=!screen.invitations;LobbySounds.window(screen.invitations);}}
            case -2 -> {
                boolean replacing=screen.dealerOpen;
                screen.rulesOpen=!screen.rulesOpen;screen.dealerOpen=false;
                if(replacing)LobbySounds.toggle();else LobbySounds.window(screen.rulesOpen);
            }
            case -3 -> {screen.playerSettings=!screen.playerSettings;LobbySounds.toggle();}
            case -4 -> {
                boolean replacing=screen.rulesOpen;
                screen.dealerOpen=!screen.dealerOpen;screen.rulesOpen=false;
                if(replacing)LobbySounds.toggle();else LobbySounds.window(screen.dealerOpen);
            }
            case Lobbies.LEAVE -> {LobbySounds.button();screen.onClose();}
            default -> {if(action>=0)screen.sendAction(action,value,target);}
        }
    }
    public static void scroll(LobbyScreen screen,double delta){
        int direction=delta>0?-1:1;
        if(mouseY < -54 || mouseY > 52)return;
        if(mouseX>=-226&&mouseX<=-82)screen.playersScroll=clamp(screen.playersScroll+direction,screen.state().players().size()-3);
        if(screen.invitations&&mouseX>=-380&&mouseX<=-236)screen.nearbyScroll=clamp(screen.nearbyScroll+direction,screen.state().nearby().size()-4);
        if(screen.rulesOpen&&mouseX>=236&&mouseX<=380)screen.rulesScroll=clamp(screen.rulesScroll+direction,screen.state().rules().size()-4);
    }
    private static int clamp(int index,int max){return Math.max(0,Math.min(Math.max(0,max),index));}
    public static void render(PoseStack stack){
        Minecraft mc=Minecraft.getInstance();action=-99;target=NONE;
        if(!(TabletopChatScreen.active() instanceof LobbyScreen screen)||LobbyCamera.orientation()==null)return;
        long now=System.nanoTime();float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1_000_000_000f);lastFrame=now;
        float speed=1-(float)Math.exp(-14*dt);
        inviteReveal+=(screen.invitations&&screen.owner()&&screen.state().capacity()>1?1-inviteReveal:-inviteReveal)*speed;
        rulesReveal+=(screen.rulesOpen||screen.dealerOpen?1-rulesReveal:-rulesReveal)*speed;
        if(screen.dealerOpen)showingDealer=true;
        else if(screen.rulesOpen)showingDealer=false;
        float opening=LobbyCamera.progress();if(opening<.03f)return;alpha=opening;
        var at=LobbyCamera.anchor();var camera=mc.gameRenderer.getMainCamera().getPosition();
        stack.pushPose();stack.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
        stack.mulPose(LobbyCamera.orientation());
        float scale=.0035f*(.78f+.22f*opening);stack.scale(scale,-scale,scale);
        pick(stack);
        panel(stack,-226,-84,144,168);panel(stack,-72,-84,144,168);panel(stack,82,-84,144,168);
        text(stack,tr("tabletop_stuff.lobby.section.players"),-216,-73,WHITE);
        right(stack,screen.state().players().size()+" / "+(screen.state().capacity()==1?screen.state().available():screen.state().capacity()),-92,-72,MUTED);
        players(stack,screen);
        sectionTab(stack,-66,43,tr("tabletop_stuff.lobby.section.places"),!screen.playerSettings);
        sectionTab(stack,-19,85,tr("tabletop_stuff.player.settings"),screen.playerSettings);
        if(screen.playerSettings)playerSettings(stack);else places(stack,screen);
        text(stack,tr("tabletop_stuff.lobby.section.settings"),92,-73,WHITE);
        settings(stack,screen);
        if(inviteReveal>.03f){
            stack.pushPose();stack.translate(12*(1-inviteReveal),0,0);alpha=opening*inviteReveal;
            invitations(stack,screen);stack.popPose();alpha=opening;
        }
        if(rulesReveal>.03f){
            stack.pushPose();stack.translate(-12*(1-rulesReveal),0,0);alpha=opening*rulesReveal;
            if(showingDealer)dealerSettings(stack,screen);else rules(stack,screen);stack.popPose();alpha=opening;
        }
        boolean exit=hit(-226,94,92,22,Lobbies.LEAVE,0,NONE,true);
        boolean solo=screen.state().capacity()==1;
        boolean canStart=screen.owner()&&(solo?screen.state().players().size()>1:screen.state().players().size()==screen.state().capacity())
            &&screen.state().available()>=screen.state().players().size();
        boolean play=hit(134,94,92,22,Lobbies.START,0,NONE,canStart);
        exitHover+=((exit?1:0)-exitHover)*speed;playHover+=((play?1:0)-playHover)*speed;
        footer(stack,tr("tabletop_stuff.lobby.exit"),-216,100,exitHover,true,false);
        footer(stack,tr("tabletop_stuff.lobby.play"),216,100,playHover,canStart,true);
        if(!screen.state().message().isEmpty())center(stack,fit(tr(screen.state().message()),270),0,127,AMBER);
        stack.popPose();
    }
    private static void players(PoseStack stack,LobbyScreen screen){
        var members=screen.state().players();screen.playersScroll=clamp(screen.playersScroll,members.size()-3);
        for(int row=0;row<Math.min(3,members.size()-screen.playersScroll);row++){
            var member=members.get(row+screen.playersScroll);int y=-46+row*29;
            if(member.bot()){
                icon(stack,"player_count",-216,y,16,WHITE);
                text(stack,fit(tr("entity.tabletop_stuff.null_bot",member.name()),32),-193,y+4,WHITE);
                boolean difficultyHover=hit(-159,y-2,48,21,Lobbies.BOT_DIFFICULTY,0,member.id(),screen.owner());
                right(stack,tr(member.difficulty().key()),-112,y+4,difficultyHover?AMBER:WHITE);
            }else{
                face(stack,member.id(),-216,y,16);
                text(stack,fit(member.name(),83),-193,y+4,WHITE);
            }
            if(member.id().equals(screen.state().owner()))rect(stack,-218,y-3,4,4,AMBER,-.4f);
            if(screen.owner()&&!member.id().equals(screen.state().owner())){
                boolean hovered=hit(-110,y-2,19,21,Lobbies.KICK,0,member.id(),true);
                center(stack,"×",-101,y+4,hovered?AMBER:MUTED);
            }
        }
        scrollbar(stack,-88,-46,78,members.size(),3,screen.playersScroll);
        boolean solo=screen.state().capacity()==1;
        boolean canAdd=screen.owner()&&(!solo||members.size()<Math.min(4,screen.state().available()));
        boolean hover=hit(-218,58,125,20,solo?Lobbies.ADD_BOT:-1,0,NONE,canAdd);
        icon(stack,"invite",-216,62,10,canAdd?WHITE:MUTED);
        text(stack,tr(solo?"tabletop_stuff.lobby.add_bot":"tabletop_stuff.lobby.invite"),-200,63,hover||!solo&&screen.invitations?AMBER:canAdd?WHITE:MUTED);
    }
    private static void places(PoseStack stack,LobbyScreen screen){
        // A geometric square, deliberately not an illustration of furniture.
        outline(stack,-26,-25,52,52,0x797B79);
        ResourceLocation game=resource(screen.state().gameIcon());
        texture(stack,game,-11,-16,22,33,0,0,1,1,0xFFFFFF,0);
        int[] xs={-8,40,-8,-56},ys={-49,-7,41,-7};
        for(int seat=0;seat<4;seat++){
            LobbyUpdate.Member occupant=null;for(var p:screen.state().players())if(p.seat()==seat)occupant=p;
            boolean available=(screen.state().availableMask()&(1<<seat))!=0;
            boolean own=occupant!=null&&Minecraft.getInstance().player!=null&&occupant.id().equals(Minecraft.getInstance().player.getUUID());
            boolean hover=hit(xs[seat]-3,ys[seat]-3,22,22,Lobbies.SEAT,seat,NONE,available&&(occupant==null||own));
            if(occupant!=null)face(stack,occupant.id(),xs[seat],ys[seat],16);
            else rect(stack,xs[seat],ys[seat],16,16,available?GREEN:RED,-.1f);
            outline(stack,xs[seat]-1,ys[seat]-1,18,18,occupant!=null?MUTED:available?0xA2D69B:0xE99B93);
            if(hover||own)outline(stack,xs[seat]-2,ys[seat]-2,20,20,AMBER);
        }
    }
    private static void sectionTab(PoseStack stack,int x,int width,String label,boolean selected){
        boolean hover=hit(x,-79,width,22,-3,0,NONE,!selected);
        int color=selected?WHITE:hover?AMBER:MUTED;
        outline(stack,x,-79,width,22,selected?AMBER:hover?AMBER:0x535957);
        if(selected)rect(stack,x+1,-59,width-2,2,AMBER,-.1f);
        stack.pushPose();stack.translate(x+width/2f,-72,0);stack.scale(.7f,.7f,1);
        center(stack,label,0,0,color);stack.popPose();
    }
    private static void playerSettings(PoseStack stack){
        text(stack,tr("tabletop_stuff.player.play_style"),-60,-41,WHITE);
        text(stack,com.wentory.tabletopstuff.PlayerStyleConfig.playStyle(),-60,-24,MUTED);
        text(stack,tr("tabletop_stuff.player.deal_style"),-60,0,WHITE);
        text(stack,com.wentory.tabletopstuff.PlayerStyleConfig.dealStyle(),-60,17,MUTED);
        var player=Minecraft.getInstance().player;if(player!=null)face(stack,player.getUUID(),-12,37,24);
    }
    private static void settings(PoseStack stack,LobbyScreen screen){
        var state=screen.state();
        icon(stack,"player_count",92,-46,12,WHITE);text(stack,tr("tabletop_stuff.lobby.count"),110,-44,WHITE);
        boolean minus=hit(112,-30,25,19,Lobbies.CAPACITY,state.capacity()-1,NONE,screen.owner()&&state.capacity()>Math.max(1,state.players().size()));
        boolean plus=hit(190,-30,25,19,Lobbies.CAPACITY,state.capacity()+1,NONE,screen.owner()&&state.players().stream().noneMatch(LobbyUpdate.Member::bot)&&state.capacity()<Math.min(4,state.available()));
        text(stack,"−",120,-24,minus?AMBER:MUTED);center(stack,Integer.toString(state.capacity()),166,-24,WHITE);text(stack,"+",197,-24,plus?AMBER:MUTED);
        icon(stack,state.inviteOnly()?"access_invite":"access_open",92,-1,12,WHITE);text(stack,tr("tabletop_stuff.lobby.access"),110,0,WHITE);
        boolean access=hit(109,12,110,20,Lobbies.ACCESS,state.inviteOnly()?0:1,NONE,screen.owner());
        text(stack,tr(state.inviteOnly()?"tabletop_stuff.lobby.private":"tabletop_stuff.lobby.public"),110,17,access?AMBER:MUTED);
        icon(stack,"house_rules",92,35,12,WHITE);
        boolean rules=hit(109,30,110,22,-2,0,NONE,true);
        text(stack,tr("tabletop_stuff.lobby.house"),110,37,rules||screen.rulesOpen?AMBER:WHITE);
        boolean dealer=hit(92,59,126,22,-4,0,NONE,true);
        icon(stack,"dealing",92,64,10,WHITE);
        text(stack,tr("tabletop_stuff.dealer.settings")+" ›",110,65,dealer||screen.dealerOpen?AMBER:WHITE);
    }
    private static void dealerSettings(PoseStack stack,LobbyScreen screen){
        panel(stack,236,-84,144,168);text(stack,tr("tabletop_stuff.dealer.settings"),246,-73,WHITE);
        text(stack,tr("tabletop_stuff.dealer.first"),246,-49,MUTED);
        for(int i=0;i<2;i++)dealerOption(stack,screen,248,-31+i*17,Lobbies.FIRST_DEALER,i,screen.state().firstDealer()==i,i==0?"host":"rps");
        text(stack,tr("tabletop_stuff.dealer.next"),246,11,MUTED);
        String loserOption=NullHouseRule.LAST_STANDING.enabled(screen.state().ruleMask())?"last_loser":"loser";
        for(int i=0;i<3;i++)dealerOption(stack,screen,248,29+i*17,Lobbies.NEXT_DEALER,i,screen.state().nextDealer()==i,i==0?"host":i==1?"winner":loserOption);
    }
    private static void dealerOption(PoseStack stack,LobbyScreen screen,int x,int y,int action,int value,boolean selected,String option){
        boolean hover=hit(x-3,y-3,124,16,action,value,NONE,screen.owner()&&screen.dealerOpen);
        outline(stack,x,y,8,8,hover?AMBER:selected?GREEN:MUTED);
        if(selected)rect(stack,x+2,y+2,4,4,GREEN,0);
        text(stack,fit(tr("tabletop_stuff.dealer."+option),108),x+13,y+1,hover?AMBER:selected?WHITE:MUTED);
    }
    private static void invitations(PoseStack stack,LobbyScreen screen){
        panel(stack,-380,-84,144,168);text(stack,tr("tabletop_stuff.lobby.invite"),-370,-73,WHITE);
        var list=screen.state().nearby();screen.nearbyScroll=clamp(screen.nearbyScroll,list.size()-4);
        if(list.isEmpty())text(stack,fit(tr("tabletop_stuff.lobby.nobody"),124),-370,-43,MUTED);
        for(int row=0;row<Math.min(4,list.size()-screen.nearbyScroll);row++){
            var p=list.get(row+screen.nearbyScroll);int y=-46+row*27;
            if(p.invited())rect(stack,-374,y-3,132,23,0x263F2B,-.5f);
            face(stack,p.id(),-370,y,16);text(stack,fit(p.name(),78),-347,y+4,p.invited()?GREEN:WHITE);
            boolean hover=hit(-263,y-2,21,22,p.invited()?Lobbies.CANCEL:Lobbies.INVITE,0,p.id(),screen.owner()&&screen.invitations);
            center(stack,p.invited()?"×":"+",-253,y+4,hover?AMBER:MUTED);
        }
        scrollbar(stack,-240,-46,104,list.size(),4,screen.nearbyScroll);
    }
    private static void rules(PoseStack stack,LobbyScreen screen){
        panel(stack,236,-84,144,168);text(stack,tr("tabletop_stuff.lobby.house"),246,-73,WHITE);
        var state=screen.state();
        var list=state.rules();screen.rulesScroll=clamp(screen.rulesScroll,list.size()-4);
        String description="",blockedBy="";
        for(int row=0;row<Math.min(4,list.size()-screen.rulesScroll);row++){
            int index=row+screen.rulesScroll,y=-46+row*23;boolean checked=(state.ruleMask()&(1<<index))!=0;
            var rule=NullHouseRule.values()[index];
            var conflict=rule.conflict(state.ruleMask());
            boolean check=hit(244,y-3,128,20,Lobbies.RULE,index,NONE,screen.owner()&&screen.rulesOpen&&conflict==null);
            outline(stack,246,y,9,9,check?AMBER:MUTED);if(checked)rect(stack,248,y+2,5,5,GREEN,0);
            String label=fit(tr(list.get(index)),104);
            text(stack,label,262,y+1,conflict!=null?MUTED:check?AMBER:WHITE);
            if(conflict!=null)rect(stack,262,y+5,Minecraft.getInstance().font.width(label),1,MUTED,.1f);
            if(mouseX>=244&&mouseX<=372&&mouseY>=y-3&&mouseY<=y+17){
                description=tr(rule.key()+".description");
                if(conflict!=null)blockedBy=tr("tabletop_stuff.rule.conflict",tr(conflict.key()));
            }
        }
        scrollbar(stack,376,-46,92,list.size(),4,screen.rulesScroll);
        if(!description.isEmpty()){
            var font=Minecraft.getInstance().font;
            var reason=blockedBy.isEmpty()?java.util.List.<net.minecraft.util.FormattedCharSequence>of():font.split(Component.literal(blockedBy),124);
            var lines=font.split(Component.literal(description),124);
            panel(stack,236,89,144,14+(lines.size()+reason.size())*11);
            for(int i=0;i<reason.size();i++){
                font.drawInBatch(reason.get(i),246,96+i*11,AMBER|((int)(alpha*255)<<24),false,
                    stack.last().pose(),LobbyRenderBuffers.UI,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
            }
            for(int i=0;i<lines.size();i++){
                font.drawInBatch(lines.get(i),246,96+(reason.size()+i)*11,WHITE|((int)(alpha*255)<<24),false,
                    stack.last().pose(),LobbyRenderBuffers.UI,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
            }
        }
    }
    private static void pick(PoseStack stack){
        mouseX=mouseY=Float.NaN;Minecraft mc=Minecraft.getInstance();if(!mc.isWindowActive())return;
        INVERSE.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
        if(Math.abs(INVERSE.determinant())<1e-12f)return;INVERSE.invert();
        float x=(float)(2*mc.mouseHandler.xpos()/mc.getWindow().getScreenWidth()-1),y=(float)(1-2*mc.mouseHandler.ypos()/mc.getWindow().getScreenHeight());
        INVERSE.transformProject(NEAR.set(x,y,-1));INVERSE.transformProject(FAR.set(x,y,1));
        float dz=FAR.z-NEAR.z;if(Math.abs(dz)<1e-8f)return;float t=-NEAR.z/dz;
        if(t>=0){mouseX=NEAR.x+(FAR.x-NEAR.x)*t;mouseY=NEAR.y+(FAR.y-NEAR.y)*t;}
    }
    private static boolean hit(float x,float y,float width,float height,int op,int number,UUID id,boolean enabled){
        boolean hover=enabled&&alpha>.95f&&mouseX>=x&&mouseX<=x+width&&mouseY>=y&&mouseY<=y+height;
        if(hover){action=op;value=number;target=id;}return hover;
    }
    private static void scrollbar(PoseStack stack,float x,float y,float height,int count,int rows,int offset){
        if(count<=rows)return;rect(stack,x,y,1,height,0x444640,-.3f);
        float thumb=height*rows/count;rect(stack,x,y+(height-thumb)*offset/(count-rows),1,thumb,AMBER,-.2f);
        if(offset>0)center(stack,"▲",x-2,y-10,AMBER);
        if(offset<count-rows)center(stack,"▼",x-2,y+height+3,AMBER);
    }
    private static void footer(PoseStack stack,String label,int x,int y,float focus,boolean enabled,boolean right){
        Minecraft mc=Minecraft.getInstance();float grow=1+.12f*focus;
        stack.pushPose();stack.translate(x,y,0);stack.scale(grow,grow,grow);
        text(stack,label,right?-mc.font.width(label):0,0,enabled?(focus>.03f?AMBER:WHITE):MUTED);stack.popPose();
    }
    private static void panel(PoseStack stack,int x,int y,int w,int h){
        ResourceLocation image=resource("tabletop_stuff:textures/gui/lobby/panels/section.png");
        // A one-pixel border is preserved rather than stretched with the panel.
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            float dx=col==0?0:col==1?1:w-1,dy=row==0?0:row==1?1:h-1;
            float width=col==1?w-2:1,height=row==1?h-2:1;
            float u0=col==0?0:col==1?1f/16:15f/16,v0=row==0?0:row==1?1f/16:15f/16;
            float u1=col==0?1f/16:col==1?15f/16:1,v1=row==0?1f/16:row==1?15f/16:1;
            texture(stack,image,x+dx,y+dy,width,height,u0,v0,u1,v1,0xFFFFFF,-.9f);
        }
        outline(stack,x,y,w,h,0x767267);
    }
    /** Round results share the lobby's panel, palette and layered player portrait. */
    public static void turnIndicator(PoseStack stack,UUID id,String name,int direction){
        float previousAlpha=alpha;alpha=1;
        try{
            face(stack,id,-12,-32,24);
            outline(stack,-13,-33,26,26,MUTED);
            center(stack,name,0,-2,WHITE);
            icon(stack,direction>0?"clockwise":"counterclockwise",-8,14,16,AMBER);
        }finally{alpha=previousAlpha;}
    }
    public static void winner(PoseStack stack,UUID id,String name,int score,boolean matchOver,boolean lastStanding,String loser){
        float previousAlpha=alpha;alpha=1;
        try{
            panel(stack,-100,-42,200,84);
            center(stack,tr(matchOver?"tabletop_stuff.null.victory_match":"tabletop_stuff.null.victory_round"),0,-30,AMBER);
            face(stack,id,-84,-9,30);outline(stack,-85,-10,32,32,MUTED);
            text(stack,fit(name,132),-43,-7,WHITE);
            if(lastStanding){
                if(!loser.isEmpty())text(stack,fit(tr("tabletop_stuff.null.last_loser",loser),132),-43,11,MUTED);
            }else text(stack,tr("tabletop_stuff.null.victory_score",score),-43,11,MUTED);
        }finally{alpha=previousAlpha;}
    }
    public static void rps(PoseStack stack,com.wentory.tabletopstuff.prototype.PreparationUpdate.Rps state,java.util.List<Integer> order,int hovered,boolean eligible){
        float previousAlpha=alpha;alpha=1;
        try{
            panel(stack,-78,-60,156,88);center(stack,tr("tabletop_stuff.rps.title"),0,-53,WHITE);
            center(stack,tr("tabletop_stuff.rps.ready",state.submitted(),state.players().size()),0,-41,MUTED);
            for(int slot=0;slot<3;slot++){
                int choice=order.get(slot),x=(slot-1)*44;boolean selected=state.ownChoice()==choice;
                rect(stack,x-17,-28,34,34,selected?0x263F2B:0x202226,-.1f);
                outline(stack,x-17,-28,34,34,hovered==choice?AMBER:selected?GREEN:MUTED);
                icon(stack,rpsIcon(choice),x-8,-19,16,eligible||selected?WHITE:MUTED);
            }
            int sign=hovered>=0?hovered:state.ownChoice();
            if(sign>=0)center(stack,tr("tabletop_stuff.rps.sign."+sign),0,12,MUTED);
        }finally{alpha=previousAlpha;}
    }
    public static void revealedRpsChoice(PoseStack stack,int choice){
        float previousAlpha=alpha;alpha=1;
        try{
            panel(stack,-14,-14,28,28);
            icon(stack,rpsIcon(choice),-8,-8,16,WHITE);
        }finally{alpha=previousAlpha;}
    }
    private static String rpsIcon(int choice){
        return switch(choice){
            case 0 -> "rock";
            case 1 -> "paper";
            case 2 -> "scissors";
            default -> throw new IllegalArgumentException("Invalid RPS choice: "+choice);
        };
    }
    public static void cancellationVote(PoseStack stack,com.wentory.tabletopstuff.prototype.PreparationUpdate.Vote vote,int hovered,boolean eligible){
        float previousAlpha=alpha;alpha=1;
        try{
            panel(stack,-104,-48,208,96);
            center(stack,tr("tabletop_stuff.vote.title"),0,-36,WHITE);
            center(stack,tr("tabletop_stuff.vote.counts",vote.yes(),vote.no(),vote.total(),vote.seconds()),0,-16,MUTED);
            for(int choice=0;choice<=1;choice++){
                int x=choice==1?-92:8;boolean selected=vote.ownChoice()==choice;
                rect(stack,x,9,84,23,selected?0x343729:0x202629,-.1f);
                outline(stack,x,9,84,23,hovered==choice?AMBER:selected?AMBER:0x636C68);
                center(stack,tr(choice==1?"tabletop_stuff.vote.yes":"tabletop_stuff.vote.no"),x+42,17,hovered==choice||selected?AMBER:eligible?WHITE:MUTED);
            }
        }finally{alpha=previousAlpha;}
    }
    private static void icon(PoseStack stack,String name,int x,int y,int size,int color){texture(stack,resource("tabletop_stuff:textures/gui/lobby/icons/"+name+".png"),x,y,size,size,0,0,1,1,color,0);}
    private static void face(PoseStack stack,UUID id,int x,int y,int size){
        Minecraft mc=Minecraft.getInstance();
        var connection=mc.getConnection();var info=connection==null?null:connection.getPlayerInfo(id);
        boolean local=mc.player!=null&&mc.player.getUUID().equals(id);
        ResourceLocation skin;
        //? if >=1.21.1 {
        /*skin=local?mc.player.getSkin().texture():info==null?resource("minecraft:textures/entity/player/wide/steve.png"):info.getSkin().texture();
        *///?} else {
        skin=local?mc.player.getSkinTextureLocation():info==null?resource("minecraft:textures/entity/player/wide/steve.png"):info.getSkinLocation();
        //?}
        if(info==null&&!local&&mc.level!=null)for(var entity:mc.level.entitiesForRendering()){
            if(entity instanceof com.wentory.tabletopstuff.prototype.TestPlayer test&&entity.getUUID().equals(id)
                &&mc.getEntityRenderDispatcher().getRenderer(test) instanceof TestPlayerRenderer renderer){skin=renderer.getTextureLocation(test);break;}
        }
        texture(stack,skin,x,y,size,size,8f/64,8f/64,16f/64,16f/64,0xFFFFFF,0);
        texture(stack,skin,x,y,size,size,40f/64,8f/64,48f/64,16f/64,0xFFFFFF,.05f);
    }
    private static void outline(PoseStack stack,float x,float y,float w,float h,int color){rect(stack,x,y,w,1,color,-.2f);rect(stack,x,y+h-1,w,1,color,-.2f);rect(stack,x,y,1,h,color,-.2f);rect(stack,x+w-1,y,1,h,color,-.2f);}
    private static void rect(PoseStack stack,float x,float y,float w,float h,int color,float depth){
        // Match the textured UI pipeline, which Iris handles consistently. The
        // untextured lightmap-only background pipeline loses these quads in shaders.
        texture(stack,resource("tabletop_stuff:textures/gui/lobby/panels/pixel.png"),x,y,w,h,0,0,1,1,color,depth);
    }
    private static void texture(PoseStack stack,ResourceLocation image,float x,float y,float w,float h,float u0,float v0,float u1,float v1,int color,float depth){
        var out=LobbyRenderBuffers.UI.getBuffer(RenderType.textSeeThrough(image));
        vertex(stack,out,x,y,depth,color,u0,v0,true);vertex(stack,out,x,y+h,depth,color,u0,v1,true);
        vertex(stack,out,x+w,y+h,depth,color,u1,v1,true);vertex(stack,out,x+w,y,depth,color,u1,v0,true);
    }
    private static void vertex(PoseStack stack,VertexConsumer out,float x,float y,float z,int color,float u,float v,boolean textured){
        int a=Math.max(0,Math.min(255,(int)(255*alpha)));
        //? if >=1.21.1 {
        /*out.addVertex(stack.last().pose(),x,y,z).setColor(color>>16&255,color>>8&255,color&255,a);
        if(textured)out.setUv(u,v);out.setLight(0xF000F0);
        *///?} else {
        out.vertex(stack.last().pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,a);
        if(textured)out.uv(u,v);out.uv2(0xF000F0).endVertex();
        //?}
    }
    private static void text(PoseStack stack,String label,float x,float y,int color){
        int a=Math.max(4,Math.min(255,(int)(255*alpha)));
        Minecraft mc=Minecraft.getInstance();mc.font.drawInBatch(label,x,y,color|(a<<24),false,stack.last().pose(),LobbyRenderBuffers.UI,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
    }
    private static void center(PoseStack stack,String label,float x,float y,int color){text(stack,label,x-Minecraft.getInstance().font.width(label)/2f,y,color);}
    private static void right(PoseStack stack,String label,int x,int y,int color){text(stack,label,x-Minecraft.getInstance().font.width(label),y,color);}
    private static String fit(String label,int width){return Minecraft.getInstance().font.plainSubstrByWidth(label,width);}
    private static String tr(String key,Object... args){return Component.translatable(key,args).getString();}
    private static ResourceLocation resource(String value){return RESOURCES.computeIfAbsent(value,key->{
        //? if >=1.21.1 {
        /*return ResourceLocation.parse(key);
        *///?} else {
        return new ResourceLocation(key);
        //?}
    });}
}
