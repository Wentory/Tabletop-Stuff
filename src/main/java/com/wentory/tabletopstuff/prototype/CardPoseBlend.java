package com.wentory.tabletopstuff.prototype;

/** Time-based pose transition, shared by all render passes and independent of frame rate. */
public final class CardPoseBlend {
    private static final double DURATION_NANOS = 450_000_000.0;
    private boolean hidden;
    private float from;
    private long started;

    public CardPoseBlend(boolean hidden) { this.hidden = hidden; from = hidden ? 1 : 0; }
    public void target(boolean hidden, long now) {
        if (this.hidden == hidden) return;
        from = value(now); started = now; this.hidden = hidden;
    }
    public float value(long now) {
        float t = (float)Math.max(0, Math.min(1, (now-started)/DURATION_NANOS));
        float eased = t*t*t*(t*(t*6-15)+10);
        return from + ((hidden ? 1 : 0)-from)*eased;
    }
}
