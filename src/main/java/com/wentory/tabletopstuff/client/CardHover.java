package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wentory.tabletopstuff.prototype.CardPicking;
import com.wentory.tabletopstuff.prototype.HandLayout;
import com.wentory.tabletopstuff.prototype.HoverMotion;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CardHover {
    private static int selected=-1,lastCount=-1;
    public static int selected(){return selected;}
    public static void selectHovered(){selected=motion.hovered();}
    private static int[] pickingFaces;
    private static HoverMotion motion = new HoverMotion();
    private static final Matrix4f INVERSE_CLIP = new Matrix4f();
    private static final Vector3f NEAR = new Vector3f(), FAR = new Vector3f();
    private static long lastFrame;

    public static void update(PoseStack stack, List<HandLayout.Card> cards,int[] faces) {
        if(selected>=0&&!PrototypeClient.canPlayCard(selected))selected=-1;
        if(lastCount!=cards.size()||selected>=cards.size())selected=-1;lastCount=cards.size();
        pickingFaces=faces;
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        double seconds = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1_000_000_000.0, 0.1);
        lastFrame = now;
        int target = -1;
        if (mc.isWindowActive()) {
            var window = mc.getWindow();
            float x = (float) (2 * mc.mouseHandler.xpos() / window.getScreenWidth() - 1);
            float y = (float) (1 - 2 * mc.mouseHandler.ypos() / window.getScreenHeight());
            // Exact shader projection and model-view for this draw, including camera motion.
            INVERSE_CLIP.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
            if (Math.abs(INVERSE_CLIP.determinant()) > 1e-12f && Math.abs(x) <= 1 && Math.abs(y) <= 1) {
                INVERSE_CLIP.invert();
                INVERSE_CLIP.transformProject(NEAR.set(x, y, -1));
                INVERSE_CLIP.transformProject(FAR.set(x, y, 1));
                target = CardPicking.pick(cards, NEAR.x, NEAR.y, NEAR.z,
                    FAR.x - NEAR.x, FAR.y - NEAR.y, FAR.z - NEAR.z, CardHover::opaque);
            }
        }
        motion.update(cards.size(), target, selected, seconds);
    }
    private static boolean opaque(int index,float u,float v){return pickingFaces==null?CardAtlas.opaque(index,u,v):CardAtlas.opaqueSlot(pickingFaces[index],u,v);}
    public static int hovered() { return motion.hovered(); }
    public static float focus() { return selected>=0?1:motion.focus(); }
    public static float offset(int index) { return motion.offset(index); }
    public static void clearHover(){motion=new HoverMotion();lastFrame=0;}
    public static void clear() { motion = new HoverMotion(); lastFrame = 0;selected=-1;lastCount=-1; }
}
