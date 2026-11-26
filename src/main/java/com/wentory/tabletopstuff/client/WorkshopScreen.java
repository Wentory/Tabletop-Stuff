package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.ModContent;
import com.wentory.tabletopstuff.workshop.WorkshopMenu;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class WorkshopScreen extends AbstractContainerScreen<WorkshopMenu> {
    private static final int TEXT=0xFFF2EBDD,MUTED=0xFFB7B5AC,AMBER=0xFFF1C56B;
    private static final int CONTENT_WIDTH=620, SIDE_WIDTH=152, COLUMN_GAP=16, SIDEBAR_Y=101;
    private static final int CATALOG_Y=SIDEBAR_Y+39, CATALOG_ROW=32, CATALOG_ROWS=6;
    private static final int PREVIEW_X=SIDE_WIDTH+COLUMN_GAP, PREVIEW_Y=83;
    private static final int PREVIEW_WIDTH=CONTENT_WIDTH-2*PREVIEW_X, PREVIEW_HEIGHT=161;
    private static final int PREVIEW_CENTER=PREVIEW_X+PREVIEW_WIDTH/2;
    private static final int PANEL_X=PREVIEW_X, PANEL_Y=267, PANEL_WIDTH=PREVIEW_WIDTH, PANEL_RIGHT=PANEL_X+PANEL_WIDTH;
    private static final int PANEL_PADDING=12, MATERIAL_X=PANEL_X+PANEL_PADDING, MATERIAL_Y=PANEL_Y+24;
    private static final int MATERIAL_HEIGHT=36, MATERIAL_ROW_STEP=39;
    private static final int MATERIALS_PER_ROW=4;
    private record Product(String title,String style,Supplier<ItemStack> icon,List<WorkshopMenu.Material> materials,
                           int craftButton,int previewWidth,int previewHeight,Preview preview){}
    @FunctionalInterface
    private interface Preview {void draw(GuiGraphics graphics);}

    private final List<Product> products=List.of(new Product("null_deck","classic_skin",
        ()->new ItemStack(ModContent.LOBBY_ITEM.get()),WorkshopMenu.NULL_MATERIALS,0,232,150,WorkshopScreen::drawNull));
    private List<Product> filtered=products;
    private Product selected=products.get(0);
    private int catalogOffset;
    private float scale=1,rotation=-25;
    private int originX,originY;
    private boolean rotating;
    private float rotationVelocity;
    private float craftHighlight,catalogHighlight;
    private Product hoveredProduct;
    private Product pressedProduct;
    private long catalogPressedAt;
    private float selectionMarker=Float.NaN;
    private static final long OPEN_NANOS=600_000_000L,CLOSE_NANOS=380_000_000L;
    private long closingAt,craftedAt,pressedAt,selectionAt;
    private int craftRevision;
    private Product previousProduct;
    private float craftReady;
    private final java.util.Map<WorkshopMenu.Material,IngredientMotion> ingredients=new java.util.HashMap<>();
    private static final class IngredientMotion{
        int count;
        long changedAt;
        float hover;
        IngredientMotion(int count){this.count=count;}
    }
    private record Layer(int x,int y,float alpha){}
    private Layer header=new Layer(0,0,1),catalog=header,modelLayer=header,settings=header,materials=header;
    private long openedAt, lastFrame, lastDrag;
    private enum Options {
        SKINS("skins"), VARIANTS("variants");
        final String label;
        final ResourceLocation icon;
        final List<Component> choices;
        Options(String label){
            this.label=label;
            this.icon=workshopIcon(label);
            this.choices="skins".equals(label)?List.of(Component.translatable("tabletop_stuff.workshop.classic_skin")):List.of();
        }
    }
    private static final ResourceLocation WORKSHOP_ICON=workshopIcon("cube");
    private static final ResourceLocation SEARCH_ICON=workshopIcon("search");
    private static ResourceLocation workshopIcon(String name){
        String path="textures/gui/games_workshop/"+name+".png";
        //? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("tabletop_stuff",path);
        *///?} else {
        return new ResourceLocation("tabletop_stuff",path);
        //?}
    }
    private Options openOptions, visibleOptions;
    private float optionsReveal;
    private final int[] selectedOptions=new int[Options.values().length];
    private static final int OPTIONS_X=CONTENT_WIDTH-SIDE_WIDTH, OPTIONS_Y=SIDEBAR_Y, OPTION_WIDTH=SIDE_WIDTH;
    private static final int OPTION_HEIGHT=34, OPTION_STEP=OPTION_HEIGHT+1;
    private static final int CRAFT_WIDTH=72, CRAFT_X=PANEL_RIGHT-PANEL_PADDING-CRAFT_WIDTH;
    private static final int MATERIAL_WIDTH=CRAFT_X-MATERIAL_X-12;
    private EditBox search;
    private static final int SEARCH_X=0, SEARCH_Y=SIDEBAR_Y+15, SEARCH_WIDTH=SIDE_WIDTH, SEARCH_HEIGHT=18;
    private static final int SEARCH_TEXT_X=SEARCH_X+22, SEARCH_TEXT_Y=SEARCH_Y+5, SEARCH_TEXT_WIDTH=SEARCH_WIDTH-29;
    private static final int[] PREVIEW_FACES={3,25,36,53};
    public WorkshopScreen(WorkshopMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);imageWidth=CONTENT_WIDTH;imageHeight=350+extraMaterialRows()*MATERIAL_ROW_STEP;
    }
    private String tr(String key){return Component.translatable("tabletop_stuff.workshop."+key).getString();}
    private void frame(GuiGraphics g,int x,int y,int w,int h,int border,int background){
        g.fill(x,y,x+w,y+h,background);
        g.renderOutline(x,y,w,h,border);
    }
    private void text(GuiGraphics g,String value,int x,int y,int color){g.drawString(font,value,x,y,color,true);}
    private void centered(GuiGraphics g,String value,int x,int y,int color){text(g,value,x-font.width(value)/2,y,color);}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
    private static float segment(float time,float start,float end){return clamp((time-start)/(end-start));}
    private static float enter(float t){return 1-(1-t)*(1-t)*(1-t);}
    private static float exit(float t){return t*t*t;}
    private static float settle(float current,float target,float dt){
        return current+(target-current)*(1-(float)Math.exp(-18*dt));
    }
    private Layer layer(float opening,float start,float end,float closing,float outStart,float outEnd,
                        int offsetX,int offsetY,int exitX,int exitY){
        float arrival=enter(segment(opening,start,end));
        float departure=exit(segment(closing,outStart,outEnd));
        return new Layer(Math.round(offsetX*(1-arrival)+exitX*departure),
            Math.round(offsetY*(1-arrival)+exitY*departure),arrival*(1-departure));
    }
    private static void pushLayer(GuiGraphics g,Layer layer){
        g.pose().pushPose();
        g.pose().translate(layer.x(),layer.y(),0);
    }
    private static int fade(int color,float opacity){
        return (color&0xFFFFFF)|(Math.round((color>>>24)*opacity)<<24);
    }
    private static int blendColor(int start,int end,float amount){
        int red=Math.round((start>>16&255)+((end>>16&255)-(start>>16&255))*amount);
        int green=Math.round((start>>8&255)+((end>>8&255)-(start>>8&255))*amount);
        int blue=Math.round((start&255)+((end&255)-(start&255))*amount);
        return 0xFF000000|(red<<16)|(green<<8)|blue;
    }
    private void large(GuiGraphics g,String value,int x,int y,float size,int color){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);text(g,value,0,0,color);g.pose().popPose();
    }
    private boolean hit(double x,double y,int left,int top,int w,int h){return x>=left&&x<left+w&&y>=top&&y<top+h;}
    @Override protected void init(){
        super.init();
        if(openedAt==0){openedAt=System.nanoTime();craftRevision=menu.craftRevision();}
        String query=search==null?"":search.getValue();
        scale=Math.min(.9f,Math.min(width*.68f/imageWidth,height*.72f/imageHeight));
        originX=Math.round((width-imageWidth*scale)/2);originY=Math.round((height-imageHeight*scale)/2);
        Component label=Component.literal(tr("search"));
        search=addWidget(new EditBox(font,SEARCH_TEXT_X,SEARCH_TEXT_Y,SEARCH_TEXT_WIDTH,9,label));
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setMaxLength(40);
        search.setHint(label);
        search.setValue(query);
        search.setResponder(this::filter);
        filter(query);
    }
    private int extraMaterialRows(){
        return Math.max(0,products.stream().mapToInt(product->(product.materials().size()-1)/MATERIALS_PER_ROW).max().orElse(0));
    }
    private void filter(String input){
        String query=input.strip().toLowerCase(Locale.ROOT);
        filtered=products.stream().filter(product->tr(product.title()).toLowerCase(Locale.ROOT).contains(query)
            ||tr(product.style()).toLowerCase(Locale.ROOT).contains(query)).toList();
        catalogOffset=0;
        selectionMarker=Float.NaN;
        if(!filtered.isEmpty()&&!filtered.contains(selected))select(filtered.get(0));
    }
    private void select(Product product){
        if(selected==product)return;
        previousProduct=selected;
        selectionAt=System.nanoTime();
        selected=product;
        rotation=-25;
        rotationVelocity=0;
        openOptions=null;
        visibleOptions=null;
        optionsReveal=0;
    }
    private boolean scrollCatalog(double mouseX,double mouseY,double delta){
        double x=(mouseX-originX)/scale-catalog.x(),y=(mouseY-originY)/scale-catalog.y();
        if(closingAt!=0||delta==0||!hit(x,y,SEARCH_X,CATALOG_Y,SEARCH_WIDTH,CATALOG_ROWS*CATALOG_ROW))return false;
        int maxOffset=Math.max(0,filtered.size()-CATALOG_ROWS);
        catalogOffset=Math.max(0,Math.min(maxOffset,catalogOffset+(delta>0?-1:1)));
        selectionMarker=Float.NaN;
        return true;
    }
    //? if >=1.21.1 {
    /*@Override public boolean mouseScrolled(double x,double y,double dx,double dy){return scrollCatalog(x,y,dy);}
    *///?} else {
    @Override public boolean mouseScrolled(double x,double y,double delta){return scrollCatalog(x,y,delta);}
    //?}
    private int materialWidth(int index){
        int first=index-index%MATERIALS_PER_ROW;
        return MATERIAL_WIDTH/Math.min(MATERIALS_PER_ROW,selected.materials().size()-first);
    }
    private int materialX(int index){return MATERIAL_X+index%MATERIALS_PER_ROW*materialWidth(index);}
    private int materialY(int index){return MATERIAL_Y+index/MATERIALS_PER_ROW*MATERIAL_ROW_STEP;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        long now=System.nanoTime();
        float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1_000_000_000f);
        lastFrame=now;
        if(closingAt!=0&&now-closingAt>=CLOSE_NANOS){super.onClose();return;}
        float opening=clamp(((closingAt==0?now:closingAt)-openedAt)/(float)OPEN_NANOS);
        float closing=closingAt==0?0:clamp((now-closingAt)/(float)CLOSE_NANOS);
        header=layer(opening,0,.60f,closing,.24f,1,-24,-5,-35,-7);
        catalog=layer(opening,.02f,.64f,closing,0,.86f,-54,0,-64,0);
        modelLayer=layer(opening,.12f,.92f,closing,.06f,.93f,10,12,-3,-9);
        settings=layer(opening,.24f,.83f,closing,.04f,.89f,36,0,52,0);
        materials=layer(opening,.18f,.91f,closing,.02f,.87f,0,30,0,42);
        float cursorX=(mouseX-originX)/scale;
        float cursorY=(mouseY-originY)/scale;
        boolean ready=!filtered.isEmpty()&&hasMaterials();
        boolean overCraft=ready&&closingAt==0
            &&hit(cursorX-materials.x(),cursorY-materials.y(),CRAFT_X,MATERIAL_Y,CRAFT_WIDTH,MATERIAL_HEIGHT);
        craftHighlight=settle(craftHighlight,overCraft?1:0,dt);
        craftReady=settle(craftReady,ready?1:0,dt);
        if(menu.craftRevision()!=craftRevision){craftRevision=menu.craftRevision();craftedAt=now;}
        float catalogOpacity=catalog.alpha();
        if(!rotating){
            rotation+=rotationVelocity*dt;
            rotationVelocity*=(float)Math.exp(-9*dt);
        }
        float revealTarget=openOptions==visibleOptions&&openOptions!=null?1:0;
        optionsReveal+=(revealTarget-optionsReveal)*(1-(float)Math.exp(-24*dt));
        if(optionsReveal<.01f&&visibleOptions!=openOptions){
            optionsReveal=0;
            visibleOptions=openOptions;
        }

        float x=cursorX,y=cursorY;
        float backdrop=enter(opening)*(1-exit(closing));
        g.fillGradient(0,0,width,height,fade(0x84101519,backdrop),fade(0xBC101416,backdrop));
        g.pose().pushPose();
        g.pose().translate(originX,originY,400);
        g.pose().scale(scale,scale,1);
        boolean matches=!filtered.isEmpty();
        if(matches)shadePreview(g,modelLayer.alpha());
        if(header.alpha()>.025f){
            pushLayer(g,header);
            g.setColor(1,1,1,header.alpha());
            g.blit(WORKSHOP_ICON,0,1,25,24,0f,0f,25,24,25,24);
            g.setColor(1,1,1,1);
            large(g,title.getString(),33,5,1.8f,fade(TEXT,header.alpha()));
            g.fill(0,30,184,31,fade(0xFF555C59,header.alpha()));
            int accentWidth=Math.round(56*enter(segment(opening,.10f,.66f))*(1-exit(closing)));
            if(accentWidth>0)g.fill(0,29,accentWidth,31,fade(AMBER,header.alpha()));
            g.pose().popPose();
        }
        float closeOpacity=enter(segment(opening,.23f,.76f))*(1-exit(closing));
        if(closeOpacity>.025f){
            pushLayer(g,header);
            centered(g,"×",608,7,fade(hit(x-header.x(),y-header.y(),596,0,24,24)?AMBER:MUTED,closeOpacity));
            g.pose().popPose();
        }

        pushLayer(g,catalog);
        float catalogX=x-catalog.x(),catalogY=y-catalog.y();
        if(catalogOpacity>.025f)text(g,tr("catalog"),SEARCH_X,SIDEBAR_Y,fade(MUTED,catalogOpacity));
        float searchOpacity=enter(segment(opening,.25f,.70f))*(1-exit(closing));
        if(searchOpacity>.025f){
            frame(g,SEARCH_X,SEARCH_Y,SEARCH_WIDTH,SEARCH_HEIGHT,
                fade(search.isFocused()?AMBER:0xFF414743,searchOpacity),fade(0xCF161A1C,searchOpacity));
            g.setColor(1,1,1,searchOpacity);
            g.blit(SEARCH_ICON,SEARCH_X+7,SEARCH_Y+5,0f,0f,8,8,8,8);
            g.setColor(1,1,1,1);
            if(searchOpacity>.96f)search.render(g,Math.round(catalogX),Math.round(catalogY),partialTick);
            else {
                String value=search.getValue();
                String label=value.isEmpty()?tr("search"):value;
                text(g,font.plainSubstrByWidth(label,SEARCH_TEXT_WIDTH),SEARCH_TEXT_X,SEARCH_TEXT_Y,
                    fade(value.isEmpty()?MUTED:TEXT,searchOpacity));
            }
        }
        if(!matches&&searchOpacity>.025f)text(g,tr("no_results"),SEARCH_X,CATALOG_Y+8,fade(MUTED,searchOpacity));
        int hoveredRow=(int)((catalogY-CATALOG_Y)/CATALOG_ROW);
        Product nextHover=opening>=1&&closingAt==0&&hit(catalogX,catalogY,SEARCH_X,CATALOG_Y,SEARCH_WIDTH,CATALOG_ROWS*CATALOG_ROW)
            &&catalogOffset+hoveredRow<filtered.size()?filtered.get(catalogOffset+hoveredRow):null;
        if(nextHover!=null&&nextHover!=hoveredProduct){
            hoveredProduct=nextHover;
            catalogHighlight=0;
        }
        catalogHighlight+=((nextHover!=null?1:0)-catalogHighlight)*(1-(float)Math.exp(-16*dt));
        for(int row=0;row<CATALOG_ROWS&&catalogOffset+row<filtered.size();row++){
            Product product=filtered.get(catalogOffset+row);
            float rowEntrance=enter(segment(opening,.30f+row*.035f,.73f+row*.035f));
            float rowOpacity=rowEntrance*(1-exit(closing));
            if(rowOpacity<=.025f)continue;
            int top=CATALOG_Y+row*CATALOG_ROW+Math.round(7*(1-rowEntrance));
            boolean current=product==selected;
            if(current)g.fill(SEARCH_X,top,SEARCH_X+SEARCH_WIDTH,top+CATALOG_ROW-2,fade(0xEE282C29,rowOpacity));
            if(hoveredProduct==product&&catalogHighlight>.01f){
                g.fill(SEARCH_X,top,SEARCH_X+SEARCH_WIDTH,top+CATALOG_ROW-2,fade(0x70565A48,catalogHighlight*rowOpacity));
            }
            if(current){
                selectionMarker=Float.isNaN(selectionMarker)?top:settle(selectionMarker,top,dt);
                int markerY=Math.round(selectionMarker);
                g.fill(SEARCH_X,markerY,SEARCH_X+2,markerY+Math.round((CATALOG_ROW-2)*rowEntrance),fade(AMBER,rowOpacity));
            }
            if(rowOpacity>.025f){
                int pressShift=product==pressedProduct
                    ?Math.round(2*(1-segment((now-catalogPressedAt)/1_000_000_000f,0,.12f))):0;
                float iconScale=1.85f*rowOpacity;
                g.pose().pushPose();
                g.pose().translate(SEARCH_X+24-8*iconScale+pressShift,top+15-8*iconScale,0);
                g.pose().scale(iconScale,iconScale,1);
                g.renderItem(product.icon().get(),0,0);
                g.pose().popPose();
                float labelScale=1.3f;
                String label=font.plainSubstrByWidth(tr(product.title()),(int)((SEARCH_WIDTH-54)/labelScale));
                large(g,label,SEARCH_X+47+pressShift,top+9,labelScale,fade(TEXT,rowOpacity));
            }
        }
        if(filtered.size()>CATALOG_ROWS){
            int height=CATALOG_ROWS*CATALOG_ROW;
            int thumb=Math.max(8,height*CATALOG_ROWS/filtered.size());
            int top=CATALOG_Y+(height-thumb)*catalogOffset/(filtered.size()-CATALOG_ROWS);
            int trackX=SEARCH_X+SEARCH_WIDTH+3;
            g.fill(trackX,CATALOG_Y,trackX+1,CATALOG_Y+height,0xFF454B47);
            g.fill(trackX,top,trackX+2,top+thumb,MUTED);
        }
        g.pose().popPose();

        if(matches){
            float selection=segment((now-selectionAt)/1_000_000_000f,0,.34f);
            boolean outgoing=previousProduct!=null&&selection<.42f;
            Product displayed=outgoing?previousProduct:selected;
            float swap=outgoing?1-enter(selection/.42f):enter(segment(selection,.42f,1));
            if(selection>=1)previousProduct=null;
            pushLayer(g,modelLayer);
            float nameOpacity=enter(segment(opening,.23f,.82f))*(1-exit(closing))*swap;
            if(nameOpacity>.025f){
                String title=tr(displayed.title());
                large(g,title,PREVIEW_CENTER-Math.round(font.width(title)*.75f),49,1.5f,fade(TEXT,nameOpacity));
                centered(g,tr(displayed.style()),PREVIEW_CENTER,68,fade(MUTED,nameOpacity));
            }
            if(modelLayer.alpha()*swap>.025f){
                clip(g,modelLayer,PREVIEW_X,PREVIEW_Y,PREVIEW_X+PREVIEW_WIDTH,PREVIEW_Y+PREVIEW_HEIGHT);
                try{
                    preview(g,modelLayer.alpha()*swap,displayed);
                }finally{
                    g.disableScissor();
                }
            }
            float hintOpacity=enter(segment(opening,.52f,1))*(1-exit(closing));
            if(hintOpacity>.025f)
                centered(g,tr("rotate"),PREVIEW_CENTER,PREVIEW_Y+PREVIEW_HEIGHT+5,fade(rotating?AMBER:MUTED,hintOpacity));
            g.pose().popPose();

            pushLayer(g,settings);
            renderOptions(g,x-settings.x(),y-settings.y(),opening);
            g.pose().popPose();

            pushLayer(g,materials);
            renderMaterials(g,x-materials.x(),y-materials.y(),dt,now,opening,closing);
            g.pose().popPose();
        }
        g.pose().popPose();
        if(!matches||opening<1||closingAt!=0)return;
        x-=materials.x();y-=materials.y();
        for(int i=0;i<selected.materials().size();i++){
            if(hit(x,y,materialX(i),materialY(i),materialWidth(i),MATERIAL_HEIGHT)){
                g.flush();
                g.pose().pushPose();
                g.pose().translate(0,0,400);
                g.renderTooltip(font,new ItemStack(selected.materials().get(i).item()),mouseX,mouseY);
                g.pose().popPose();
                break;
            }
        }
    }
    private void clip(GuiGraphics g,Layer layer,int left,int top,int right,int bottom){
        g.enableScissor(originX+Math.round((layer.x()+left)*scale),originY+Math.round((layer.y()+top)*scale),
            originX+Math.round((layer.x()+right)*scale),originY+Math.round((layer.y()+bottom)*scale));
    }
    private void shadePreview(GuiGraphics g,float opacity){
        // A soft falloff blocks out the outline of the physical workbench behind the deck.
        var buffer=g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose=g.pose().last().pose();
        int centerColor=fade(0x66090B0E,opacity);
        int middleColor=fade(0x44090B0E,opacity);
        float centerY=170;
        float centerX=PREVIEW_CENTER;
        float radiusX=PREVIEW_WIDTH*.65f,radiusY=142;
        int segments=32;
        for(int i=0;i<segments;i++){
            double start=-i*2*Math.PI/segments,end=-(i+1)*2*Math.PI/segments;
            float x1=radiusX*(float)Math.cos(start),y1=radiusY*(float)Math.sin(start);
            float x2=radiusX*(float)Math.cos(end),y2=radiusY*(float)Math.sin(end);
            float innerX1=centerX+x1*.55f,innerY1=centerY+y1*.55f;
            float innerX2=centerX+x2*.55f,innerY2=centerY+y2*.55f;
            shadeVertex(buffer,pose,centerX,centerY,centerColor);
            shadeVertex(buffer,pose,innerX1,innerY1,middleColor);
            shadeVertex(buffer,pose,innerX2,innerY2,middleColor);
            shadeVertex(buffer,pose,centerX,centerY,centerColor);
            shadeVertex(buffer,pose,innerX1,innerY1,middleColor);
            shadeVertex(buffer,pose,centerX+x1,centerY+y1,0x00090B0E);
            shadeVertex(buffer,pose,centerX+x2,centerY+y2,0x00090B0E);
            shadeVertex(buffer,pose,innerX2,innerY2,middleColor);
        }
        g.flush();
    }
    private static void shadeVertex(VertexConsumer out,Matrix4f pose,float x,float y,int color){
        //? if >=1.21.1 {
        /*out.addVertex(pose,x,y,-1).setColor(color);
        *///?} else {
        out.vertex(pose,x,y,-1).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
        //?}
    }
    private void renderMaterials(GuiGraphics g,float mouseX,float mouseY,float dt,long now,float opening,float closing){
        if(materials.alpha()<=.025f)return;
        int extraRows=(selected.materials().size()-1)/MATERIALS_PER_ROW;
        int panelHeight=MATERIAL_Y-PANEL_Y+MATERIAL_HEIGHT+7+extraRows*MATERIAL_ROW_STEP;
        float alpha=materials.alpha();
        g.fill(PANEL_X,PANEL_Y,PANEL_RIGHT,PANEL_Y+panelHeight,fade(0xDB191B1E,alpha));
        text(g,tr("materials"),MATERIAL_X,PANEL_Y+7,fade(TEXT,alpha));
        int ruleWidth=Math.round((PANEL_WIDTH-2*PANEL_PADDING)*enter(segment(opening,.28f,.70f))*(1-exit(closing)));
        if(ruleWidth>0)g.fill(MATERIAL_X,MATERIAL_Y-5,MATERIAL_X+ruleWidth,MATERIAL_Y-4,fade(0xFF555C59,alpha));
        float success=1-segment((now-craftedAt)/1_000_000_000f,0,.34f);
        for(int i=0;i<selected.materials().size();i++){
            var material=selected.materials().get(i);
            int owned=menu.owned(material.item());
            IngredientMotion motion=ingredients.computeIfAbsent(material,key->new IngredientMotion(owned));
            if(motion.count!=owned){motion.count=owned;motion.changedAt=now;}
            float arrival=enter(segment(opening,.37f+Math.min(i,7)*.027f,.81f+Math.min(i,7)*.027f))*alpha;
            if(arrival<.035f)continue;
            int left=materialX(i),top=materialY(i),cellWidth=materialWidth(i);
            boolean hovered=closingAt==0&&hit(mouseX,mouseY,left,top,cellWidth,MATERIAL_HEIGHT);
            motion.hover=settle(motion.hover,hovered?1:0,dt);
            float change=1-segment((now-motion.changedAt)/1_000_000_000f,0,.28f);
            int center=left+cellWidth/2;
            g.fill(left+2,top,left+cellWidth-2,top+MATERIAL_HEIGHT,
                fade(0x8B494D3C,arrival*Math.max(motion.hover,success)));
            int lift=Math.round(3*motion.hover+3*success);
            g.pose().pushPose();
            g.pose().translate(center,top+10-lift,0);
            g.pose().scale(arrival,arrival,1);
            g.renderItem(new ItemStack(material.item()),-8,-8);
            g.pose().popPose();
            int countColor=blendColor(owned>=material.count()?TEXT:0xFFE2A89C,AMBER,Math.max(change,success));
            centered(g,owned+" / "+material.count(),center,top+23,fade(countColor,arrival));
        }
        float buttonAlpha=enter(segment(opening,.57f,1))*alpha;
        if(buttonAlpha<=.025f)return;
        float pressed=1-segment((now-pressedAt)/1_000_000_000f,0,.12f);
        int lift=Math.round(7*(1-buttonAlpha)+2*pressed);
        int top=MATERIAL_Y+lift;
        frame(g,CRAFT_X,top,CRAFT_WIDTH,MATERIAL_HEIGHT,fade(0xFF515753,buttonAlpha),
            fade(0xE1242726,buttonAlpha));
        float accent=Math.max(craftReady,success);
        if(accent>.01f){
            g.fill(CRAFT_X+1,top+1,CRAFT_X+CRAFT_WIDTH-1,top+MATERIAL_HEIGHT-1,
                fade(0x74595134,buttonAlpha*accent*(.3f+.7f*Math.max(craftHighlight,success))));
            int lineWidth=Math.round((CRAFT_WIDTH-2)*accent);
            g.fill(CRAFT_X+1,top+MATERIAL_HEIGHT-1,CRAFT_X+1+lineWidth,top+MATERIAL_HEIGHT,fade(AMBER,buttonAlpha*accent));
        }
        centered(g,tr("craft"),CRAFT_X+CRAFT_WIDTH/2,top+14,fade(blendColor(MUTED,AMBER,accent),buttonAlpha));
    }
    private boolean hasMaterials(){
        return selected.materials().stream().allMatch(material->menu.owned(material.item())>=material.count());
    }
    private int optionY(Options option){
        int extra=visibleOptions==Options.SKINS?Math.round(visibleOptions.choices.size()*OPTION_STEP*optionsReveal):0;
        return OPTIONS_Y+(option==Options.VARIANTS?OPTION_STEP+extra:0);
    }
    private void renderOptions(GuiGraphics g,float mouseX,float mouseY,float opening){
        for(var option:Options.values()){
            float opacity=settings.alpha()*enter(segment(opening,.24f+option.ordinal()*.10f,.85f+option.ordinal()*.09f));
            if(opacity<=.025f)continue;
            int shift=Math.round(9*(1-opacity));
            g.pose().pushPose();
            g.pose().translate(shift,0,0);
            int y=optionY(option);
            boolean enabled=!option.choices.isEmpty();
            boolean hovered=enabled&&hit(mouseX,mouseY,OPTIONS_X,y,OPTION_WIDTH,OPTION_HEIGHT);
            g.fill(OPTIONS_X,y,OPTIONS_X+OPTION_WIDTH,y+OPTION_HEIGHT,fade(hovered?0xE82C302C:0xD0181C1E,opacity));
            if(hovered)g.fill(OPTIONS_X,y,OPTIONS_X+2,y+OPTION_HEIGHT,AMBER);
            int labelColor=fade(enabled?0xFFEAA65C:0xFFB5B7B2,opacity);
            if(minecraft.getResourceManager().getResource(option.icon).isPresent()){
                g.setColor(enabled?1f:.55f,enabled?1f:.55f,enabled?1f:.55f,opacity);
                g.blit(option.icon,OPTIONS_X+5,y+8,18,17,0f,0f,25,24,25,24);
                g.setColor(1,1,1,1);
            }
            text(g,tr(option.label),OPTIONS_X+28,y+5,labelColor);
            text(g,tr(option==Options.SKINS?"classic_skin":"standard_deck"),OPTIONS_X+28,y+19,fade(0xFF9B9F99,opacity));
            if(enabled)text(g,"›",OPTIONS_X+OPTION_WIDTH-12,y+11,labelColor);
            if(option==Options.SKINS){
                g.fill(OPTIONS_X+8,y+OPTION_HEIGHT,OPTIONS_X+8+Math.round((OPTION_WIDTH-16)*opacity),y+OPTION_HEIGHT+1,fade(0xFF555C59,opacity));
            }
            g.pose().popPose();
            if(option!=visibleOptions||optionsReveal<.01f)continue;
            int top=y+OPTION_STEP;
            int bottom=top+Math.round(option.choices.size()*OPTION_STEP*optionsReveal);
            if(bottom<=top)continue;
            clip(g,settings,OPTIONS_X,top,OPTIONS_X+OPTION_WIDTH,bottom);
            try{
                for(int i=0;i<option.choices.size();i++){
                    float itemReveal=enter(segment(optionsReveal,Math.min(.35f,i*.05f),1));
                    int row=top+i*OPTION_STEP+Math.round(6*(1-itemReveal));
                    boolean selected=selectedOptions[option.ordinal()]==i;
                    boolean over=openOptions==option&&mouseY<bottom&&hit(mouseX,mouseY,OPTIONS_X+8,row,OPTION_WIDTH-8,OPTION_HEIGHT);
                    frame(g,OPTIONS_X+8,row,OPTION_WIDTH-8,OPTION_HEIGHT,fade(over?AMBER:0xFF545B55,opacity*itemReveal),fade(0xEA23282A,opacity*itemReveal));
                    if(selected)g.fill(OPTIONS_X+8,row,OPTIONS_X+10,row+OPTION_HEIGHT,AMBER);
                    if(option==Options.SKINS&&itemReveal>.1f)g.renderItem(this.selected.icon().get(),OPTIONS_X+16,row+9);
                    text(g,option.choices.get(i).getString(),OPTIONS_X+39,row+12,fade(TEXT,Math.max(.03f,opacity*itemReveal)));
                }
            }finally{
                g.disableScissor();
            }
        }
    }
    private void preview(GuiGraphics g,float visibility,Product product){
        g.flush();Lighting.setupFor3DItems();
        g.pose().pushPose();g.pose().translate(PREVIEW_CENTER,197,150);
        float fit=Math.min((float)PREVIEW_WIDTH/product.previewWidth(),(float)PREVIEW_HEIGHT/product.previewHeight());
        float size=fit*visibility;
        g.pose().scale(size,size,size);
        g.pose().mulPose(Axis.YP.rotationDegrees(rotation+20*(1-visibility)));
        product.preview().draw(g);
        g.flush();g.pose().popPose();Lighting.setupFor3DItems();
    }
    private static void drawNull(GuiGraphics g){
        for(int i=0;i<4;i++){
            float spread=i-1.5f;
            g.pose().pushPose();g.pose().translate(spread*37,11+spread*spread*4,i*2);
            g.pose().mulPose(Axis.ZP.rotationDegrees(spread*10));
            g.pose().scale(-24,24,24);
            CardRenderer.single(g.pose(),g.bufferSource(),0xF000F0,PREVIEW_FACES[i],1);
            g.pose().popPose();
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){}
    //? if >=1.21.1 {
    /*@Override public void renderBackground(GuiGraphics g,int x,int y,float partialTick){}
    @Override protected void renderBlurredBackground(float partialTick){}
    *///?}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(button!=0||closingAt!=0)return false;
        double x=(mouseX-originX)/scale,y=(mouseY-originY)/scale;
        if(hit(x-header.x(),y-header.y(),596,0,24,24)){LobbySounds.button();onClose();return true;}
        if(System.nanoTime()-openedAt<OPEN_NANOS)return false;
        double catalogX=x-catalog.x(),catalogY=y-catalog.y();
        if(hit(catalogX,catalogY,SEARCH_X,SEARCH_Y,SEARCH_WIDTH,SEARCH_HEIGHT)){
            setFocused(search);
            double fieldX=Math.max(SEARCH_TEXT_X,Math.min(SEARCH_TEXT_X+SEARCH_TEXT_WIDTH-1,catalogX));
            search.mouseClicked(fieldX,SEARCH_TEXT_Y+4,button);
            return true;
        }
        setFocused(null);
        if(filtered.isEmpty())return false;
        if(hit(catalogX,catalogY,SEARCH_X,CATALOG_Y,SEARCH_WIDTH,CATALOG_ROWS*CATALOG_ROW)){
            int row=(int)(catalogY-CATALOG_Y)/CATALOG_ROW;
            if(catalogOffset+row<filtered.size()){
                Product product=filtered.get(catalogOffset+row);
                pressedProduct=product;
                catalogPressedAt=System.nanoTime();
                if(product!=selected){select(product);LobbySounds.toggle();}
                return true;
            }
        }
        if(hit(x-materials.x(),y-materials.y(),CRAFT_X,MATERIAL_Y,CRAFT_WIDTH,MATERIAL_HEIGHT)){
            if(hasMaterials()&&minecraft.gameMode!=null){pressedAt=System.nanoTime();LobbySounds.button();minecraft.gameMode.handleInventoryButtonClick(menu.containerId,selected.craftButton());}
            return true;
        }
        for(var option:Options.values()){
            if(option.choices.isEmpty())continue;
            int yStart=optionY(option);
            if(hit(x-settings.x(),y-settings.y(),OPTIONS_X,yStart,OPTION_WIDTH,OPTION_HEIGHT)){
                openOptions=openOptions==option?null:option;
                LobbySounds.window(openOptions!=null);
                return true;
            }
            if(openOptions!=option||visibleOptions!=option||optionsReveal<.99f)continue;
            for(int i=0;i<option.choices.size();i++){
                if(hit(x-settings.x(),y-settings.y(),OPTIONS_X+8,yStart+(i+1)*OPTION_STEP,OPTION_WIDTH-8,OPTION_HEIGHT)){
                    selectedOptions[option.ordinal()]=i;
                    LobbySounds.toggle();
                    return true;
                }
            }
        }
        rotating=hit(x-modelLayer.x(),y-modelLayer.y(),PREVIEW_X,PREVIEW_Y,PREVIEW_WIDTH,PREVIEW_HEIGHT);
        if(rotating){rotationVelocity=0;lastDrag=System.nanoTime();}
        return rotating;
    }
    @Override public void onClose(){
        if(closingAt==0){
            closingAt=System.nanoTime();
            rotating=false;
            setFocused(null);
        }
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(closingAt!=0)return true;
        if(search.isFocused()&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){
            search.keyPressed(key,scan,modifiers);
            return true;
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(!rotating||button!=0)return false;
        long now=System.nanoTime();
        float delta=(float)(dx/scale);
        rotation+=delta;
        rotationVelocity=Math.max(-250,Math.min(250,delta/Math.max(.016f,(now-lastDrag)/1_000_000_000f)));
        lastDrag=now;
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button!=0||!rotating)return false;
        if(System.nanoTime()-lastDrag>90_000_000L)rotationVelocity=0;
        rotating=false;
        return true;
    }
}
