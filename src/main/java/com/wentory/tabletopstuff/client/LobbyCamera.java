package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.DampedValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** World-space UI follows player translation while retaining its opening orientation. */
public final class LobbyCamera {
    public record View(Vec3 position, float yaw, float pitch) {}
    private static final DampedValue YAW=new DampedValue(), PITCH=new DampedValue();
    private static Vec3 source, anchor,offset;
    private static Quaternionf orientation;
    private static float baseYaw,basePitch;
    private static long started,previous;
    private static boolean open;

    public static void begin(BlockPos pos){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        baseYaw=mc.player.getYRot();basePitch=mc.player.getXRot();
        source=Vec3.atCenterOf(pos).add(0,-0.3,0);
        offset=Vec3.directionFromRotation(basePitch,baseYaw).scale(1.05);
        anchor=mc.player.getEyePosition().add(offset);
        orientation=new Quaternionf().rotationYXZ((float)Math.PI-(float)Math.toRadians(baseYaw),-(float)Math.toRadians(basePitch),0);
        YAW.reset();PITCH.reset();started=System.nanoTime();previous=0;open=true;
    }
    public static float progress(){float t=Math.max(0,Math.min(1,(System.nanoTime()-started)/450_000_000f));return t*t*t*(t*(t*6-15)+10);}
    public static Vec3 anchor(){return source.lerp(anchor,progress());}
    public static Quaternionf orientation(){return orientation;}
    public static void end(){clear();}
    public static void clear(){open=false;source=null;anchor=null;offset=null;orientation=null;previous=0;YAW.reset();PITCH.reset();}
    public static View apply(Vec3 position,float yaw,float pitch){
        Minecraft mc=Minecraft.getInstance();if(!open||!(TabletopChatScreen.active() instanceof LobbyScreen)||mc.player==null)return null;
        // Camera position already includes interpolated jumping/falling and eye height.
        // Follow that translation, without making the menu rotate after the cursor.
        anchor=position.add(offset);
        long now=System.nanoTime();double dt=previous==0?0:Math.min(.1,(now-previous)/1_000_000_000.0);previous=now;
        var window=mc.getWindow();
        if(!TabletopChatScreen.open()){
            YAW.update(com.wentory.tabletopstuff.prototype.VisualPose.cursorOffset(mc.mouseHandler.xpos(),window.getScreenWidth(),16),dt,12,16);
            PITCH.update(com.wentory.tabletopstuff.prototype.VisualPose.cursorOffset(mc.mouseHandler.ypos(),window.getScreenHeight(),10),dt,12,10);
        }
        float viewYaw=baseYaw+YAW.value(),viewPitch=Math.max(-89,Math.min(89,basePitch+PITCH.value()));
        mc.player.setYRot(viewYaw);mc.player.setYHeadRot(viewYaw);mc.player.setXRot(viewPitch);
        return new View(position,viewYaw,viewPitch);
    }
}
