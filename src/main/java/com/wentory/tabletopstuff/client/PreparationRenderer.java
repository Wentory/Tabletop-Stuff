package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.wentory.tabletopstuff.prototype.*;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-side trajectories and scatter, no card entities or client-side deck order. */
public final class PreparationRenderer {
    private static final Matrix4f INVERSE=new Matrix4f();
    private static final Vector3f NEAR=new Vector3f(),FAR=new Vector3f();
    private static int hover,hoverValue;
    private static UUID target=new UUID(0,0);
    private static boolean deckHover;
    private static String tooltip="";
    private static int tooltipColor=-1;
    private record WorldText(String text,float x,int color,Matrix4f pose){}
    private static final List<WorldText> TEXT=new ArrayList<>();
    private static final Matrix4f WORLD_PROJECTION=new Matrix4f(), WORLD_VIEW=new Matrix4f();
    private record ColorSquare(Matrix4f pose,int color,float half,int outline){}
    private static final List<ColorSquare> COLOR_SQUARES=new ArrayList<>();
    private record Winner(UUID id,String name,int score,boolean matchOver,boolean lastStanding,String loser,Matrix4f pose){}
    private static Winner winner;
    private record Turn(UUID id,String name,int direction,Matrix4f pose){}
    private static Turn turn;
    private record VotePanel(PreparationUpdate.Vote state,int hovered,boolean eligible,Matrix4f pose){}
    private static VotePanel votePanel;
    private record RpsPanel(PreparationUpdate.Rps state,List<Integer> order,int hovered,boolean eligible,Matrix4f pose){}
    private record RpsChoice(int sign,Matrix4f pose){}
    private static final List<RpsChoice> RPS_CHOICES=new ArrayList<>();
    private static RpsPanel rpsPanel;
    public static String tooltip(){return tooltip;}
    public static int tooltipColor(){return tooltipColor;}
    public static boolean hasTarget(){return hover!=0||deckHover&&PreparationClient.local()!=null&&PreparationClient.local().update.phase()==2;}
    private static void hint(String text,boolean enabled){tooltip=text;tooltipColor=enabled?0xFFFFE25C:0xFF999999;}
    private static final net.minecraft.resources.ResourceLocation PIXEL=resource();
    private static net.minecraft.resources.ResourceLocation resource(){
        //? if >=1.21.1 {
        /*return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tabletop_stuff","textures/gui/lobby/panels/pixel.png");
        *///?} else {
        return new net.minecraft.resources.ResourceLocation("tabletop_stuff","textures/gui/lobby/panels/pixel.png");
        //?}
    }
    public static void clear(){DeckRenderer.clearOutlines();hover=0;deckHover=false;tooltip="";TEXT.clear();RPS_CHOICES.clear();COLOR_SQUARES.clear();winner=null;rpsPanel=null;turn=null;votePanel=null;}
    /** Flush world geometry first; text must be drawn after the complete world, not between its batches. */
    public static void overlay(){
        DeckRenderer.overlay();
        if(TEXT.isEmpty()&&RPS_CHOICES.isEmpty()&&COLOR_SQUARES.isEmpty()&&winner==null&&rpsPanel==null&&turn==null&&votePanel==null)return;
        var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();buffers.endBatch();
        RenderSystem.backupProjectionMatrix();var view=RenderSystem.getModelViewStack();
        //? if >=1.21.1 {
        /*view.pushMatrix();view.identity();
        *///?} else {
        view.pushPose();view.setIdentity();
        //?}
        RenderSystem.applyModelViewMatrix();
        try{
            RenderSystem.setProjectionMatrix(WORLD_PROJECTION,VertexSorting.DISTANCE_TO_ORIGIN);
            for(var square:COLOR_SQUARES){
                PoseStack stack=new PoseStack();stack.last().pose().set(square.pose());
                drawColorSquare(stack,square.color(),square.half(),square.outline());
            }
            if(winner!=null){
                PoseStack stack=new PoseStack();stack.last().pose().set(winner.pose());
                LobbyWorldMenu.winner(stack,winner.id(),winner.name(),winner.score(),winner.matchOver(),winner.lastStanding(),winner.loser());
            }
            if(rpsPanel!=null){
                PoseStack stack=new PoseStack();stack.last().pose().set(rpsPanel.pose());
                LobbyWorldMenu.rps(stack,rpsPanel.state(),rpsPanel.order(),rpsPanel.hovered(),rpsPanel.eligible());
            }
            for(var choice:RPS_CHOICES){
                PoseStack stack=new PoseStack();stack.last().pose().set(choice.pose());
                LobbyWorldMenu.revealedRpsChoice(stack,choice.sign());
            }
            if(turn!=null){
                PoseStack stack=new PoseStack();stack.last().pose().set(turn.pose());
                LobbyWorldMenu.turnIndicator(stack,turn.id(),turn.name(),turn.direction());
            }
            if(votePanel!=null){PoseStack stack=new PoseStack();stack.last().pose().set(votePanel.pose());LobbyWorldMenu.cancellationVote(stack,votePanel.state(),votePanel.hovered(),votePanel.eligible());}
            for(var text:TEXT)mc.font.drawInBatch(text.text(),text.x(),0,text.color(),false,text.pose(),LobbyRenderBuffers.UI,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
            buffers.endBatch();
        }finally{
            //? if >=1.21.1 {
            /*view.popMatrix();
            *///?} else {
            view.popPose();
            //?}
            RenderSystem.applyModelViewMatrix();RenderSystem.restoreProjectionMatrix();TEXT.clear();RPS_CHOICES.clear();COLOR_SQUARES.clear();winner=null;rpsPanel=null;turn=null;votePanel=null;
        }
    }
    private static Quaternionf billboardRotation(){
        Quaternionf rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        //? if >=1.21.1 {
        /*return rotation;
        *///?} else {
        // The 1.20.1 camera quaternion looks along +Z. The view's billboards face -Z.
        return new Quaternionf(rotation).rotateY((float)Math.PI);
        //?}
    }
    private static Matrix4f overlayPose(PoseStack stack){
        return new Matrix4f(WORLD_VIEW).mul(stack.last().pose());
    }
    private static void text(PoseStack root,String value,float x,int color){
        TEXT.add(new WorldText(value,x,color,overlayPose(root)));
    }
    public static void click(){
        if(hover==PreparationUpdate.CANCEL_VOTE){PrototypeNetwork.lobbyAction(PreparationUpdate.CANCEL_VOTE,hoverValue);return;}
        var local=PreparationClient.local();if(local==null||local.update.phase()<2)return;
        var player=Minecraft.getInstance().player;if(player!=null&&local.take(player.getUUID())<1)return;
        if(player!=null&&local.play(player.getUUID())!=null)return;
        if(hover==PreparationUpdate.PLAY&&CardHover.selected()>=0)PrototypeNetwork.lobbyAction(PreparationUpdate.PLAY,CardHover.selected());
        else if(deckHover&&local.update.phase()==2&&local.update.rules().dealer().equals(player.getUUID()))PrototypeNetwork.lobbyAction(PreparationUpdate.SHUFFLE,0);
        else if(hover!=0)PrototypeNetwork.lobbyAction(hover,hoverValue,target);
    }
    public static Vec3 pile(PreparationClient.State state,int seat){
        double x=seat==1?.84:seat==3?.16:.5,z=seat==0?.16:seat==2?.84:.5;
        var pos=state.update.table();var mc=Minecraft.getInstance();double lift=0;
        if(mc.level!=null&&mc.level.getBlockState(pos).getBlock() instanceof LobbyBlock)lift=LobbyBlock.surfaceHeight(mc.level.getBlockState(pos));
        return new Vec3(pos.getX()+x,pos.getY()+lift+.003,pos.getZ()+z);
    }
    public static Vec3 deck(PreparationClient.State state,boolean corner){
        var p=state.update.table();Vec3 center=new Vec3(p.getX()+.5,p.getY()+.003,p.getZ()+.5);
        var mc=Minecraft.getInstance();if(mc.level!=null&&mc.level.getBlockState(p).getBlock() instanceof LobbyBlock)center=center.add(0,LobbyBlock.surfaceHeight(mc.level.getBlockState(p)),0);
        if(!corner)return center;
        var owner=state.actor(state.update.rules().dealer());Vec3 forward=center.subtract(owner.feet()).multiply(1,0,1).normalize();Vec3 right=forward.cross(new Vec3(0,1,0));
        return center.add(right.scale(.28)).subtract(forward.scale(.28));
    }
    public static float holdAmount(LivingEntity entity){
        var state=PreparationClient.of(entity);if(state==null||!state.update.rules().dealer().equals(entity.getUUID()))return 0;
        return switch(state.update.phase()){case 1->smooth(state.phase(PreparationMotion.PHASE));case 2->1;case 3->1-smooth(state.phase(PreparationMotion.PHASE));default->0;};
    }
    public static boolean resting(LivingEntity entity){return PreparationClient.of(entity)!=null&&PreparationClient.of(entity).play(entity.getUUID())==null&&PrototypeClient.hand(entity).isEmpty()&&!PreparationClient.dealerHolding(entity);}
    public static void pose(PlayerModel<?> model,LivingEntity entity){
        var state=PreparationClient.of(entity);if(state==null)return;
        PreparationMotion.pose(model,entity,state);
        model.leftSleeve.copyFrom(model.leftArm);model.rightSleeve.copyFrom(model.rightArm);
    }
    public static void held(PlayerModel<?> model,PoseStack root,MultiBufferSource buffers,int light,LivingEntity entity,float palmX,float palmY,float palmZ,float breathing){
        var state=PreparationClient.of(entity);if(state==null)return;
        boolean holding=PreparationClient.dealerHolding(entity);float take=state.take(entity.getUUID());
        if(!holding){
            if(take<1)pickupCards(model,root,buffers,light,entity,palmX,palmY,palmZ,breathing,state);
            return;
        }
        var actor=state.actor(entity.getUUID());
        Matrix4f local=holding?PreparationMotion.deck(state,actor):PreparationMotion.taken(state,actor);
        float breathWeight=holding?(state.update.phase()==1?PreparationMotion.smooth(state.phase(PickupAnimation.LENGTH)*PickupAnimation.LENGTH/.5f):state.update.phase()==3?1-PreparationMotion.smooth((state.phase(PreparationMotion.PHASE)*PreparationMotion.PHASE-1.25f)/.25f):1):1;
        PoseStack draw=new PoseStack();draw.last().pose().set(root.last().pose()).translate(0,breathing*breathWeight/16,0).mul(local).scale(1f/(16*.9375f));
        draw.last().pose().normal(draw.last().normal());
        if(holding&&entity==Minecraft.getInstance().player){
            deckHover=hitBox(draw,VisualPose.CARD_WIDTH/2,-DeckRenderer.height(state.update.remaining()),VisualPose.CARD_HEIGHT/2,true);
            if(deckHover&&state.update.phase()==2)DeckRenderer.outline(draw,buffers,state.update.remaining(),PIXEL,0xFFFFE25C);
        }
        if(holding&&state.update.action()==PreparationUpdate.SHUFFLE&&state.action(PreparationMotion.SHUFFLE)<1){
            int count=state.update.remaining(),packet=Math.min(count,state.update.packetSize());
            int above=Math.min(count-packet,state.update.insertionDepth()),below=count-packet-above;
            float t=state.action(PreparationMotion.SHUFFLE);
            float lowered=MotionCurves.ease((t-.4f)/.3f);
            float gap=DeckRenderer.height(packet);
            DeckRenderer.render(draw,buffers,light,below);
            draw.pushPose();draw.translate(0,-DeckRenderer.height(below)-gap*lowered,0);DeckRenderer.render(draw,buffers,light,above);draw.popPose();
            draw.pushPose();draw.last().pose().mul(PreparationMotion.shufflePacket(count,packet,above,t));
            draw.last().pose().normal(draw.last().normal());
            DeckRenderer.render(draw,buffers,light,packet);draw.popPose();
        }else DeckRenderer.render(draw,buffers,light,holding?state.update.remaining():state.actor(entity.getUUID()).taken());
    }

    private static Matrix4f fanCard(Matrix4f fan,com.wentory.tabletopstuff.prototype.HandLayout.Card card){
        return new Matrix4f(fan).translate(card.x(),card.y(),card.z()).rotateZ(card.roll()).translate(0,com.wentory.tabletopstuff.prototype.HandLayout.GRIP_INSET,0);
    }
    private static void pickupCards(PlayerModel<?> model,PoseStack root,MultiBufferSource buffers,int light,LivingEntity entity,
        float palmX,float palmY,float palmZ,float breathing,PreparationClient.State state){
        var id=entity.getUUID();var actor=state.actor(id);float seconds=state.take(id)*PreparationMotion.TAKE;
        float opening=MotionCurves.ease((seconds-1.05f)/(PreparationMotion.TAKE-1.05f));
        var old=actor.awayState()==AwayState.LEAVING?List.<HandLayout.Card>of():state.priorHands.getOrDefault(id,List.of());var all=state.hands.getOrDefault(id,List.of());
        float hidden=PrototypeClient.hiddenAmount(entity);var grip=VisualPose.GRIP;var tucked=VisualPose.HIDDEN_GRIP;
        Matrix4f fan=new Matrix4f().translate(model.leftArm.x/16,model.leftArm.y/16,model.leftArm.z/16).rotateZYX(model.leftArm.zRot,model.leftArm.yRot,model.leftArm.xRot)
            .translate((palmX+grip.x()+(tucked.x()-grip.x())*hidden)/16,(palmY+grip.y()+(tucked.y()-grip.y())*hidden)/16,(palmZ+grip.z()+(tucked.z()-grip.z())*hidden)/16)
            .rotateZYX(grip.roll()+(tucked.roll()-grip.roll())*hidden,grip.yaw()+(tucked.yaw()-grip.yaw())*hidden,grip.pitch()+(tucked.pitch()-grip.pitch())*hidden).scale(1f/16);
        Matrix4f carried=null;
        if(seconds>=TableAnimations.CONTACT)carried=new Matrix4f().translate(0,breathing/16,0).mul(PreparationMotion.taken(state,actor)).scale(1f/(16*.9375f));
        float opacity=entity==Minecraft.getInstance().player?1-.8f*hidden:1;
        for(int i=0;i<all.size();i++){
            Matrix4f destination=fanCard(fan,all.get(i)),from;
            if(i<old.size())from=fanCard(fan,old.get(i));
            else{
                if(carried==null)continue;
                from=new Matrix4f(carried).translate(0,-DeckRenderer.height(i-old.size()+1),-VisualPose.CARD_HEIGHT/2).rotateX(-(float)Math.PI/2);
            }
            Matrix4f card=PreparationMotion.cardBlend(from,destination,opening);
            root.pushPose();root.last().pose().mul(card);root.last().pose().normal(root.last().normal());
            CardRenderer.single(root,buffers,light,actor.faces()[i],opacity);root.popPose();
        }
    }

    public static void playHand(PlayerModel<?> model,PoseStack root,MultiBufferSource buffers,int light,LivingEntity entity,
        float palmX,float palmY,float palmZ,PreparationClient.State state){
        var play=state.play(entity.getUUID());if(play==null)return;float t=play.progress();
        var grip=VisualPose.GRIP;
        Matrix4f fan=new Matrix4f().translate(model.leftArm.x/16,model.leftArm.y/16,model.leftArm.z/16).rotateZYX(model.leftArm.zRot,model.leftArm.yRot,model.leftArm.xRot)
            .translate((palmX+grip.x())/16,(palmY+grip.y())/16,(palmZ+grip.z())/16).rotateZYX(grip.roll(),grip.yaw(),grip.pitch()).scale(1f/16);
        var after=state.hands.getOrDefault(entity.getUUID(),List.of());
        float close=PreparationMotion.smooth((t-PlayMotion.GRAB)/(PlayMotion.RELEASE-PlayMotion.GRAB));
        for(int i=0;i<play.before.size();i++){
            if(i==play.card.index()&&t>=PlayMotion.GRAB)continue;
            Matrix4f from=fanCard(fan,play.before.get(i)),card=from;
            if(i==play.card.index())card.translate(0,-play.lift*(1-PreparationMotion.smooth(t/PlayMotion.GRAB)),0);
            else{int next=i<play.card.index()?i:i-1;if(next<after.size())card=PreparationMotion.cardBlend(from,fanCard(fan,after.get(next)),close);}
            int face=i==play.card.index()?play.card.face():play.faces[i];
            root.pushPose();root.last().pose().mul(card);root.last().pose().normal(root.last().normal());CardRenderer.single(root,buffers,light,face,1);root.popPose();
        }
    }
    private static void playedCards(PoseStack root,MultiBufferSource buffers,int light,PreparationClient.State state,Vec3 camera){
        Vec3 center=deck(state,false);
        for(var card:state.update.discarded()){
            Matrix4f landing=PlayMotion.landing(card,new Vector3f((float)center.x,(float)center.y,(float)center.z)),frame=landing;
            var play=state.play(card.player());
            if(play!=null&&play.card.id()==card.id()){
                float t=play.progress();if(t<PlayMotion.GRAB)continue;
                var actor=state.actor(card.player());Matrix4f source=PlayMotion.source(play.before.get(card.index()),PreparationMotion.floorHandOffset(state,actor));
                Vector3f target=PreparationMotion.table(state,actor,center.add(card.x(),card.id()*PlayMotion.STACK_STEP,card.z())).getTranslation(new Vector3f());
                Matrix4f carried=PlayMotion.carried(source,target,t);
                frame=new Matrix4f().translation((float)actor.feet().x,(float)actor.feet().y,(float)actor.feet().z).mul(PreparationMotion.body(actor)).mul(carried);
                if(t>=PlayMotion.RELEASE)frame=PlayMotion.flight(frame,landing,(t-PlayMotion.RELEASE)/(1-PlayMotion.RELEASE));
            }
            root.pushPose();root.translate(-camera.x,-camera.y,-camera.z);root.last().pose().mul(frame);root.last().pose().normal(root.last().normal());CardRenderer.single(root,buffers,light,card.face(),1);root.popPose();
        }
    }

    public static void render(PoseStack root,Matrix4f worldProjection){
        WORLD_PROJECTION.set(worldProjection);
        // NeoForge applies camera rotation through the render system; Forge
        // carries it in the pose stack. Capture both before entering GUI space.
        WORLD_VIEW.set(RenderSystem.getModelViewMatrix());
        Minecraft mc=Minecraft.getInstance();TEXT.clear();RPS_CHOICES.clear();COLOR_SQUARES.clear();winner=null;rpsPanel=null;turn=null;votePanel=null;hover=0;hoverValue=0;tooltip="";target=new UUID(0,0);if(mc.level==null)return;
        var buffers=mc.renderBuffers().bufferSource();var camera=mc.gameRenderer.getMainCamera().getPosition();var local=PrototypeClient.controlling()?PreparationClient.local():null;
        for(var state:PreparationClient.games()){
            absenceTimers(root,buffers,state,camera);
            if(state.update.phase()==0)continue;
            int light=net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level,state.update.table());
            playedCards(root,buffers,light,state,camera);
            chosenColor(root,state,camera);
            lastCall(root,buffers,state,camera);
            if(state.update.rps().active()){
                if(local==state&&state.update.vote().active())vote(root,state,camera,true);
                else if(state.update.rps().revealed())revealRps(root,buffers,state,camera);
                else if(local==state)rps(root,state,camera);
                continue;
            }
            if(state.update.phase()==4){
                root.pushPose();Vec3 at=deck(state,true).subtract(camera);root.translate(at.x,at.y,at.z);root.mulPose(Axis.YP.rotationDegrees(180-state.actor(state.update.rules().dealer()).yaw()));
                int remaining=state.update.remaining();
                root.scale(-1f/16,-1f/16,1f/16);
                if(local==state){
                    var actor=state.actor(Minecraft.getInstance().player.getUUID());boolean available=actor.canDraw();
                    boolean needsDraw=available&&state.update.rules().turn().equals(actor.id())&&actor.playable().length==0&&!actor.hidden();
                    boolean inside=remaining>0&&hitBox(root,VisualPose.CARD_WIDTH/2,
                        -DeckRenderer.height(remaining),VisualPose.CARD_HEIGHT/2,false);
                    if(inside){if(available)hover=PreparationUpdate.DRAW;hint(tr("draw"),available);}
                    if(inside||needsDraw){
                        DeckRenderer.outline(root,buffers,remaining,PIXEL,available?0xFFFFE25C:0xFF999999,needsDraw);
                    }
                }
                DeckRenderer.render(root,buffers,light,remaining);root.popPose();
            }
            for(var actor:state.update.actors()){
                Vec3 at=pile(state,actor.seat()).subtract(camera);root.pushPose();root.translate(at.x,at.y,at.z);
                int away=actor.awayState();
                boolean tableHand=away==AwayState.AWAY||away==AwayState.RETURN_FADE
                    ||away==AwayState.LEAVING&&state.take(actor.id())*PreparationMotion.TAKE<TableAnimations.CONTACT
                    ||away==AwayState.PICKUP&&state.take(actor.id())*PreparationMotion.TAKE<TableAnimations.CONTACT;
                if(tableHand)awayCards(root,buffers,light,actor);
                float[] pointer=plane(root);boolean inside=pointer!=null&&Math.abs(pointer[0])<.125&&Math.abs(pointer[1])<.15;
                if(local==state&&inside&&mc.player!=null){
                    if(actor.id().equals(Minecraft.getInstance().player.getUUID())&&actor.pile().length>0&&(!actor.id().equals(state.update.rules().dealer())||state.update.phase()==4)){hover=PreparationUpdate.TAKE;target=actor.id();hint(tr(actor.pile().length==1?"take_one":"take_many"),true);}
                    else if(state.update.rules().dealer().equals(Minecraft.getInstance().player.getUUID())&&state.update.phase()==2&&actor.awayState()!=AwayState.RETIRED&&actor.delivered()<7){hover=PreparationUpdate.DEAL;target=actor.id();hint(tr("deal_to",name(actor.id())),true);}
                }
                boolean dealerHints=actor.awayState()!=AwayState.RETIRED&&local==state&&state.update.phase()<=2&&state.update.rules().dealer().equals(Minecraft.getInstance().player.getUUID());
                boolean ownHints=local==state&&actor.id().equals(Minecraft.getInstance().player.getUUID())&&(actor.delivered()<7||actor.pile().length>0);
                boolean hints=dealerHints&&actor.delivered()<7||ownHints;
                if(hints)target(root,buffers,inside?0xEACCAC55:0xAA6CAAAD);
                if(actor.awayState()==AwayState.ACTIVE&&!actor.fromDeck()&&state.take(actor.id())*PreparationMotion.TAKE<TableAnimations.CONTACT){
                    for(int index:state.takenIndices.getOrDefault(actor.id(),new int[0])){
                        root.pushPose();root.translate(scatter(actor.id(),index,0)*.035,.002+index*.0002,scatter(actor.id(),index,1)*.035);root.mulPose(Axis.YP.rotationDegrees(state.actor(state.update.rules().dealer()).yaw()+scatter(actor.id(),index,2)*19));
                        root.scale(-1f/16,-1f/16,1f/16);DeckRenderer.card(root,buffers,light);root.popPose();
                    }
                }
                if(actor.fromDeck()&&state.take(actor.id())*PreparationMotion.TAKE<TableAnimations.CONTACT){
                    root.pushPose();Vec3 draw=deck(state,true).add(0,DeckRenderer.height(state.update.remaining())/16,0).subtract(pile(state,actor.seat()));
                    root.translate(draw.x,draw.y,draw.z);root.mulPose(Axis.YP.rotationDegrees(180-state.actor(state.update.rules().dealer()).yaw()));root.scale(-1f/16,-1f/16,1f/16);DeckRenderer.card(root,buffers,light);root.popPose();
                }
                for(int index:actor.pile()){
                    if(state.update.action()==PreparationUpdate.DEAL&&state.update.target().equals(actor.id())&&index==actor.delivered()-1&&state.action(PreparationMotion.DEAL)<1)continue;
                    root.pushPose();root.translate(scatter(actor.id(),index,0)*.035,.002+index*.0002,scatter(actor.id(),index,1)*.035);root.mulPose(Axis.YP.rotationDegrees(state.actor(state.update.rules().dealer()).yaw()+scatter(actor.id(),index,2)*19));root.scale(-1f/16,-1f/16,1f/16);DeckRenderer.card(root,buffers,light);root.popPose();
                }
                if(hints){root.pushPose();root.translate(0,.12,0);root.mulPose(billboardRotation());root.scale(.008f,-.008f,.008f);
                String count=actor.delivered()+"/7";text(root,count,-mc.font.width(count)/2f,-1);root.popPose();}root.popPose();
            }
            if(state.update.action()==PreparationUpdate.DEAL&&state.action(PreparationMotion.DEAL)<1){
                var actor=state.actor(state.update.target());if(actor!=null){
                    float action=state.action(PreparationMotion.DEAL);
                    var owner=state.actor(state.update.rules().dealer());
                    Matrix4f tracked=PreparationMotion.deal(state,owner);
                    Vec3 start=PreparationMotion.world(owner,tracked);
                    if(action<PreparationMotion.DEAL_RELEASE){
                        root.pushPose();root.translate(owner.feet().x-camera.x,owner.feet().y-camera.y,owner.feet().z-camera.z);
                        root.last().pose().mul(PreparationMotion.body(owner).mul(tracked));root.last().pose().normal(root.last().normal());root.scale(1f/(16*.9375f),1f/(16*.9375f),1f/(16*.9375f));
                        DeckRenderer.card(root,buffers,light);root.popPose();
                    }else{
                        float t=(action-PreparationMotion.DEAL_RELEASE)/(1-PreparationMotion.DEAL_RELEASE);
                        Vec3 end=pile(state,actor.seat()).add(scatter(actor.id(),actor.delivered()-1,0)*.035,.002+(actor.delivered()-1)*.0002,scatter(actor.id(),actor.delivered()-1,1)*.035);
                        Matrix4f release=PreparationMotion.body(owner).mul(tracked);
                        release.setTranslation(release.getTranslation(new Vector3f()).add((float)owner.feet().x,(float)owner.feet().y,(float)owner.feet().z));
                        Matrix4f landing=new Matrix4f().translation((float)end.x,(float)end.y,(float)end.z).rotateY((float)Math.toRadians(owner.yaw()+scatter(actor.id(),actor.delivered()-1,2)*19)).rotateZ((float)Math.PI);
                        Matrix4f frame=PreparationMotion.flight(release,landing,t);
                        root.pushPose();root.translate(-camera.x,-camera.y,-camera.z);root.last().pose().mul(frame);root.last().pose().normal(root.last().normal());root.scale(1f/16,1f/16,1f/16);DeckRenderer.card(root,buffers,light);root.popPose();
                    }
                }
            }
            if(local==state&&state.update.phase()==4&&CardHover.selected()>=0&&PrototypeClient.canPlayCard(CardHover.selected())&&state.play(Minecraft.getInstance().player.getUUID())==null){
                root.pushPose();Vec3 center=deck(state,false).add(0,state.update.discarded().size()*PlayMotion.STACK_STEP,0).subtract(camera);root.translate(center.x,center.y,center.z);
                float[] pointer=plane(root);if(pointer!=null&&Math.abs(pointer[0])<.18&&Math.abs(pointer[1])<.18&&hover==0){hover=PreparationUpdate.PLAY;hint(tr("play_card"),true);}
                target(root,buffers,0x99FFD400);root.popPose();
            }
            if(local==state&&state.update.phase()==4)rules(root,buffers,state,camera);
            if(local==state&&state.update.rules().dealer().equals(Minecraft.getInstance().player.getUUID())&&state.update.phase()==2&&state.update.actors().stream().filter(a->a.awayState()!=AwayState.RETIRED).allMatch(a->a.delivered()==7)){
                root.pushPose();Vec3 center=deck(state,true).subtract(camera);root.translate(center.x,center.y,center.z);root.mulPose(Axis.YP.rotationDegrees(180-state.actor(state.update.rules().dealer()).yaw()));float[] p=plane(root);
                boolean inside=p!=null&&Math.abs(p[0])<.13&&Math.abs(p[1])<.17;
                rectangle(root,LobbyRenderBuffers.UI,inside?0xFFFFE25C:0xCCB7B6AC,.13f,.17f,.006f);
                if(inside&&hover==0){hover=PreparationUpdate.PLACE;hint(tr("put_deck"),true);}root.popPose();
            }
            if(state.update.vote().active())vote(root,state,camera,local==state);
        }
        if(local!=null&&local.update.phase()==2&&local.update.rules().dealer().equals(Minecraft.getInstance().player.getUUID())&&deckHover)hint(tr("shuffle"),true);
    }
    private static String tr(String key,Object... args){return net.minecraft.network.chat.Component.translatable("tabletop_stuff.null."+key,args).getString();}
    private static String name(UUID id){var level=Minecraft.getInstance().level;var entity=level==null?null:level.getPlayerByUUID(id);if(entity!=null)return entity.getDisplayName().getString();if(level!=null)for(var e:level.entitiesForRendering())if(e.getUUID().equals(id))return e.getDisplayName().getString();for(var game:PreparationClient.games()){var actor=game.actor(id);if(actor!=null&&!actor.name().isEmpty())return actor.name();}return "?";}
    private static void absenceTimers(PoseStack root,MultiBufferSource buffers,PreparationClient.State state,Vec3 camera){
        for(var actor:state.update.actors())if(actor.awayState()==AwayState.AWAY||actor.awayState()==AwayState.RETURN_FADE){
            Vec3 at=actor.feet().add(0,1.05,0);
            label(root,buffers,at.add(0,.12,0),camera,actor.name(),0xFFD8DCD5,0,0,.0035f);
            int seconds=actor.awaySeconds();String time=seconds<0?"∞":String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
            label(root,buffers,at,camera,time,0xFFE9CD81,0,0,.006f);
        }
    }
    /** Private controls use the same camera-facing world plane as the rest of the tabletop UI. */
    private static void label(PoseStack root,MultiBufferSource buffers,Vec3 at,Vec3 camera,String text,int color,int action,int value){
        label(root,buffers,at,camera,text,color,action,value,.004f);
    }
    private static void label(PoseStack root,MultiBufferSource buffers,Vec3 at,Vec3 camera,String text,int color,int action,int value,float scale){
        var mc=Minecraft.getInstance();root.pushPose();root.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);root.mulPose(billboardRotation());root.scale(scale,-scale,scale);
        float half=mc.font.width(text)/2f;boolean inside=false;
        if(action!=0&&ray(root)){float dz=FAR.z-NEAR.z;if(Math.abs(dz)>1e-8){float t=-NEAR.z/dz;float x=NEAR.x+(FAR.x-NEAR.x)*t,y=NEAR.y+(FAR.y-NEAR.y)*t;inside=t>=0&&Math.abs(x)<=half+3&&y>=-3&&y<=11;}}
        if(inside){hover=action;hoverValue=value;color=0xFFFFD400;}
        // Use the same depth-independent material as the lobby: shader packs can
        // otherwise clip individual glyphs against entities or the world.
        text(root,text,-half,color);root.popPose();
    }
    private static void rules(PoseStack root,MultiBufferSource buffers,PreparationClient.State state,Vec3 camera){
        var r=state.update.rules();var me=Minecraft.getInstance().player.getUUID();var actor=state.actor(me);if(actor==null)return;
        Vec3 center=deck(state,false),draw=deck(state,true);
        if(!r.started())return;
        if(actor.debt()>0)label(root,buffers,draw.add(0,DeckRenderer.height(state.update.remaining())/16+.13,0),camera,"+"+actor.debt(),0xFFFFD400,0,0,.008f);
        if(actor.canDraw()&&state.update.remaining()==0)label(root,buffers,draw.add(0,.12,0),camera,tr("draw"),-1,PreparationUpdate.DRAW,0);
        if(r.challenger().equals(me))label(root,buffers,center.add(0,.43,0),camera,tr("challenge"),-1,PreparationUpdate.CHALLENGE,0);
        if(r.chooser().equals(me)&&state.plays.values().stream().noneMatch(p->p.progress()<1)){
            label(root,buffers,center.add(0,.53,0),camera,tr("choose_color"),-1,0,0);
            root.pushPose();Vec3 at=center.add(0,.36,0).subtract(camera);root.translate(at.x,at.y,at.z);
            root.mulPose(billboardRotation());
            for(int i=0;i<4;i++){
                root.pushPose();root.translate((i-1.5)*.16,0,0);root.mulPose(Axis.XP.rotationDegrees(90));
                float[] pointer=plane(root);boolean inside=pointer!=null&&Math.abs(pointer[0])<.066&&Math.abs(pointer[1])<.066;
                if(inside){hover=PreparationUpdate.COLOR;hoverValue=i;}
                colorSquare(root,COLORS[i],inside?.071f:.066f,inside?0xFFFFD400:0xFFB7B6AC);root.popPose();
            }
            root.popPose();
        }
        if(r.winner().equals(PreparationUpdate.NONE)){
            if(r.paused())label(root,buffers,center.add(0,.65,0),camera,tr("paused"),-1,0,0);
            else if(!state.update.vote().active()){
                root.pushPose();Vec3 at=center.add(0,.65,0).subtract(camera);root.translate(at.x,at.y,at.z);
                root.mulPose(billboardRotation());root.scale(.004f,-.004f,.004f);
                turn=new Turn(r.turn(),name(r.turn()),r.direction(),overlayPose(root));
                root.popPose();
            }
        }
        else{
            root.pushPose();Vec3 at=center.add(0,.65,0).subtract(camera);root.translate(at.x,at.y,at.z);
            root.mulPose(billboardRotation());root.scale(.004f,-.004f,.004f);
            var winningActor=state.actor(r.winner());
            String loser=r.loser().equals(PreparationUpdate.NONE)?"":name(r.loser());
            winner=new Winner(r.winner(),name(r.winner()),winningActor==null?0:winningActor.score(),r.matchOver(),r.lastStanding(),loser,overlayPose(root));
            root.popPose();
            if(me.equals(r.host())&&!r.matchOver()&&!r.paused())label(root,buffers,center.add(0,.43,0),camera,tr("next"),0xFFF2EBDD,PreparationUpdate.NEXT_ROUND,0);
        }
        if(r.reveal()){
            label(root,buffers,center.add(0,1.15,0),camera,tr(r.challengeWon()?"challenge_won":"challenge_lost"),-1,0,0);
        }
    }
    private static final int[] COLORS={0xFFE44141,0xFF4189E4,0xFF49CB55,0xFFFFD940};
    private static void vote(PoseStack root,PreparationClient.State game,Vec3 camera,boolean local){
        var state=game.update.vote();var viewer=Minecraft.getInstance().player;var actor=viewer==null?null:game.actor(viewer.getUUID());
        boolean eligible=local&&actor!=null&&actor.awayState()==AwayState.ACTIVE&&state.ownChoice()<0;
        root.pushPose();Vec3 at=deck(game,false).add(0,.9,0).subtract(camera);root.translate(at.x,at.y,at.z);
        root.mulPose(billboardRotation());root.scale(.004f,-.004f,.004f);
        int hovered=-1;
        if(eligible&&ray(root)){float dz=FAR.z-NEAR.z;if(Math.abs(dz)>1e-8){float t=-NEAR.z/dz,x=NEAR.x+(FAR.x-NEAR.x)*t,y=NEAR.y+(FAR.y-NEAR.y)*t;
            if(t>=0&&y>=9&&y<=32){if(x>=-92&&x<=-8)hovered=1;else if(x>=8&&x<=92)hovered=0;}
        }}
        if(hovered>=0){hover=PreparationUpdate.CANCEL_VOTE;hoverValue=hovered;}
        votePanel=new VotePanel(state,hovered,eligible,overlayPose(root));root.popPose();
    }
    private static void rps(PoseStack root,PreparationClient.State game,Vec3 camera){
        var state=game.update.rps();UUID me=Minecraft.getInstance().player.getUUID();
        boolean eligible=state.players().contains(me)&&state.ownChoice()<0&&!game.update.rules().paused();
        List<Integer> order=game.rpsOrder(me);
        Vec3 center=deck(game,false);
        var actor=game.actor(me);
        if(actor==null)return;
        root.pushPose();
        rpsTablePose(root,center.add(0,.24,0),camera,actor.yaw(),0,.004f);
        int hovered=-1;
        if(eligible&&ray(root)){
            float dz=FAR.z-NEAR.z;if(Math.abs(dz)>1e-8){float t=-NEAR.z/dz,x=NEAR.x+(FAR.x-NEAR.x)*t,y=NEAR.y+(FAR.y-NEAR.y)*t;
                if(t>=0&&y>=-28&&y<=6)for(int slot=0;slot<3;slot++)if(Math.abs(x-(slot-1)*44)<=17){hovered=order.get(slot);hover=PreparationUpdate.RPS_CHOICE;hoverValue=hovered;}
            }
        }
        rpsPanel=new RpsPanel(state,order,hovered,eligible,overlayPose(root));
        root.popPose();
    }
    private static void revealRps(PoseStack root,MultiBufferSource buffers,PreparationClient.State game,Vec3 camera){
        var state=game.update.rps();
        Vec3 center=deck(game,false);
        for(int i=0;i<state.players().size();i++){
            var actor=game.actor(state.players().get(i));
            int sign=state.choices()[i];
            if(actor==null||sign<0)continue;
            Vec3 at=pile(game,actor.seat()).add(0,.009,0);
            root.pushPose();
            rpsTablePose(root,at,camera,actor.yaw(),-90,.007f);
            RPS_CHOICES.add(new RpsChoice(sign,overlayPose(root)));
            root.popPose();
        }
        String verdict=!state.winner().equals(PreparationUpdate.NONE)
            ? Component.translatable("tabletop_stuff.rps.winner",name(state.winner())).getString()
            : state.remaining()==state.players().size()
                ? Component.translatable("tabletop_stuff.rps.tie").getString()
                : Component.translatable("tabletop_stuff.rps.advance",state.remaining()).getString();
        label(root,buffers,center.add(0,.40,0),camera,verdict,0xFFF1C56B,0,0,.005f);
    }
    private static void rpsTablePose(PoseStack root,Vec3 at,Vec3 camera,float yaw,float tilt,float scale){
        Vec3 offset=at.subtract(camera);
        root.translate(offset.x,offset.y,offset.z);
        root.mulPose(Axis.YP.rotationDegrees(180-yaw));
        root.mulPose(Axis.XP.rotationDegrees(tilt));
        root.scale(scale,-scale,scale);
    }
    private static void awayCards(PoseStack root,MultiBufferSource buffers,int light,PreparationUpdate.Actor actor){
        int count=actor.faces().length;if(count==0)return;
        root.pushPose();root.mulPose(Axis.YP.rotationDegrees(180-actor.yaw()));
        if(count>12){root.scale(-1f/16,-1f/16,1f/16);DeckRenderer.render(root,buffers,light,count);}
        else for(int i=0;i<count;i++){
            float middle=i-(count-1)/2f;
            root.pushPose();root.translate(middle*.012,.002+i*.0003,Math.abs(middle)*.002);root.mulPose(Axis.YP.rotationDegrees(middle*2));
            root.scale(-1f/16,-1f/16,1f/16);DeckRenderer.card(root,buffers,light);root.popPose();
        }
        root.popPose();
    }
    private static void lastCall(PoseStack root,MultiBufferSource buffers,PreparationClient.State state,Vec3 camera){
        if(state.lastAt==0)return;
        float t=(System.nanoTime()-state.lastAt)/1_000_000_000f;
        if(t>=1)return;
        var actor=state.actor(state.lastPlayer);if(actor==null)return;
        Vec3 mouth=actor.feet().add(Vec3.directionFromRotation(0,actor.yaw()).scale(.24)).add(0,1.48,0);
        Vec3 destination=deck(state,false).add(0,.35,0);
        float travel=1-(1-t)*(1-t);
        Vec3 at=mouth.lerp(destination,travel);
        int alpha=Math.round(255*(1-MotionCurves.ease((t-.25f)/.75f)));
        if(alpha<=0)return;
        label(root,buffers,at,camera,"LAST!",alpha<<24|(state.lastFalse?0xEF5555:0xFFE25C),0,0,.0025f+.006f*MotionCurves.ease(t));
    }
    private static void colorSquare(PoseStack root,int color,float half,int outline){
        COLOR_SQUARES.add(new ColorSquare(overlayPose(root),color,half,outline));
    }
    private static void drawColorSquare(PoseStack root,int color,float half,int outline){
        var out=LobbyRenderBuffers.UI.getBuffer(RenderType.textSeeThrough(PIXEL));
        border(root,out,outline,-half,-half,half,half);
        root.translate(0,.0005,0);border(root,out,0xE8202226,-half+.003f,-half+.003f,half-.003f,half-.003f);
        root.translate(0,.0005,0);border(root,out,color,-half+.009f,-half+.009f,half-.009f,half-.009f);
    }
    /** Each viewer gets a front-facing indicator at the same world position above the discard. */
    private static void chosenColor(PoseStack root,PreparationClient.State state,Vec3 camera){
        var rules=state.update.rules();if(!rules.started()||rules.color()<0||!rules.chooser().equals(PreparationUpdate.NONE)||state.update.discarded().isEmpty())return;
        int topFace=state.update.discarded().get(state.update.discarded().size()-1).face();
        if(topFace!=52&&topFace!=53)return;
        Vec3 at=deck(state,false).add(0,state.update.discarded().size()*PlayMotion.STACK_STEP+.19,0).subtract(camera);
        root.pushPose();root.translate(at.x,at.y,at.z);root.mulPose(billboardRotation());root.mulPose(Axis.XP.rotationDegrees(90));
        colorSquare(root,COLORS[rules.color()],.045f,0xFFB7B6AC);root.popPose();
    }
    private static void rectangle(PoseStack stack,MultiBufferSource buffers,int color,float halfWidth,float halfLength,float edge){
        var out=buffers.getBuffer(RenderType.text(PIXEL));
        border(stack,out,color,-halfWidth,-halfLength,halfWidth,-halfLength+edge);
        border(stack,out,color,-halfWidth,halfLength-edge,halfWidth,halfLength);
        border(stack,out,color,-halfWidth,-halfLength+edge,-halfWidth+edge,halfLength-edge);
        border(stack,out,color,halfWidth-edge,-halfLength+edge,halfWidth,halfLength-edge);
    }
    private static void target(PoseStack stack,MultiBufferSource buffers,int color){
        rectangle(stack,buffers,color,.12f,.145f,.008f);
    }
    private static void border(PoseStack stack,VertexConsumer out,int color,float left,float top,float right,float bottom){
        float[] x={left,left,right,right},z={top,bottom,bottom,top};
        for(int i=0;i<4;i++){
            //? if >=1.21.1 {
            /*out.addVertex(stack.last().pose(),x[i],0,z[i]).setColor(color).setUv(i<2?0:1,i==0||i==3?0:1).setLight(0xF000F0);
            *///?} else {
            out.vertex(stack.last().pose(),x[i],0,z[i]).color(color>>16&255,color>>8&255,color&255,color>>>24).uv(i<2?0:1,i==0||i==3?0:1).uv2(0xF000F0).endVertex();
            //?}
        }
    }
    private static float[] plane(PoseStack stack){
        if(!ray(stack))return null;float dy=FAR.y-NEAR.y;if(Math.abs(dy)<1e-8)return null;float t=-NEAR.y/dy;
        return t<0?null:new float[]{NEAR.x+(FAR.x-NEAR.x)*t,NEAR.z+(FAR.z-NEAR.z)*t};
    }
    private static boolean hitBox(PoseStack stack,float half,float yMin,float length,boolean held){
        if(held){
            INVERSE.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
            if(!unprojectPointer())return false;
        }else if(!ray(stack))return false;
        double lo=0,hi=Double.POSITIVE_INFINITY;
        float[] a={NEAR.x,NEAR.y,NEAR.z},d={FAR.x-NEAR.x,FAR.y-NEAR.y,FAR.z-NEAR.z},min={-half,yMin,-length},max={half,0,length};
        for(int i=0;i<3;i++){if(Math.abs(d[i])<1e-8){if(a[i]<min[i]||a[i]>max[i])return false;}else{double p=(min[i]-a[i])/d[i],q=(max[i]-a[i])/d[i];lo=Math.max(lo,Math.min(p,q));hi=Math.min(hi,Math.max(p,q));if(hi<lo)return false;}}return true;
    }
    private static boolean ray(PoseStack stack){
        INVERSE.set(WORLD_PROJECTION).mul(WORLD_VIEW).mul(stack.last().pose());
        return unprojectPointer();
    }
    private static boolean unprojectPointer(){
        if(Math.abs(INVERSE.determinant())<1e-12)return false;
        var mc=Minecraft.getInstance();
        INVERSE.invert();float x=(float)(2*mc.mouseHandler.xpos()/mc.getWindow().getScreenWidth()-1),y=(float)(1-2*mc.mouseHandler.ypos()/mc.getWindow().getScreenHeight());
        INVERSE.transformProject(NEAR.set(x,y,-1));INVERSE.transformProject(FAR.set(x,y,1));return true;
    }
    private static float scatter(UUID id,int index,int axis){long bits=id.getLeastSignificantBits()+index*0x9E3779B97F4A7C15L+axis*0x632BE59BD9B4E019L;bits=(bits^(bits>>>30))*0xBF58476D1CE4E5B9L;return((bits>>>40)&0xFFFFFF)/(float)0xFFFFFF*2-1;}
    private static float smooth(float t){return t*t*(3-2*t);}
}
