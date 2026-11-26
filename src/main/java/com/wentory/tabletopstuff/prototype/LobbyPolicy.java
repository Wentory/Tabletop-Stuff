package com.wentory.tabletopstuff.prototype;

import java.util.*;

/** Shared, independently testable access and reservation policy. */
public final class LobbyPolicy {
    public static boolean allowsEntry(boolean inviteOnly, boolean kicked, boolean invited) {
        return !(inviteOnly || kicked) || invited;
    }
    public static boolean discoverable(boolean sameLevel, boolean alive, boolean spectator, boolean invisible, boolean member, double distanceSquared) {
        return sameLevel && alive && !spectator && !invisible && !member && distanceSquared >= 0 && distanceSquared <= 32*32;
    }
    public static Optional<Map<UUID,Integer>> assign(List<UUID> members, Map<UUID,Integer> choices, List<Integer> available) {
        Map<UUID,Integer> assigned = new LinkedHashMap<>(); Set<Integer> used = new HashSet<>();
        if (new HashSet<>(available).size() < members.size()) return Optional.empty();
        for (UUID member : members) {
            Integer choice = choices.get(member);
            if (choice != null) {
                if (!available.contains(choice) || !used.add(choice)) return Optional.empty();
                assigned.put(member, choice);
            }
        }
        for (UUID member : members) if (!assigned.containsKey(member)) {
            Integer free = available.stream().filter(index -> !used.contains(index)).findFirst().orElse(null);
            if (free == null) return Optional.empty();
            assigned.put(member,free); used.add(free);
        }
        return Optional.of(Map.copyOf(assigned));
    }
}
