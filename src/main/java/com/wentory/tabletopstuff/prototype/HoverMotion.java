package com.wentory.tabletopstuff.prototype;

import java.util.Arrays;

/** Local offsets are never used by picking or networking. */
public final class HoverMotion {
    private float[] amounts = new float[0];
    private int hovered = -1;
    private float focus;

    public void update(int count, int target, double seconds) {
        update(count,target,-1,seconds);
    }
    public void update(int count,int target,int selected,double seconds){
        if (amounts.length != count) amounts = Arrays.copyOf(amounts, count);
        hovered = target >= 0 && target < count ? target : -1;
        float blend = (float) (1 - Math.exp(-18 * Math.max(0, seconds)));
        focus += ((hovered >= 0 || selected>=0&&selected<count ? 1 : 0) - focus) * blend;
        for (int i = 0; i < count; i++) {
            float destination = i == hovered || i==selected ? 1 : 0;
            amounts[i] += (destination - amounts[i]) * blend;
            if (Math.abs(amounts[i] - destination) < 0.0001f) amounts[i] = destination;
        }
    }
    public int hovered() { return hovered; }
    public float focus() { return focus; }
    public float offset(int index) { return amounts[index] * VisualPose.CARD_HEIGHT * 0.15f; }
    public static int outlineColor(boolean playable) { return playable ? 0xFFFFD400 : 0xFF909090; }
}
