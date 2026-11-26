package com.wentory.tabletopstuff.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Wentory's taking_cards (1.5s), numeric keys copied from deck_pickup_scene.bbmodel.
 * Only final keys were aligned with the saved holding pose. No runtime bbmodel loader. */
final class PickupAnimation {
    static final float LENGTH=1.5f;
    private static final float[][] LEFT_ARM_POSITION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.29167000f,1.21000000f,-0.60000000f,-4.43000000f}, new float[]{0.50000000f,2.00000000f,-1.00000000f,-9.00000000f}, new float[]{0.58333000f,2.00000000f,-1.00000000f,-9.00000000f}, new float[]{1.00000000f,2.00000000f,-1.00000000f,-2.00000000f}, new float[]{1.50000000f,0.00000000f,-3.00000000f,1.00000000f}};
    private static final float[][] LEFT_ARM_ROTATION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.29167000f,123.76000000f,0.00000000f,0.00000000f}, new float[]{0.50000000f,72.50000000f,0.00000000f,0.00000000f}, new float[]{0.58333000f,72.50000000f,0.00000000f,0.00000000f}, new float[]{1.50000000f,86.85153000f,-5.62287000f,32.50432000f}};
    private static final float[][] RIGHT_ARM_POSITION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.37500000f,0.00000000f,0.00000000f,-4.00000000f}, new float[]{1.04167000f,0.00000000f,0.00000000f,-4.00000000f}, new float[]{1.50000000f,0.00000000f,-4.00000000f,0.00000000f}};
    private static final float[][] RIGHT_ARM_ROTATION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.37500000f,55.00000000f,15.00000000f,0.00000000f}, new float[]{1.04167000f,55.00000000f,15.00000000f,0.00000000f}, new float[]{1.50000000f,109.76727000f,22.68079000f,4.03977000f}};
    private static final float[][] BODY_POSITION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.50000000f,0.00000000f,0.00000000f,-1.50000000f}, new float[]{1.50000000f,0.00000000f,0.00000000f,0.00000000f}};
    private static final float[][] BODY_ROTATION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.50000000f,-7.52840000f,-4.95710000f,0.65420000f}, new float[]{1.50000000f,0.00000000f,0.00000000f,0.00000000f}};
    private static final float[][] HEAD_POSITION = new float[][]{new float[]{0.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.50000000f,0.00000000f,0.00000000f,-2.10000000f}, new float[]{1.50000000f,0.00000000f,0.00000000f,0.00000000f}};
    private static final float[][] HEAD_ROTATION = new float[][]{};
    private static final float[][] DECK_GRIP_POSITION = new float[][]{new float[]{0.50000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.58333000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{1.00000000f,0.00000000f,0.00000000f,8.00000000f}, new float[]{1.50000000f,-2.16507572f,3.43957671f,13.94179673f}};
    private static final float[][] DECK_GRIP_ROTATION = new float[][]{new float[]{0.50000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{0.58333000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{1.00000000f,0.00000000f,0.00000000f,0.00000000f}, new float[]{1.50000000f,8.97468379f,-8.05710842f,-56.77415309f}};

    static Vector3f sample(float[][] keys,float time){
        if(keys.length==0)return new Vector3f();
        if(time<=keys[0][0])return value(keys[0]);
        if(time>=keys[keys.length-1][0])return value(keys[keys.length-1]);
        int i=0;while(keys[i+1][0]<time)i++;
        float t=(time-keys[i][0])/(keys[i+1][0]-keys[i][0]);
        if(keys[i].length>4&&keys[i+1].length>4&&keys[i][4]==0&&keys[i+1][4]==0)return value(keys[i]).lerp(value(keys[i+1]),t);
        Vector3f p0=value(keys[Math.max(0,i-1)]),p1=value(keys[i]),p2=value(keys[i+1]),p3=value(keys[Math.min(keys.length-1,i+2)]);
        // Time-aware, slope-limited Hermite: preserve authored holds and avoid overshoot.
        float span=keys[i+1][0]-keys[i][0];
        Vector3f result=new Vector3f();
        for(int axis=0;axis<3;axis++){
            float a=p0.get(axis),b=p1.get(axis),c=p2.get(axis),d=p3.get(axis);
            float slope=(c-b)/span;
            float before=i==0?0:(b-a)/(keys[i][0]-keys[i-1][0]);
            float after=i+2>=keys.length?0:(d-c)/(keys[i+2][0]-keys[i+1][0]);
            float m0=tangent(before,slope)*span,m1=tangent(slope,after)*span;
            float t2=t*t,t3=t2*t;
            result.setComponent(axis,(2*t3-3*t2+1)*b+(t3-2*t2+t)*m0+(-2*t3+3*t2)*c+(t3-t2)*m1);
        }
        return result;
    }
    private static float tangent(float a,float b){return a*b<=0?0:2*a*b/(a+b);}
    private static Vector3f value(float[] row){return new Vector3f(row[1],row[2],row[3]);}
    private static float rad(float value){return(float)Math.toRadians(value);}
    static Matrix4f bone(float[][] position,float[][] rotation,float time,Vector3f origin,Vector3f baseRotation){
        Vector3f p=sample(position,time).add(origin),r=sample(rotation,time).add(baseRotation);
        return new Matrix4f().translate(-p.x/16,(24-p.y)/16,p.z/16).rotateZYX(rad(r.z),rad(-r.y),rad(-r.x));
    }
    static Matrix4f deck(float time){return bone(DECK_GRIP_POSITION,DECK_GRIP_ROTATION,time,new Vector3f(0,17.10187f,-20.48f),new Vector3f());}
    static Matrix4f left(float time){return bone(LEFT_ARM_POSITION,LEFT_ARM_ROTATION,time,new Vector3f(-5,22,0),new Vector3f(.01195f,-6.91962f,-3.77296f));}
    static Matrix4f right(float time){return bone(RIGHT_ARM_POSITION,RIGHT_ARM_ROTATION,time,new Vector3f(5,22,0),new Vector3f(-2.26727f,-.68079f,3.96023f));}
    private static void apply(ModelPart part,Matrix4f pose){
        Vector3f p=pose.getTranslation(new Vector3f()),r=PreparationMotion.euler(pose);
        part.setPos(p.x*16,p.y*16,p.z*16);part.xRot=r.x;part.yRot=r.y;part.zRot=r.z;
    }
    static void pose(PlayerModel<?> model,float time){
        apply(model.leftArm,left(time));apply(model.rightArm,right(time));
        apply(model.body,bone(BODY_POSITION,BODY_ROTATION,time,new Vector3f(0,24,0),new Vector3f()));
        // Keep cursor-driven head rotation; the authored track changes its position only.
        Vector3f head=sample(HEAD_POSITION,time);model.head.setPos(-head.x,-head.y,head.z);
        model.hat.copyFrom(model.head);model.jacket.copyFrom(model.body);
    }
    private PickupAnimation(){}
}
