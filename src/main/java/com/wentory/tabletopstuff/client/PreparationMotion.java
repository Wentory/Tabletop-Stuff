package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rigid grip tracks in player-model space. Arms and held objects use the same track. */
final class PreparationMotion {
    static final float PHASE=2f, SHUFFLE=.65f, DEAL=.4f, DEAL_GRAB=.25f, DEAL_RELEASE=.58f, TAKE=TableAnimations.TAKE_LENGTH;
    private static final Matrix4f AUTHORED_LEFT_GRIP=new Matrix4f().rotateY(rad(35))
        .translate(VisualPose.SHUFFLE_GRIP.x()/16,VisualPose.SHUFFLE_GRIP.y()/16,VisualPose.SHUFFLE_GRIP.z()/16)
        .rotateZYX(VisualPose.SHUFFLE_GRIP.roll(),VisualPose.SHUFFLE_GRIP.yaw(),VisualPose.SHUFFLE_GRIP.pitch());
    private static final Vector3f GRIP_CORRECTION=gripCorrection();
    private static final Matrix4f LEFT_GRIP=new Matrix4f(AUTHORED_LEFT_GRIP).translate(GRIP_CORRECTION);
    private static final float HOLD_LOWER=4f/16;
    private static final Matrix4f HOLD=lower(arm(VisualPose.SHUFFLE_LEFT,true).mul(LEFT_GRIP),1);
    private static final Matrix4f[] RIGHT_HOLDS=new Matrix4f[109];
    private static Vector3f gripCorrection(){
        return GripContact.bottomSupport(AUTHORED_LEFT_GRIP,true);
    }
    static Matrix4f holdingDeck(){return new Matrix4f(HOLD);}
    static Matrix4f holdingRight(int count){
        count=Math.max(1,Math.min(108,count));
        if(RIGHT_HOLDS[count]==null)RIGHT_HOLDS[count]=rightContact(HOLD,count);
        return new Matrix4f(RIGHT_HOLDS[count]);
    }
    private static Matrix4f rightContact(Matrix4f deck,int count){
        Matrix4f right=lower(arm(VisualPose.SHUFFLE_RIGHT,false),1);
        Matrix4f relative=new Matrix4f(right).invert().mul(deck);
        Vector3f shift=right.transformDirection(GripContact.deckShift(relative,false,count));
        return right.setTranslation(right.getTranslation(new Vector3f()).sub(shift));
    }
    static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static Matrix4f lower(Matrix4f track,float weight){return track.setTranslation(track.getTranslation(new Vector3f()).add(0,HOLD_LOWER*weight,0));}
    private static float rad(float v){return(float)Math.toRadians(v);}
    static Matrix4f body(PreparationUpdate.Actor actor){return new Matrix4f().rotateY(rad(180-actor.yaw())).scale(-.9375f,-.9375f,.9375f).translate(0,-1.501f,0);}
    static Matrix4f arm(VisualPose.Arm pose,boolean left){return new Matrix4f().translate(((left?5:-5)+pose.offsetX())/16,(2+pose.offsetY())/16,pose.offsetZ()/16).rotateZYX(pose.roll(),pose.yaw(),pose.pitch());}
    static Matrix4f table(PreparationClient.State state,PreparationUpdate.Actor actor,Vec3 at){
        return body(actor).invert().translate((float)(at.x-actor.feet().x),(float)(at.y-actor.feet().y),(float)(at.z-actor.feet().z))
            .rotateY(rad(state.actor(state.update.rules().dealer()).yaw())).rotateZ((float)Math.PI).scale(.9375f);
    }
    static Matrix4f blend(Matrix4f a,Matrix4f b,float t){
        return new Matrix4f().translationRotateScale(a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),t),
            a.getUnnormalizedRotation(new Quaternionf()).slerp(b.getUnnormalizedRotation(new Quaternionf()),t),new Vector3f(1));
    }
    static Matrix4f cardBlend(Matrix4f a,Matrix4f b,float t){
        return new Matrix4f().translationRotateScale(a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),t),
            a.getUnnormalizedRotation(new Quaternionf()).slerp(b.getUnnormalizedRotation(new Quaternionf()),t),a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()),t));
    }
    static Matrix4f flight(Matrix4f release,Matrix4f landing,float t){
        float settle=Math.min(1,t/.35f);
        Vector3f p=release.getTranslation(new Vector3f()).lerp(landing.getTranslation(new Vector3f()),t);
        p.y+=.035f*16*settle*settle*(1-settle)*(1-settle);
        return new Matrix4f().translationRotateScale(p,release.getUnnormalizedRotation(new Quaternionf()).slerp(landing.getUnnormalizedRotation(new Quaternionf()),smooth(settle)),new Vector3f(1));
    }
    static Matrix4f deck(PreparationClient.State state,PreparationUpdate.Actor actor){
        if(state.update.phase()==1){float time=state.phase(PickupAnimation.LENGTH)*PickupAnimation.LENGTH;return lower(PickupAnimation.deck(time).translate(new Vector3f(GRIP_CORRECTION).mul(smooth((time-.8f)/.7f))),smooth((time-.8f)/.7f));}
        if(state.update.phase()==3){float time=state.phase(PHASE)*PHASE;return lower(shifted(TableAnimations.placeDeckGrip(time),placeDelta(state,actor),smooth(time/1.5f)).translate(new Vector3f(GRIP_CORRECTION).mul(1-smooth(time/1.5f))),1-smooth(time/1.5f));}
        return new Matrix4f(HOLD);
    }
    static Matrix4f taken(PreparationClient.State state,PreparationUpdate.Actor actor){
        float time=state.take(actor.id())*TAKE;
        Vector3f source=source(state,actor),destination=insertPoint(state,actor);
        boolean first=state.firstTake(actor.id());
        float ownerYaw=actor.awayState()==AwayState.ACTIVE?state.actor(state.update.rules().dealer()).yaw():actor.yaw();
        Quaternionf sourceRotation=body(actor).invert().rotateY(rad(actor.fromDeck()?180-ownerYaw:ownerYaw)).rotateZ((float)Math.PI).getUnnormalizedRotation(new Quaternionf());
        Matrix4f contact=pickupRight(TableAnimations.CONTACT,first,source,destination);
        Matrix4f grip=contact.invert().mul(new Matrix4f().translationRotateScale(source,sourceRotation,new Vector3f(1)));
        return pickupRight(time,first,source,destination).mul(grip);
    }
    private static Vector3f insertPoint(PreparationClient.State state,PreparationUpdate.Actor actor){
        Matrix4f left=state.firstTake(actor.id())?TableAnimations.takeLeftArm(TableAnimations.MERGE):arm(VisualPose.LEFT,true);
        return left.translate(1f/16,10f/16,-1f/16).getTranslation(new Vector3f());
    }
    /** Aim the rigid forearm at the source, then carry towards the left hand, without a sideways sweep. */
    static Matrix4f pickupRight(float time,boolean first,Vector3f source,Vector3f destination){
        Matrix4f start=first?TableAnimations.takeRightArm(0):arm(VisualPose.RIGHT,false);
        Matrix4f end=arm(VisualPose.RIGHT,false);
        Vector3f palm=new Vector3f(-1f/16,10f/16,0);
        Vector3f startPalm=start.transformPosition(new Vector3f(palm)),endPalm=end.transformPosition(new Vector3f(palm));
        Vector3f contactPalm=new Vector3f(source).add(0,-.02f,.07f);
        Quaternionf startRotation=start.getUnnormalizedRotation(new Quaternionf()),endRotation=end.getUnnormalizedRotation(new Quaternionf());
        Vector3f forward=startRotation.transform(new Vector3f(palm)).normalize();
        Vector3f towards=new Vector3f(contactPalm).sub(start.getTranslation(new Vector3f())).normalize();
        Quaternionf reach=new Quaternionf().rotationTo(forward,towards).mul(startRotation);
        // Preserve the palm-to-object offset through the turn towards the left hand.
        Vector3f localOffset=reach.conjugate(new Quaternionf()).transform(new Vector3f(source).sub(contactPalm));
        Vector3f mergePalm=new Vector3f(destination).sub(endRotation.transform(new Vector3f(localOffset)));
        Quaternionf rotation;Vector3f at;
        if(time<=TableAnimations.CONTACT){float t=MotionCurves.ease(time/TableAnimations.CONTACT);rotation=new Quaternionf(startRotation).slerp(reach,t);at=MotionCurves.arc(startPalm,contactPalm,t,.025f);}
        else if(time<=TableAnimations.MERGE){float t=MotionCurves.ease((time-TableAnimations.CONTACT)/(TableAnimations.MERGE-TableAnimations.CONTACT));rotation=new Quaternionf(reach).slerp(endRotation,t);at=MotionCurves.arc(contactPalm,mergePalm,t,.045f);}
        else{float t=MotionCurves.ease((time-TableAnimations.MERGE)/(TAKE-TableAnimations.MERGE));rotation=new Quaternionf(endRotation);at=new Vector3f(mergePalm).lerp(endPalm,t);}
        return new Matrix4f().translationRotateScale(at.sub(rotation.transform(new Vector3f(palm))),rotation,new Vector3f(1));
    }
    static Vector3f source(PreparationClient.State state,PreparationUpdate.Actor actor){
        Vec3 point=actor.fromDeck()?PreparationRenderer.deck(state,true).add(0,DeckRenderer.height(state.update.remaining())/16,0):PreparationRenderer.pile(state,actor.seat());
        return table(state,actor,point).getTranslation(new Vector3f());
    }
    private static Vector3f placeDelta(PreparationClient.State state,PreparationUpdate.Actor actor){return table(state,actor,PreparationRenderer.deck(state,true)).getTranslation(new Vector3f()).sub(TableAnimations.placeDeckGrip(2).getTranslation(new Vector3f()));}
    private static Matrix4f shifted(Matrix4f matrix,Vector3f delta,float weight){return matrix.setTranslation(matrix.getTranslation(new Vector3f()).add(delta.mul(weight)));}
    static Matrix4f packet(PreparationClient.State state){
        int total=state.update.remaining(),packet=state.update.packetSize(),above=state.update.insertionDepth();
        return new Matrix4f(HOLD).scale(1f/(16*.9375f)).mul(shufflePacket(total,packet,above,state.action(SHUFFLE))).scale(16*.9375f);
    }
    static Matrix4f shufflePacket(int total,int packet,int above,float t){
        Vector3f offset=shuffleOffset(total,packet,above,t);
        float turn=MotionCurves.ease((t-.15f)/.25f)*(1-MotionCurves.ease((t-.7f)/.2f));
        // Rotate around the packet centre; the right hand follows this same rigid frame.
        float halfHeight=DeckRenderer.height(packet)/2;
        return new Matrix4f().translate(offset).translate(0,-halfHeight,0)
            .rotateY(rad(5)*turn).rotateZ(rad(-3)*turn).translate(0,halfHeight,0);
    }
    static Vector3f shuffleOffset(int total,int packet,int above,float t){
        float out=t<.4f?MotionCurves.ease((t-.15f)/.25f):t<.7f?1:1-MotionCurves.ease((t-.7f)/.2f);
        float lowered=MotionCurves.ease((t-.4f)/.3f);
        return new Vector3f(out*2.8f,-DeckRenderer.height(total-packet)+DeckRenderer.height(above)*lowered-.35f*out*(1-lowered),.25f*out);
    }
    static Matrix4f shuffleRight(Matrix4f packet,int count){
        return new Matrix4f(packet).mul(new Matrix4f(HOLD).invert()).mul(holdingRight(count));
    }
    static Matrix4f deal(PreparationClient.State state,PreparationUpdate.Actor actor){
        Matrix4f top=new Matrix4f(HOLD).translate(0,-DeckRenderer.height(state.update.remaining()+1)/(16*.9375f),0);
        var recipient=state.actor(state.update.target());if(recipient==null)return top;
        Vector3f to=table(state,actor,PreparationRenderer.pile(state,recipient.seat())).getTranslation(new Vector3f());
        return dealtCard(top,to,state.action(DEAL),state.update.remaining()+1);
    }
    static Matrix4f dealtCard(Matrix4f top,Vector3f target,float progress){
        return dealtCard(top,target,progress,108);
    }
    static Matrix4f dealtCard(Matrix4f top,Vector3f target,float progress,int count){
        if(progress<=DEAL_GRAB)return new Matrix4f(top);
        Matrix4f contact=dealRight(top,target,DEAL_GRAB,count);
        return dealRight(top,target,Math.min(progress,DEAL_RELEASE),count).mul(contact.invert().mul(new Matrix4f(top)));
    }
    static Matrix4f dealRight(Matrix4f top,Vector3f target,float progress){
        return dealRight(top,target,progress,108);
    }
    static Matrix4f dealRight(Matrix4f top,Vector3f target,float progress,int count){
        Matrix4f start=holdingRight(count),end=holdingRight(Math.max(1,count-1));
        Vector3f localPalm=new Vector3f(1f/16,9f/16,0);
        Vector3f contact=top.transformPosition(new Vector3f(-VisualPose.CARD_WIDTH/(2*16*.9375f),0,VisualPose.CARD_HEIGHT/(4*16*.9375f)));
        Vector3f shoulder=start.getTranslation(new Vector3f());Quaternionf initial=start.getUnnormalizedRotation(new Quaternionf());
        Vector3f initialAxis=initial.transform(new Vector3f(localPalm)).normalize();
        Quaternionf grip=new Quaternionf().rotationTo(initialAxis,new Vector3f(contact).sub(shoulder).normalize()).mul(initial);
        Vector3f release=new Vector3f(contact).lerp(target,.38f);release.y-=.04f;
        Quaternionf throwRotation=new Quaternionf().rotationTo(grip.transform(new Vector3f(localPalm)).normalize(),new Vector3f(release).sub(shoulder).normalize()).mul(grip);
        Quaternionf rotation;Vector3f at;
        if(progress<=DEAL_GRAB){float t=MotionCurves.ease(progress/DEAL_GRAB);rotation=new Quaternionf(initial).slerp(grip,t);at=MotionCurves.arc(start.transformPosition(new Vector3f(localPalm)),contact,t,.012f);}
        else if(progress<=DEAL_RELEASE){float t=MotionCurves.throwProgress((progress-DEAL_GRAB)/(DEAL_RELEASE-DEAL_GRAB));rotation=new Quaternionf(grip).slerp(throwRotation,t);at=MotionCurves.arc(contact,release,t,.025f);}
        else{float t=(progress-DEAL_RELEASE)/(1-DEAL_RELEASE);rotation=new Quaternionf(throwRotation).slerp(end.getUnnormalizedRotation(new Quaternionf()),MotionCurves.ease(t));at=MotionCurves.followThrough(contact,release,end.transformPosition(new Vector3f(localPalm)),t,(1-DEAL_RELEASE)/(DEAL_RELEASE-DEAL_GRAB));}
        return new Matrix4f().translationRotateScale(at.sub(rotation.transform(new Vector3f(localPalm))),rotation,new Vector3f(1));
    }
    static Vec3 world(PreparationUpdate.Actor actor,Matrix4f object){
        Vector3f at=body(actor).mul(object).transformPosition(new Vector3f());return actor.feet().add(at.x,at.y,at.z);
    }
    static float floorHandOffset(PreparationClient.State state,PreparationUpdate.Actor actor){
        if(state.update.phase()!=4||!actor.seated())return 0;
        double seatTop=actor.feet().y+com.wentory.tabletopstuff.prototype.TableSeat.HIP_HEIGHT;
        return Math.abs(seatTop-state.update.table().getY())<.001?3f/16:0;
    }
    static void pose(net.minecraft.client.model.PlayerModel<?> model,LivingEntity entity,PreparationClient.State state){
        var actor=state.actor(entity.getUUID());boolean owner=actor.id().equals(state.update.rules().dealer());
        if(state.update.phase()==0||actor.awayState()==AwayState.RETURN_FADE||actor.awayState()==AwayState.AWAY||actor.awayState()==AwayState.RETIRED)return;
        var play=state.play(entity.getUUID());
        if(play!=null){
            Matrix4f source=PlayMotion.source(play.before.get(play.card.index()),floorHandOffset(state,actor));
            Vec3 at=PreparationRenderer.deck(state,false).add(play.card.x(),play.card.id()*PlayMotion.STACK_STEP,play.card.z());
            Vector3f target=table(state,actor,at).getTranslation(new Vector3f());
            follow(model.rightArm,PlayMotion.right(source,target,play.progress()),1);
            if(actor.faces().length==0){float lower=smooth((play.progress()-.65f)/.35f);
                var leftRest=model.leftArm.getInitialPose();var rightRest=model.rightArm.getInitialPose();
                follow(model.leftArm,new Matrix4f().translate(leftRest.x/16,leftRest.y/16,leftRest.z/16),lower);
                follow(model.rightArm,new Matrix4f().translate(rightRest.x/16,rightRest.y/16,rightRest.z/16),lower);
            }
            return;
        }
        if(owner&&state.update.phase()==1){float time=state.phase(PickupAnimation.LENGTH)*PickupAnimation.LENGTH;PickupAnimation.pose(model,time);float lowered=smooth((time-.8f)/.7f);model.leftArm.y+=HOLD_LOWER*16*lowered;follow(model.rightArm,blend(lower(PickupAnimation.right(time),lowered),holdingRight(108),smooth((time-1.1f)/.4f)),1);return;}
        if(owner&&state.update.phase()==3){
            float time=state.phase(PHASE)*PHASE;
            follow(model.leftArm,lower(TableAnimations.placeLeftArm(time),1-smooth(time/1.5f)),1);
            follow(model.rightArm,blend(holdingRight(state.update.remaining()),lower(shifted(TableAnimations.placeRightArm(time),placeDelta(state,actor),smooth(time/1.5f)*(1-smooth((time-1.5f)/.5f))),1-smooth(time/1.5f)),smooth(time/.4f)),1);
            follow(model.body,TableAnimations.placeBody(time),1);model.jacket.copyFrom(model.body);
            Vector3f head=TableAnimations.placeHead(time).getTranslation(new Vector3f());model.head.setPos(head.x*16,head.y*16,head.z*16);model.hat.copyFrom(model.head);return;
        }
        if(owner&&state.update.phase()>=1&&state.update.phase()<=3){
            Matrix4f deck=deck(state,actor);float t=state.phase(PHASE),weight=state.update.phase()==1?smooth(t/.3f):state.update.phase()==3?1-smooth((t-.7f)/.3f):1;
            follow(model.leftArm,new Matrix4f(deck).mul(new Matrix4f(LEFT_GRIP).invert()),weight);
            Matrix4f right=holdingRight(state.update.remaining());
            if(state.update.phase()==2&&state.update.action()==PreparationUpdate.SHUFFLE&&state.action(SHUFFLE)<1){
                float a=state.action(SHUFFLE);
                right=shuffleRight(packet(state),state.update.packetSize());
                right=blend(holdingRight(state.update.remaining()),right,a<.15f?smooth(a/.15f):1-smooth((a-.9f)/.1f));
            }
            if(state.update.phase()==2&&state.update.action()==PreparationUpdate.DEAL&&state.action(DEAL)<1){
                Matrix4f top=new Matrix4f(HOLD).translate(0,-DeckRenderer.height(state.update.remaining()+1)/(16*.9375f),0);
                var recipient=state.actor(state.update.target());
                if(recipient!=null)right=dealRight(top,table(state,actor,PreparationRenderer.pile(state,recipient.seat())).getTranslation(new Vector3f()),state.action(DEAL),state.update.remaining()+1);
            }
            follow(model.rightArm,right,weight);
        }else if(state.take(actor.id())<1){
            float time=state.take(actor.id())*TAKE;boolean first=state.firstTake(actor.id());
            if(first)follow(model.leftArm,TableAnimations.takeLeftArm(time),1);
            follow(model.rightArm,pickupRight(time,first,source(state,actor),insertPoint(state,actor)),1);
            follow(model.body,TableAnimations.takeBody(time),1);model.jacket.copyFrom(model.body);
            Vector3f head=TableAnimations.takeHead(time).getTranslation(new Vector3f());model.head.setPos(head.x*16,head.y*16,head.z*16);model.hat.copyFrom(model.head);
        }
    }
    private static void follow(ModelPart arm,Matrix4f track,float weight){
        Matrix4f previous=new Matrix4f().translate(arm.x/16,arm.y/16,arm.z/16).rotateZYX(arm.zRot,arm.yRot,arm.xRot);
        Matrix4f pose=blend(previous,track,weight);Vector3f p=pose.getTranslation(new Vector3f());
        Vector3f r=euler(pose);
        arm.setPos(p.x*16,p.y*16,p.z*16);arm.xRot=r.x;arm.yRot=r.y;arm.zRot=r.z;
    }
    static Vector3f euler(Matrix4f pose){return new Vector3f((float)Math.atan2(pose.m12(),pose.m22()),
        (float)Math.asin(Math.max(-1,Math.min(1,-pose.m02()))),(float)Math.atan2(pose.m01(),pose.m00()));}
    private PreparationMotion(){}
}
