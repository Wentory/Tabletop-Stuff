package com.wentory.tabletopstuff.client;

import org.joml.Vector3f;

/** Curves shared by hands and held objects; all distances are in model space. */
final class MotionCurves {
    static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*t*(10+t*(-15+6*t));}
    // Accelerate into the release rather than stopping the hand before the card flies.
    static float throwProgress(float t){t=Math.max(0,Math.min(1,t));return t*t*(2-t);}
    static Vector3f arc(Vector3f from,Vector3f to,float t,float lift){
        Vector3f p=new Vector3f(from).lerp(to,t);
        float envelope=16*t*t*(1-t)*(1-t);
        return p.add(0,-lift*envelope,0);
    }
    /** Cubic Hermite return with the outgoing carry velocity and a stationary finish. */
    static Vector3f followThrough(Vector3f contact,Vector3f release,Vector3f rest,float t,float durationRatio){
        t=Math.max(0,Math.min(1,t));float t2=t*t,t3=t2*t;
        return new Vector3f(release).mul(2*t3-3*t2+1)
            .add(new Vector3f(release).sub(contact).mul(durationRatio*(t3-2*t2+t)))
            .add(new Vector3f(rest).mul(-2*t3+3*t2));
    }
    private MotionCurves(){}
}
