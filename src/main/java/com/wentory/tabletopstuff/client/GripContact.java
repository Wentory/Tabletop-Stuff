package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.VisualPose;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Contact between the entire wide arm/sleeve box and the oriented deck, in arm space. */
final class GripContact {
    static Vector3f bottomSupport(Matrix4f relative,boolean left){
        Matrix4f inverse=new Matrix4f(relative).invert();Vector3f contact=null;
        for(int x:new int[]{-1,1})for(int y:new int[]{-1,1})for(int z:new int[]{-1,1}){
            Vector3f corner=inverse.transformPosition(new Vector3f(((left?1:-1)+x*2.25f)/16,(4+y*6.25f)/16,z*2.25f/16));
            if(contact==null||corner.y<contact.y)contact=corner;
        }
        float half=VisualPose.CARD_WIDTH/(2*16*.9375f),length=VisualPose.CARD_HEIGHT/(2*16*.9375f);
        return new Vector3f(contact.x-Math.max(-half,Math.min(half,contact.x)),contact.y,contact.z-Math.max(-length,Math.min(length,contact.z)));
    }
    private record Boxes(Vector3f hand,Vector3f deck,Vector3f half,Vector3f[] directions,List<Vector3f> axes){}
    private static Boxes boxes(Matrix4f relative,boolean left,int count){
        float scale=16*.9375f;
        Vector3f half=new Vector3f(VisualPose.CARD_WIDTH/(2*scale),DeckRenderer.height(count)/(2*scale),VisualPose.CARD_HEIGHT/(2*scale));
        Vector3f center=relative.transformPosition(new Vector3f(0,-half.y,0));
        Vector3f[] directions={relative.transformDirection(new Vector3f(1,0,0)).normalize(),relative.transformDirection(new Vector3f(0,1,0)).normalize(),relative.transformDirection(new Vector3f(0,0,1)).normalize()};
        Vector3f[] basis={new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)};
        List<Vector3f> axes=new ArrayList<>(15);for(var axis:basis)axes.add(axis);for(var axis:directions)axes.add(axis);
        for(var a:basis)for(var b:directions){var cross=a.cross(b,new Vector3f());if(cross.lengthSquared()>1e-10f)axes.add(cross.normalize());}
        return new Boxes(new Vector3f((left?1:-1)/16f,4f/16,0),center,half,directions,axes);
    }
    private static float radius(Boxes b,Vector3f axis){
        // Sleeve dilation is 0.25 model units beyond the skin cube on every face.
        return (2.25f*Math.abs(axis.x)+6.25f*Math.abs(axis.y)+2.25f*Math.abs(axis.z))/16
            +b.half.x*Math.abs(axis.dot(b.directions[0]))+b.half.y*Math.abs(axis.dot(b.directions[1]))+b.half.z*Math.abs(axis.dot(b.directions[2]));
    }
    static Vector3f deckShift(Matrix4f relative,boolean left,int count){
        Boxes b=boxes(relative,left,count);Vector3f offset=new Vector3f(b.deck).sub(b.hand);
        boolean overlap=true;float depth=Float.POSITIVE_INFINITY;Vector3f separate=new Vector3f();
        for(var axis:b.axes){float distance=offset.dot(axis),penetration=radius(b,axis)-Math.abs(distance);
            if(penetration<0)overlap=false;
            if(penetration>=0&&penetration<depth){depth=penetration;separate.set(axis).mul(distance>=0?penetration:-penetration);}
        }
        if(overlap)return separate;
        // Move towards the distal hand region; swept SAT finds first volume contact.
        Vector3f motion=new Vector3f((left?1:-1)/16f,8.5f/16,0).sub(b.deck);
        float enter=0,exit=1;
        for(var axis:b.axes){float distance=offset.dot(axis),speed=motion.dot(axis),r=radius(b,axis);
            if(Math.abs(speed)<1e-9f){if(Math.abs(distance)>r)return new Vector3f();continue;}
            float a=(-r-distance)/speed,c=(r-distance)/speed;
            enter=Math.max(enter,Math.min(a,c));exit=Math.min(exit,Math.max(a,c));
        }
        return enter<=exit?motion.mul(Math.max(0,enter)):new Vector3f();
    }
    static float gap(Matrix4f relative,boolean left,int count){
        Boxes b=boxes(relative,left,count);Vector3f offset=new Vector3f(b.deck).sub(b.hand);float gap=-Float.MAX_VALUE;
        for(var axis:b.axes)gap=Math.max(gap,Math.abs(offset.dot(axis))-radius(b,axis));return gap;
    }
    private GripContact(){}
}
