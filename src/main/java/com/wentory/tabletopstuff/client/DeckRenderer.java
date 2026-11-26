package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.wentory.tabletopstuff.prototype.VisualPose;

/** One six-face box, sharing the same atlas as all cards. Coordinates are model units, Y down. */
public final class DeckRenderer {
    private record Silhouette(org.joml.Matrix4f pose,org.joml.Matrix4f projection,
        java.util.List<org.joml.Vector3f> vertices,net.minecraft.resources.ResourceLocation pixel,int color){}
    private static final java.util.List<Silhouette> SILHOUETTES=new java.util.ArrayList<>();
    public static void clearOutlines(){SILHOUETTES.clear();}
    /** Draw after world post-processing so shaders cannot distort or shade the contour. */
    public static void overlay(){
        if(SILHOUETTES.isEmpty())return;
        var buffers=net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
        buffers.endBatch();
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
        var view=com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        //? if >=1.21.1 {
        /*view.pushMatrix();view.identity();
        *///?} else {
        view.pushPose();view.setIdentity();
        //?}
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        try{
            for(var silhouette:SILHOUETTES){
                com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(silhouette.projection(),com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
                var out=LobbyRenderBuffers.UI.getBuffer(net.minecraft.client.renderer.RenderType.text(silhouette.pixel()));
                for(var point:silhouette.vertices()){
                    int color=silhouette.color();
                    //? if >=1.21.1 {
                    /*out.addVertex(silhouette.pose(),point.x,point.y,point.z).setColor(color).setUv(0,0).setLight(0xF000F0);
                    *///?} else {
                    out.vertex(silhouette.pose(),point.x,point.y,point.z).color(color>>16&255,color>>8&255,color&255,color>>>24).uv(0,0).uv2(0xF000F0).endVertex();
                    //?}
                }
                buffers.endBatch();
            }
        }finally{
            //? if >=1.21.1 {
            /*view.popMatrix();
            *///?} else {
            view.popPose();
            //?}
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
            SILHOUETTES.clear();
        }
    }
    /** Project the box and outline only its convex silhouette in screen space. */
    public static void outline(PoseStack stack,MultiBufferSource buffers,int count,net.minecraft.resources.ResourceLocation pixel,int color){
        outline(stack,buffers,count,pixel,color,false);
    }
    public static void outline(PoseStack stack,MultiBufferSource buffers,int count,net.minecraft.resources.ResourceLocation pixel,int color,boolean drawHint){
        if(count<=0)return;
        var clip=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix())
            .mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(stack.last().pose());
        if(Math.abs(clip.determinant())<1e-12f)return;
        var inverse=new org.joml.Matrix4f(clip).invert();
        var window=net.minecraft.client.Minecraft.getInstance().getWindow();
        float halfWidth=window.getWidth()/2f,halfHeight=window.getHeight()/2f;
        var points=new java.util.ArrayList<org.joml.Vector3f>(8);
        for(int i=0;i<8;i++){
            var point=new org.joml.Vector4f((i&1)==0?-VisualPose.CARD_WIDTH/2:VisualPose.CARD_WIDTH/2,
                (i&2)==0?-height(count):0,(i&4)==0?-VisualPose.CARD_HEIGHT/2:VisualPose.CARD_HEIGHT/2,1);
            clip.transform(point);
            if(point.w<=0)return;
            points.add(new org.joml.Vector3f(point.x/point.w*halfWidth,point.y/point.w*halfHeight,point.z/point.w));
        }
        points.sort(java.util.Comparator.comparingDouble((org.joml.Vector3f p)->p.x).thenComparingDouble(p->p.y));
        var hull=new java.util.ArrayList<org.joml.Vector3f>(16);
        for(var point:points){
            while(hull.size()>=2&&cross(hull.get(hull.size()-2),hull.get(hull.size()-1),point)<=0)hull.remove(hull.size()-1);
            hull.add(point);
        }
        int lower=hull.size();
        for(int i=points.size()-2;i>=0;i--){
            var point=points.get(i);
            while(hull.size()>lower&&cross(hull.get(hull.size()-2),hull.get(hull.size()-1),point)<=0)hull.remove(hull.size()-1);
            hull.add(point);
        }
        hull.remove(hull.size()-1);
        if(hull.size()<3)return;
        float thickness=drawHint?5f:3f;
        var outer=new java.util.ArrayList<org.joml.Vector3f>(hull.size());
        for(int i=0;i<hull.size();i++){
            var previous=hull.get((i+hull.size()-1)%hull.size());
            var point=hull.get(i);
            var next=hull.get((i+1)%hull.size());
            var before=new org.joml.Vector2f(point.y-previous.y,previous.x-point.x).normalize();
            var after=new org.joml.Vector2f(next.y-point.y,point.x-next.x).normalize();
            var miter=new org.joml.Vector2f(before).add(after).normalize();
            float distance=thickness/Math.max(.25f,miter.dot(after));
            outer.add(new org.joml.Vector3f(point.x+miter.x*distance,point.y+miter.y*distance,point.z));
        }
        var vertices=new java.util.ArrayList<org.joml.Vector3f>(hull.size()*4);
        for(int i=0;i<hull.size();i++){
            int next=(i+1)%hull.size();
            // The hull is counterclockwise; walk outwards first to keep the border front-facing.
            vertices.add(inverse.transformProject(new org.joml.Vector3f(hull.get(i).x/halfWidth,hull.get(i).y/halfHeight,hull.get(i).z)));
            vertices.add(inverse.transformProject(new org.joml.Vector3f(outer.get(i).x/halfWidth,outer.get(i).y/halfHeight,outer.get(i).z)));
            vertices.add(inverse.transformProject(new org.joml.Vector3f(outer.get(next).x/halfWidth,outer.get(next).y/halfHeight,outer.get(next).z)));
            vertices.add(inverse.transformProject(new org.joml.Vector3f(hull.get(next).x/halfWidth,hull.get(next).y/halfHeight,hull.get(next).z)));
        }
        SILHOUETTES.add(new Silhouette(
            new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(stack.last().pose()),
            new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix()),vertices,pixel,color));
    }
    private static float cross(org.joml.Vector3f a,org.joml.Vector3f b,org.joml.Vector3f c){
        return (b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);
    }
    public static float height(int count){return count<=0?0:VisualPose.CARD_HEIGHT/4*count/108f;}
    public static void render(PoseStack stack,MultiBufferSource buffers,int light,int count){
        if(count<=0)return;
        float w=VisualPose.CARD_WIDTH/2,l=VisualPose.CARD_HEIGHT/2,h=height(count);
        var out=buffers.getBuffer(CardAtlas.MATERIAL);
        face(stack,out,light,CardAtlas.BACK_SLOT,new float[]{-w,-h,-l,-w,-h,l,w,-h,l,w,-h,-l},0,-1,0,1);
        face(stack,out,light,0,new float[]{-w,0,l,-w,0,-l,w,0,-l,w,0,l},0,1,0,1);
        float fraction=Math.min(1,count/108f);
        float side=fraction*31f/95;
        face(stack,out,light,CardAtlas.SIDES_SLOT,new float[]{-w,-h,-l,w,-h,-l,w,0,-l,-w,0,-l},0,0,-1,side);
        face(stack,out,light,CardAtlas.SIDES_SLOT,new float[]{w,-h,l,-w,-h,l,-w,0,l,w,0,l},0,0,1,side);
        face(stack,out,light,CardAtlas.SIDES_SLOT,new float[]{-w,-h,l,-w,-h,-l,-w,0,-l,-w,0,l},-1,0,0,side);
        face(stack,out,light,CardAtlas.SIDES_SLOT,new float[]{w,-h,-l,w,-h,l,w,0,l,w,0,-l},1,0,0,side);
    }
    public static void card(PoseStack stack,MultiBufferSource buffers,int light){
        float w=VisualPose.CARD_WIDTH/2,l=VisualPose.CARD_HEIGHT/2;
        var out=buffers.getBuffer(CardAtlas.MATERIAL);
        face(stack,out,light,CardAtlas.BACK_SLOT,new float[]{-w,0,-l,-w,0,l,w,0,l,w,0,-l},0,-1,0,1);
    }
    private static void face(PoseStack stack,VertexConsumer out,int light,int slot,float[] xyz,float nx,float ny,float nz,float vFraction){
        float u0=CardAtlas.u0(slot),u1=CardAtlas.u1(slot),v0=CardAtlas.v0(slot),v1=v0+(CardAtlas.v1(slot)-v0)*vFraction;
        for(int i=0;i<4;i++){
            float u=ny==0?(i==0||i==3?u0:u1):(i<2?u0:u1);
            float v=ny==0?(i<2?v0:v1):(i==0||i==3?v0:v1);
            //? if >=1.21.1 {
            /*out.addVertex(stack.last(),xyz[i*3],xyz[i*3+1],xyz[i*3+2]).setColor(-1).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(stack.last(),nx,ny,nz);
            *///?} else {
            out.vertex(stack.last().pose(),xyz[i*3],xyz[i*3+1],xyz[i*3+2]).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(),nx,ny,nz).endVertex();
            //?}
        }
    }
}
