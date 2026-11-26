package com.wentory.tabletopstuff.prototype;

/** Render-independent pose values, in radians and Minecraft model units (1/16 block). */
public final class VisualPose {
    public record Arm(float offsetX, float offsetY, float offsetZ, float pitch, float yaw, float roll) {}
    public record Grip(float x, float y, float z, float pitch, float yaw, float roll) {}

    // Wentory's saved holding_cards_corrected.bbmodel, 2026-10-30.
    // Blockbench -> renderer: (x, y, z) -> (-x, 24-y, z), rotations -> (-x, -y, z).
    // Preserve author shoulder origins at BB Y=19, Z=1, relative to vanilla Y=2.
    public static final Arm LEFT = new Arm(0, 3, 1, radians(-79.17127f), radians(18.33465f), radians(-5.72958f));
    public static final Arm RIGHT = new Arm(0, 3, 1, radians(-79.17127f), radians(-18.33465f), radians(5.72958f));
    // Translation is relative to the actual palm, not the incorrect 12-unit end in the editor template.
    public static final Grip GRIP = new Grip(0, 0, -1, radians(110.96337f), radians(-9.07197f), radians(-17.21001f));
    // Wentory's holding_cards_corrected — копия.bbmodel, 2026-10-30.
    public static final Arm HIDDEN_LEFT = new Arm(0, 0, 4, radians(-53.19635f), radians(33.46409f), radians(-10.37698f));
    // The author's right-arm cube is shifted -3 along local Z relative to its group.
    public static final Arm HIDDEN_RIGHT = shifted(new Arm(0, 1, 3, radians(-53.19635f), radians(-33.46409f), radians(10.37698f)), -3);
    public static final Grip HIDDEN_GRIP = new Grip(0, 0, -1, radians(45.96337f), radians(-9.07197f), radians(-17.21001f));
    // Wentory's shuffling_deck.bbmodel: group rotation composed with the cube's local Y=35°.
    public static final Arm SHUFFLE_LEFT = new Arm(0,3,1,radians(-86.86348f),radians(12.54249f),radians(28.73136f));
    public static final Arm SHUFFLE_RIGHT = new Arm(0,4,0,radians(-107.5f),radians(-22),radians(8));
    public static final Grip SHUFFLE_GRIP = new Grip(-.99268f,8.0476f,-1.21987f,
        radians(97.59586f),radians(48.74982f),radians(5.72541f));
    public static final float CARD_WIDTH = 2.8f * 0.95f;
    public static final float CARD_HEIGHT = 4.2f * 0.95f;
    // 0.004 model units = 0.25 mm at Minecraft scale, shared with layout spacing.
    public static final float CARD_THICKNESS = 0.002f;
    public static final float MAX_YAW = 16;
    public static final float MAX_PITCH = 10;

    private static float radians(float degrees) { return (float) Math.toRadians(degrees); }

    private static Arm shifted(Arm arm, float localZ) {
        double sx = Math.sin(arm.pitch), cx = Math.cos(arm.pitch);
        double sy = Math.sin(arm.yaw), cy = Math.cos(arm.yaw);
        double sz = Math.sin(arm.roll), cz = Math.cos(arm.roll);
        return new Arm(arm.offsetX + (float)(localZ*(cz*sy*cx+sz*sx)),
            arm.offsetY + (float)(localZ*(sz*sy*cx-cz*sx)), arm.offsetZ + (float)(localZ*cy*cx), arm.pitch, arm.yaw, arm.roll);
    }

    public static float cursorOffset(double coordinate, double extent, float limit) {
        if (extent <= 0) return 0;
        return (float) Math.max(-1, Math.min(1, 2 * coordinate / extent - 1)) * limit;
    }

    private VisualPose() {}
}
