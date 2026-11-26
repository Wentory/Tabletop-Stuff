package com.wentory.tabletopstuff.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.BitSet;
import com.wentory.tabletopstuff.nullgame.NullDeck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Bake author layers once per resource reload; render all card faces using one texture. */
public final class CardAtlas extends SimplePreparableReloadListener<NativeImage> {
    public static final int COLUMNS = 8;
    public static final int ROWS = 8;
    public static final int CELL_WIDTH = 64;
    public static final int CELL_HEIGHT = 96;
    public static final int SYMBOL_COUNT = 15;
    public static final int BACK_SLOT = 54;
    public static final int EDGE_SLOT = 55;
    public static final int SIDES_SLOT = 56;
    private static final String[] COLORS = {"red", "blue", "green", "yellow"};
    private static final ResourceLocation LOCATION = location("dynamic/cards/null_classic");
    public static final RenderType MATERIAL = RenderType.entityCutoutNoCull(LOCATION);
    public static final RenderType TRANSLUCENT_MATERIAL = localTranslucent();
    private static BitSet[] masks;

    private static RenderType localTranslucent() {
        RenderType base = RenderType.entityTranslucentCull(LOCATION);
        // The hidden hand is drawn nearest plane first with depth writes. Do not
        // sort it back-to-front: overlapping cards would accumulate back to opaque.
        return new RenderType("tabletop_hidden_hand", base.format(), base.mode(), base.bufferSize(), false, false,
            base::setupRenderState, base::clearRenderState) {};
    }

    private static ResourceLocation location(String path) {
        //? if >=1.21.1 {
        /*return ResourceLocation.fromNamespaceAndPath("tabletop_stuff", path);
        *///?} else {
        return new ResourceLocation("tabletop_stuff", path);
        //?}
    }

    private static NativeImage read(ResourceManager manager, String name, int width, int height) throws IOException {
        try (var stream = manager.open(location("textures/cards/null/classic/" + name + ".png"))) {
            NativeImage image = NativeImage.read(stream);
            if (image.getWidth() != width || image.getHeight() != height) {
                image.close();
                throw new IOException(name + ".png must be " + width + "x" + height);
            }
            return image;
        }
    }

    @Override
    protected NativeImage prepare(ResourceManager manager, ProfilerFiller profiler) {
        NativeImage atlas = new NativeImage(COLUMNS * CELL_WIDTH, ROWS * CELL_HEIGHT, true);
        try (NativeImage symbols = read(manager, "symbols", 256, 384)) {
            for (int color = 0; color < COLORS.length; color++) {
                try (NativeImage base = read(manager, COLORS[color], 128, 96)) {
                    for (int symbol = 0; symbol < 13; symbol++) bake(atlas, base, symbols, color * 13 + symbol, symbol);
                }
            }
            try (NativeImage wild = read(manager, "wild", 128, 96)) {
                bake(atlas, wild, symbols, 52, 14);
                bake(atlas, wild, symbols, 53, 13);
            }
            try (NativeImage back = read(manager, "back", CELL_WIDTH, CELL_HEIGHT)) {
                for (int y = 0; y < CELL_HEIGHT; y++) for (int x = 0; x < CELL_WIDTH; x++) {
                    atlas.setPixelRGBA(BACK_SLOT % COLUMNS * CELL_WIDTH + x, BACK_SLOT / COLUMNS * CELL_HEIGHT + y,
                        back.getPixelRGBA(x, y));
                    atlas.setPixelRGBA(EDGE_SLOT % COLUMNS * CELL_WIDTH + x, EDGE_SLOT / COLUMNS * CELL_HEIGHT + y, -1);
                }
            }
            try(var stream=manager.open(location("textures/block/null_deck/sides.png"));NativeImage sides=NativeImage.read(stream)){
                if(sides.getWidth()!=64||sides.getHeight()!=32)throw new IOException("Deck sides must be 64x32");
                for(int y=0;y<32;y++)for(int x=0;x<64;x++)atlas.setPixelRGBA(SIDES_SLOT%COLUMNS*CELL_WIDTH+x,SIDES_SLOT/COLUMNS*CELL_HEIGHT+y,sides.getPixelRGBA(x,y));
            }
            return atlas;
        } catch (IOException | RuntimeException error) {
            atlas.close();
            throw new IllegalStateException("Cannot bake NULL card atlas", error);
        }
    }

    private static void bake(NativeImage atlas, NativeImage base, NativeImage symbols, int slot, int symbol) {
        int atlasX = slot % COLUMNS * CELL_WIDTH, atlasY = slot / COLUMNS * CELL_HEIGHT;
        for (int y = 0; y < CELL_HEIGHT; y++) for (int x = 0; x < CELL_WIDTH; x++) {
            int pixel = PixelLayers.over(base.getPixelRGBA(x, y), base.getPixelRGBA(x + CELL_WIDTH, y));
            pixel = PixelLayers.over(pixel, symbols.getPixelRGBA(symbol % 4 * CELL_WIDTH + x, symbol / 4 * CELL_HEIGHT + y));
            atlas.setPixelRGBA(atlasX + x, atlasY + y, pixel);
        }
    }

    @Override
    protected void apply(NativeImage image, ResourceManager manager, ProfilerFiller profiler) {
        BitSet[] nextMasks = new BitSet[BACK_SLOT];
        for (int slot = 0; slot < BACK_SLOT; slot++) {
            BitSet mask = new BitSet(CELL_WIDTH * CELL_HEIGHT);
            int ax = slot % COLUMNS * CELL_WIDTH, ay = slot / COLUMNS * CELL_HEIGHT;
            for (int y = 0; y < CELL_HEIGHT; y++) for (int x = 0; x < CELL_WIDTH; x++) {
                if ((image.getPixelRGBA(ax + x, ay + y) >>> 24) >= 26) mask.set(y * CELL_WIDTH + x);
            }
            nextMasks[slot] = mask;
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, false);
        // TextureManager closes the previous atlas on replacement, including its native image.
        Minecraft.getInstance().getTextureManager().register(LOCATION, texture);
        masks = nextMasks;
    }

    public static boolean opaque(int index, float u, float v) {
        return opaqueSlot(frontSlot(index),u,v);
    }
    public static boolean opaqueSlot(int slot,float u,float v){
        if (masks == null) return true;
        int x = Math.max(0, Math.min(CELL_WIDTH - 1, (int) (u * CELL_WIDTH)));
        int y = Math.max(0, Math.min(CELL_HEIGHT - 1, (int) (v * CELL_HEIGHT)));
        return masks[slot].get(y * CELL_WIDTH + x);
    }

    public static int frontSlot(int cardIndex) { return NullDeck.previewCard(cardIndex).faceSlot(); }
    public static float u0(int slot) { return (slot % COLUMNS * CELL_WIDTH + 0.5f) / (COLUMNS * CELL_WIDTH); }
    public static float v0(int slot) { return (slot / COLUMNS * CELL_HEIGHT + 0.5f) / (ROWS * CELL_HEIGHT); }
    public static float u1(int slot) { return (slot % COLUMNS * CELL_WIDTH + CELL_WIDTH - 0.5f) / (COLUMNS * CELL_WIDTH); }
    public static float v1(int slot) { return (slot / COLUMNS * CELL_HEIGHT + CELL_HEIGHT - 0.5f) / (ROWS * CELL_HEIGHT); }
}
