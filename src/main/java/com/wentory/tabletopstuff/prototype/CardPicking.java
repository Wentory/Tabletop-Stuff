package com.wentory.tabletopstuff.prototype;

import java.util.List;

/** Picks the original unanimated geometry, including texture transparency and occlusion. */
public final class CardPicking {
    @FunctionalInterface
    public interface FaceMask { boolean opaque(int index, float u, float v); }

    public static int pick(List<HandLayout.Card> cards, double ox, double oy, double oz,
            double dx, double dy, double dz, FaceMask mask) {
        if (!Double.isFinite(dx + dy + dz + ox + oy + oz) || dz >= -1e-10) return -1;
        int picked = -1;
        double closest = Double.POSITIVE_INFINITY;
        for (var card : cards) {
            double t = (card.z() + VisualPose.CARD_THICKNESS / 2 - oz) / dz;
            if (t < 0 || t >= closest) continue;
            double x = ox + t * dx - card.x(), y = oy + t * dy - card.y();
            double sin = Math.sin(card.roll()), cos = Math.cos(card.roll());
            double localX = x * cos + y * sin;
            double localY = -x * sin + y * cos - HandLayout.GRIP_INSET;
            if (Math.abs(localX) > VisualPose.CARD_WIDTH / 2 || localY < -VisualPose.CARD_HEIGHT || localY > 0) continue;
            float u = (float) (0.5 - localX / VisualPose.CARD_WIDTH);
            float v = (float) (1 + localY / VisualPose.CARD_HEIGHT);
            if (!mask.opaque(card.index(), u, v)) continue;
            closest = t;
            picked = card.index();
        }
        return picked;
    }
    private CardPicking() {}
}
