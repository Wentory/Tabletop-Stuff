package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.PreparationUpdate;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Numeric keys and Bezier tangents from art/poses/show_cards_scene.bbmodel. */
final class ShowCardsMotion {
    private record Key(float time, Vector3f value, Vector3f tangent) {}
    private record Clip(float duration, Key[] armPosition, Key[] armRotation, Key[] gripRotation) {}
    record Pose(Vector3f armPosition, Vector3f armRotation, Vector3f gripRotation, float shown) {
        void apply(ModelPart arm) {
            arm.x-=armPosition.x;
            arm.y-=armPosition.y;
            arm.z+=armPosition.z;
            arm.xRot-=radians(armRotation.x);
            arm.yRot-=radians(armRotation.y);
            arm.zRot+=radians(armRotation.z);
        }
    }

    private static final Clip LEFT=new Clip(0.5f,
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.5f, new Vector3f(14.7895f, 19.9507f, 1.4262f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.5f, new Vector3f(26.8202f, -3.3842f, 119.4912f), new Vector3f(0f, 0f, 0f))
        });

    private static final Clip FRONT_FROM_LEFT=new Clip(0.75f,
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.75f, new Vector3f(1f, 0f, -3f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.25f, new Vector3f(14.7895f, 19.9507f, 1.4262f), new Vector3f(0.023844659f, 0f, 0f)),
            new Key(0.75f, new Vector3f(14.7948f, 14.9627f, 1.0807f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.25f, new Vector3f(26.8202f, -3.3842f, 119.4912f), new Vector3f(8.1771713f, -14.694705f, 210.77308f)),
            new Key(0.75f, new Vector3f(28.7177f, -11.6112f, 181.528f), new Vector3f(0f, 0f, 0f))
        });

    private static final Clip FRONT_FROM_RIGHT=new Clip(0.75f,
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.75f, new Vector3f(1f, 0f, -2f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.29167f, new Vector3f(15.2276f, -7.479f, -0.6042f), new Vector3f(0f, 0f, 0f)),
            new Key(0.75f, new Vector3f(15.1408f, -4.9862f, -0.3955f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.33333f, new Vector3f(20.8643f, -17.2286f, -105.7927f), new Vector3f(24.177443f, 0f, -171.49719f)),
            new Key(0.75f, new Vector3f(26.9295f, -14.6448f, -153.5902f), new Vector3f(0f, 0f, 0f))
        });

    private static final Clip RIGHT=new Clip(0.5f,
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.5f, new Vector3f(15.2276f, -7.479f, -0.6042f), new Vector3f(0f, 0f, 0f))
        },
        new Key[]{
            new Key(0f, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 0f, 0f)),
            new Key(0.5f, new Vector3f(20.8643f, -17.2286f, -105.7927f), new Vector3f(0f, 0f, 0f))
        });


    static Pose sample(PreparationClient.State state, PreparationUpdate.Actor source){
        var reveal=state.update.reveal();
        if(reveal.remainingTicks()==0||!reveal.source().equals(source.id()))return null;
        var viewer=state.actor(reveal.viewer());
        if(viewer==null)return null;
        int relative=Math.floorMod(viewer.seat()-source.seat(),4);
        Clip clip=switch(relative){
            case 1 -> LEFT;
            case 2 -> state.update.rules().direction()>0?FRONT_FROM_LEFT:FRONT_FROM_RIGHT;
            case 3 -> RIGHT;
            default -> null;
        };
        if(clip==null)return null;
        float elapsed=(System.nanoTime()-state.revealAt)/1_000_000_000f;
        float total=PreparationUpdate.revealDurationTicks(source.seat(),viewer.seat())/20f;
        float time=Math.max(0,Math.min(clip.duration(),Math.min(elapsed,total-elapsed)));
        return new Pose(sample(clip.armPosition(),time),sample(clip.armRotation(),time),
            sample(clip.gripRotation(),time),PreparationMotion.smooth(time/.20f));
    }
    private static Vector3f sample(Key[] keys,float time){
        for(int i=1;i<keys.length;i++){
            Key before=keys[i-1],after=keys[i];
            if(time<=after.time()){
                float duration=after.time()-before.time(),t=(time-before.time())/duration,u=1-t;
                return new Vector3f(before.value()).mul(u*u*(1+2*t))
                    .fma(duration*u*u*t,before.tangent()).fma(t*t*(3-2*t),after.value())
                    .fma(-duration*u*t*t,after.tangent());
            }
        }
        return new Vector3f(keys[keys.length-1].value());
    }
    static float radians(float degrees){return (float)Math.toRadians(degrees);}
}
