package com.wentory.tabletopstuff.nullgame;

import java.util.Arrays;
import java.util.List;

public enum NullHouseRule {
    STACKING("stacking"),
    JUMP_IN("jump_in"),
    NO_U("no_you"),
    DEFLECT("deflect"),
    LAST_STANDING("last_standing");

    private final String key;

    NullHouseRule(String name) {
        key = "tabletop_stuff.rule." + name;
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(rule -> rule.key).toList();
    }

    public boolean enabled(int mask) {
        return (mask & (1 << ordinal())) != 0;
    }

    public NullHouseRule conflict(int mask) {
        if (this == STACKING && DEFLECT.enabled(mask)) return DEFLECT;
        if (this == DEFLECT && STACKING.enabled(mask)) return STACKING;
        return null;
    }

    public String key() {
        return key;
    }

    public static int toggle(int mask, int index) {
        var rules = values();
        if (index < 0 || index >= rules.length || rules[index].conflict(mask) != null) return mask;
        return mask ^ (1 << index);
    }
}
