package com.wentory.tabletopstuff.prototype;

import java.util.ArrayList;
import java.util.List;

/** Shared geometry calculation, independent of Minecraft, loaders and rendering APIs. */
public final class HandLayout {
    public static final int FIRST_ROW_CAPACITY = 16;
    public static final int ROW_CAPACITY_INCREMENT = 2;
    public record Card(float x, float y, float z, float roll, int index, int row) {}
    // The grip lies inside the lower part of each card, not below its bottom edge.
    public static final float GRIP_INSET = 0.45f * 0.95f;
    private static final float TOP_RADIUS = VisualPose.CARD_HEIGHT - GRIP_INSET;
    private static final float MIN_GRIP_RADIUS = VisualPose.CARD_WIDTH * 0.20f;
    private static final float CORNER_SPACING = VisualPose.CARD_WIDTH / 4 + 0.04f;
    private static final float MAX_HALF_ANGLE = (float) Math.toRadians(45);
    private static final float PREFERRED_ANGLE_STEP = 2 * (float) Math.asin(
        CORNER_SPACING / (2 * (TOP_RADIUS + MIN_GRIP_RADIUS)));
    private static final float CARD_DEPTH_STEP = VisualPose.CARD_THICKNESS + 0.0002f;

    public static int rowCapacity(int row) {
        if (row < 0) throw new IllegalArgumentException("Negative row");
        return Math.addExact(FIRST_ROW_CAPACITY, Math.multiplyExact(row, ROW_CAPACITY_INCREMENT));
    }

    public static List<Card> arrange(int count) {
        if (count < 0) throw new IllegalArgumentException("Negative hand size");
        if (count == 0) return List.of();
        List<Card> cards = new ArrayList<>(count);
        float rowDepth = 0;
        int previousSize = 0;
        for (int start = 0, row = 0; start < count; row++) {
            int size = Math.min(rowCapacity(row), count - start);
            float middle = (size - 1) / 2f;
            float angleStep = size > 1 ? Math.min(PREFERRED_ANGLE_STEP, MAX_HALF_ANGLE / middle) : 0;
            // Chord spacing at the top of the card determines radius, not an ever wider angle.
            float topRadius = size > 1 ? CORNER_SPACING / (2 * (float) Math.sin(angleStep / 2)) : TOP_RADIUS;
            float gripRadius = size > 1 ? Math.max(MIN_GRIP_RADIUS, topRadius - TOP_RADIUS) : 0;
            // Increase the arc radius until foreground cards leave the upper-left symbol clear.
            if (size > 1) {
                float low = gripRadius;
                float high = gripRadius;
                while (!readableRow(size, angleStep, high)) high = high * 1.25f + 0.1f;
                for (int pass = 0; pass < 12; pass++) {
                    float candidate = (low + high) / 2;
                    if (readableRow(size, angleStep, candidate)) high = candidate;
                    else low = candidate;
                }
                gripRadius = high;
            }
            float rowLift = 0;
            if (row > 0) {
                // Reserve the whole corner box above the silhouettes of all earlier rows.
                for (int column = 0; column < size; column++) {
                    Card candidate = arcCard(column, middle, angleStep, gripRadius);
                    float[] corner = corner(candidate);
                    for (Card foreground : cards) rowLift = Math.max(rowLift, requiredLift(corner, outline(foreground)));
                }
                rowLift += VisualPose.CARD_HEIGHT / 96 * 2;
            }
            if (row > 0) rowDepth += (previousSize + size) * 0.5f * CARD_DEPTH_STEP;
            for (int column = 0; column < size; column++) {
                float offset = column - middle;
                float angle = offset * angleStep;
                // Move card bases around a circular arc whose middle remains anchored at the palm.
                float x = gripRadius * (float) Math.sin(angle);
                float y = gripRadius * (1 - (float) Math.cos(angle)) - rowLift;
                // Keep each complete row behind every card in the preceding row.
                float z = -0.08f - rowDepth - offset * CARD_DEPTH_STEP;
                cards.add(new Card(x, y, z, angle, start + column, row));
            }
            start += size;
            previousSize = size;
        }
        return List.copyOf(cards);
    }

    private static Card arcCard(int column, float middle, float angleStep, float radius) {
        float angle = (column - middle) * angleStep;
        return new Card(radius * (float) Math.sin(angle), radius * (1 - (float) Math.cos(angle)), 0, angle, column, 0);
    }

    private static boolean readableRow(int size, float step, float radius) {
        float middle = (size - 1) / 2f;
        for (int i = 1; i < size; i++) {
            float[] symbol = corner(arcCard(i, middle, step, radius));
            for (int j = 0; j < i; j++) if (overlaps(symbol, outline(arcCard(j, middle, step, radius)))) return false;
        }
        return true;
    }

    private static float[] outline(Card c) {
        return rectangle(c, -VisualPose.CARD_WIDTH / 2, -VisualPose.CARD_HEIGHT, VisualPose.CARD_WIDTH / 2, 0);
    }

    private static float[] corner(Card c) {
        // Texture U=0 is positive model X. Protect a quarter-card corner, including a margin.
        float w = VisualPose.CARD_WIDTH, h = VisualPose.CARD_HEIGHT;
        return rectangle(c, w * (0.5f - 0.3125f), -h * (1 - 0.03125f),
            w * (0.5f - 0.0625f), -h * (1 - 0.28125f));
    }

    private static float[] rectangle(Card c, float x0, float y0, float x1, float y1) {
        float[] p = {x0,y0,x1,y0,x1,y1,x0,y1};
        float sin = (float) Math.sin(c.roll), cos = (float) Math.cos(c.roll);
        for (int i = 0; i < 8; i += 2) {
            float x = p[i], y = p[i + 1] + GRIP_INSET;
            p[i] = c.x + x * cos - y * sin;
            p[i + 1] = c.y + x * sin + y * cos;
        }
        return p;
    }

    private static boolean overlaps(float[] a, float[] b) {
        for (float[] polygon : new float[][] {a, b}) for (int i = 0; i < 8; i += 2) {
            int j = (i + 2) % 8;
            float nx = polygon[j + 1] - polygon[i + 1], ny = polygon[i] - polygon[j];
            float aMin = Float.POSITIVE_INFINITY, aMax = Float.NEGATIVE_INFINITY;
            float bMin = Float.POSITIVE_INFINITY, bMax = Float.NEGATIVE_INFINITY;
            for (int k = 0; k < 8; k += 2) {
                float ap = a[k] * nx + a[k + 1] * ny, bp = b[k] * nx + b[k + 1] * ny;
                aMin = Math.min(aMin, ap); aMax = Math.max(aMax, ap);
                bMin = Math.min(bMin, bp); bMax = Math.max(bMax, bp);
            }
            if (aMax <= bMin || bMax <= aMin) return false;
        }
        return true;
    }

    private static float requiredLift(float[] digit, float[] foreground) {
        float lift = 0;
        for (float[] polygon : new float[][] {digit, foreground}) for (int i = 0; i < 8; i += 2) {
            float bottom = verticalEdge(digit, polygon[i], true);
            float top = verticalEdge(foreground, polygon[i], false);
            if (Float.isFinite(bottom) && Float.isFinite(top)) lift = Math.max(lift, bottom - top);
        }
        return lift;
    }

    private static float verticalEdge(float[] p, float x, boolean bottom) {
        float value = bottom ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        for (int i = 0; i < 8; i += 2) {
            int j = (i + 2) % 8;
            float dx = p[j] - p[i];
            if (x < Math.min(p[i], p[j]) || x > Math.max(p[i], p[j])) continue;
            if (Math.abs(dx) < 0.000001f) {
                float y = bottom ? Math.max(p[i + 1], p[j + 1]) : Math.min(p[i + 1], p[j + 1]);
                value = bottom ? Math.max(value, y) : Math.min(value, y);
            } else {
                float y = p[i + 1] + (x - p[i]) / dx * (p[j + 1] - p[i + 1]);
                value = bottom ? Math.max(value, y) : Math.min(value, y);
            }
        }
        return value;
    }

    private HandLayout() {}
}
