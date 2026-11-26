package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rigid card-to-palm binding until release; face-up landing keeps the thrower's orientation. */
final class PlayMotion {
    static final float LENGTH=1,GRAB=.22f,RELEASE=.55f,STACK_STEP=.0003f;
    static Matrix4f source(HandLayout.Card card,float handOffset){
        var grip=VisualPose.GRIP;
        return PreparationMotion.arm(VisualPose.LEFT,true).translateLocal(0,handOffset,0).translate((1+grip.x())/16,(10+grip.y())/16,grip.z()/16)
            .rotateZYX(grip.roll(),grip.yaw(),grip.pitch()).scale(1f/16).translate(card.x(),card.y(),card.z()).rotateZ(card.roll()).translate(0,HandLayout.GRIP_INSET,0);
    }
    static Matrix4f right(Matrix4f source,Vector3f target,float t){
        Matrix4f start=PreparationMotion.arm(VisualPose.RIGHT,false);
        Vector3f local=new Vector3f(1f/16,9f/16,0),shoulder=start.getTranslation(new Vector3f());
        Vector3f contact=source.transformPosition(new Vector3f(-VisualPose.CARD_WIDTH/2,-VisualPose.CARD_HEIGHT*.55f,0));
        Quaternionf initial=start.getUnnormalizedRotation(new Quaternionf());
        Quaternionf grip=new Quaternionf().rotationTo(initial.transform(new Vector3f(local)).normalize(),new Vector3f(contact).sub(shoulder).normalize()).mul(initial);
        Vector3f release=new Vector3f(contact).lerp(target,.42f);release.y-=.035f;
        Quaternionf flick=new Quaternionf().rotationTo(grip.transform(new Vector3f(local)).normalize(),new Vector3f(release).sub(shoulder).normalize()).mul(grip);
        Matrix4f contactArm=new Matrix4f().translationRotateScale(new Vector3f(contact).sub(grip.transform(new Vector3f(local))),grip,new Vector3f(1));
        Matrix4f cardGrip=contactArm.invert().mul(new Matrix4f(source));
        Matrix4f releasedCard=new Matrix4f().translationRotateScale(new Vector3f(release).sub(flick.transform(new Vector3f(local))),flick,new Vector3f(1)).mul(cardGrip);
        float bottom=-Float.MAX_VALUE;
        for(float x:new float[]{-VisualPose.CARD_WIDTH/2,VisualPose.CARD_WIDTH/2})for(float y:new float[]{-VisualPose.CARD_HEIGHT,0})for(float z:new float[]{-VisualPose.CARD_THICKNESS/2,VisualPose.CARD_THICKNESS/2})
            bottom=java.lang.Math.max(bottom,releasedCard.transformPosition(new Vector3f(x,y,z)).y);
        // Model Y points down: the entire card must clear the table before release.
        release.y-=java.lang.Math.max(0,bottom-target.y+.045f);
        Quaternionf rotation;Vector3f at;
        if(t<=GRAB){float f=MotionCurves.ease(t/GRAB);rotation=new Quaternionf(initial).slerp(grip,f);at=MotionCurves.arc(start.transformPosition(new Vector3f(local)),contact,f,.015f);}
        else if(t<=RELEASE){float f=MotionCurves.throwProgress((t-GRAB)/(RELEASE-GRAB));rotation=new Quaternionf(grip).slerp(flick,f);at=MotionCurves.arc(contact,release,f,.03f);}
        else{float f=(t-RELEASE)/(1-RELEASE);rotation=new Quaternionf(flick).slerp(initial,MotionCurves.ease(f));at=MotionCurves.followThrough(contact,release,start.transformPosition(new Vector3f(local)),f,(1-RELEASE)/(RELEASE-GRAB));}
        return new Matrix4f().translationRotateScale(at.sub(rotation.transform(new Vector3f(local))),rotation,new Vector3f(1));
    }
    static Matrix4f carried(Matrix4f source,Vector3f target,float t){
        if(t<=GRAB)return new Matrix4f(source);
        Matrix4f grip=right(source,target,GRAB).invert().mul(new Matrix4f(source));
        return right(source,target,Math.min(t,RELEASE)).mul(grip);
    }
    static Matrix4f landing(PreparationUpdate.Played card,Vector3f center){
        return new Matrix4f().translation(center.x+card.x(),center.y+card.id()*STACK_STEP,center.z+card.z())
            .rotateY((float)java.lang.Math.toRadians(card.yaw())).rotateX(-(float)java.lang.Math.PI/2).scale(.9375f/16).translate(0,VisualPose.CARD_HEIGHT/2,0);
    }
    static Matrix4f flight(Matrix4f from,Matrix4f to,float t){
        Quaternionf rotation=from.getUnnormalizedRotation(new Quaternionf()).slerp(to.getUnnormalizedRotation(new Quaternionf()),PreparationMotion.smooth(t/.35f));
        Vector3f scale=from.getScale(new Vector3f()).lerp(to.getScale(new Vector3f()),t);
        Vector3f localCenter=new Vector3f(0,-VisualPose.CARD_HEIGHT/2,0);
        Vector3f destination=to.transformPosition(new Vector3f(localCenter));
        Vector3f center=from.transformPosition(new Vector3f(localCenter)).lerp(destination,t);
        float arc=4*t*(1-t);center.y+=.085f*arc;
        Matrix4f basis=new Matrix4f().rotation(rotation).scale(scale);
        float extent=java.lang.Math.abs(basis.m01())*VisualPose.CARD_WIDTH/2+java.lang.Math.abs(basis.m11())*VisualPose.CARD_HEIGHT/2+java.lang.Math.abs(basis.m21())*VisualPose.CARD_THICKNESS/2;
        float landedExtent=java.lang.Math.abs(to.m01())*VisualPose.CARD_WIDTH/2+java.lang.Math.abs(to.m11())*VisualPose.CARD_HEIGHT/2+java.lang.Math.abs(to.m21())*VisualPose.CARD_THICKNESS/2;
        center.y=java.lang.Math.max(center.y,destination.y+extent-landedExtent+.025f*arc);
        Vector3f at=center.sub(basis.transformDirection(localCenter));
        return new Matrix4f().translationRotateScale(at,rotation,scale);
    }
    private PlayMotion(){}
}
