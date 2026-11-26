package com.wentory.tabletopstuff.client;

/** Source-over for straight-alpha packed pixels, with alpha in the high byte. */
public final class PixelLayers {
    public static int over(int base, int layer) {
        int a = layer >>> 24;
        if (a == 0) return base;
        if (a == 255) return layer;
        int b = base >>> 24;
        int alpha = a * 255 + b * (255 - a);
        int result = ((alpha + 127) / 255) << 24;
        for (int shift = 0; shift < 24; shift += 8) {
            int numerator = ((layer >>> shift) & 255) * a * 255 + ((base >>> shift) & 255) * b * (255 - a);
            result |= ((numerator + alpha / 2) / alpha) << shift;
        }
        return result;
    }

    private PixelLayers() {}
}
