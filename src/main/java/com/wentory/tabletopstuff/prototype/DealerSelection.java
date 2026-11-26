package com.wentory.tabletopstuff.prototype;

import java.util.*;

/** Dealer candidates are independent of lobby ownership and turn direction. */
public final class DealerSelection {
    public static final int FIRST_HOST=0,FIRST_RPS=1,NEXT_HOST=0,NEXT_WINNER=1,NEXT_LOSER=2;
    public static List<UUID> next(int mode,UUID host,UUID winner,Map<UUID,Integer> handPoints){
        if(mode==NEXT_HOST)return List.of(host);
        if(mode==NEXT_WINNER)return List.of(handPoints.containsKey(winner)?winner:host);
        if(mode!=NEXT_LOSER)throw new IllegalArgumentException("Invalid dealer policy");
        if(handPoints.keySet().stream().noneMatch(id->!id.equals(winner)))return List.of(host);
        int maximum=handPoints.entrySet().stream().filter(e->!e.getKey().equals(winner)).mapToInt(Map.Entry::getValue).max().orElseThrow();
        return handPoints.entrySet().stream().filter(e->!e.getKey().equals(winner)&&e.getValue()==maximum).map(Map.Entry::getKey).toList();
    }
}
