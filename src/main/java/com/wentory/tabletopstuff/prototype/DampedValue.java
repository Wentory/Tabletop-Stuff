package com.wentory.tabletopstuff.prototype;

/** Exact critically damped spring for a constant target over one frame. */
public final class DampedValue {
    private double position;
    private double velocity;

    public void update(double target, double seconds, double frequency, double limit) {
        double dt = Math.max(0, seconds);
        double delta = position - target;
        double step = (velocity + frequency * delta) * dt;
        double decay = Math.exp(-frequency * dt);
        position = target + (delta + step) * decay;
        velocity = (velocity - frequency * step) * decay;
        if (Math.abs(position) > limit) {
            position = Math.copySign(limit, position);
            velocity = 0;
        }
    }

    public float value() { return (float) position; }
    public void reset() { position = 0; velocity = 0; }
}
