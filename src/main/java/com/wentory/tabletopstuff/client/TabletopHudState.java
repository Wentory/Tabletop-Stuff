package com.wentory.tabletopstuff.client;

/** Frame-rate-independent HUD transitions. No Minecraft or loader dependencies. */
public final class TabletopHudState {
    private static final long HEALTH_DURATION = 5_000_000_000L;
    private static final float SLIDE_SECONDS = 0.30f;
    private long lastFrame;
    private long healthUntil;
    private float lastHealth;
    private boolean active;
    private boolean initialized;
    private float hotbar = 1;
    private float health;
    private float food;

    public void update(boolean playing, float currentHealth, int foodLevel, long now) {
        float elapsed = initialized ? (float) Math.max(0, (now - lastFrame) / 1_000_000_000.0) : 0;
        lastFrame = now;
        if (playing && !active) {
            lastHealth = currentHealth;
            healthUntil = 0;
            health = 0;
            food = 0;
        } else if (playing && Float.compare(currentHealth, lastHealth) != 0) {
            healthUntil = now + HEALTH_DURATION;
        }
        lastHealth = currentHealth;
        active = playing;
        initialized = true;
        float step = elapsed / SLIDE_SECONDS;
        hotbar = approach(hotbar, playing ? 0 : 1, step);
        health = approach(health, playing && now < healthUntil ? 1 : 0, step);
        food = approach(food, playing && foodLevel <= 10 ? 1 : 0, step);
    }

    private static float approach(float current, float target, float step) {
        return current < target ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    public float visibility(String layer) {
        return switch (layer) {
            case "hotbar" -> hotbar;
            case "player_health" -> health;
            case "food_level" -> food;
            default -> 0;
        };
    }

    public static float slide(float visibility, int screenHeight) {
        float eased = visibility * visibility * (3 - 2 * visibility);
        return (1 - eased) * screenHeight;
    }
}
