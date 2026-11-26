package com.wentory.tabletopstuff.prototype;

import java.util.*;

/** Fixed electorate and host weight for one server-owned cancellation ballot. */
public final class CancellationVote {
    private final Map<UUID,Integer> weights=new LinkedHashMap<>();
    private final Map<UUID,Boolean> votes=new HashMap<>();
    private final long deadline;
    public CancellationVote(Collection<UUID> players,UUID host,long tick,int seconds){
        if(players.isEmpty()||seconds<=0)throw new IllegalArgumentException("Invalid vote");
        for(UUID id:players)weights.put(id,id.equals(host)?2:1);
        deadline=tick+seconds*20L;
    }
    public boolean choose(UUID player,boolean yes,long tick){
        if(finished(tick)||!weights.containsKey(player)||votes.containsKey(player))return false;
        votes.put(player,yes);return true;
    }
    public int yes(){return weights.entrySet().stream().filter(e->Boolean.TRUE.equals(votes.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();}
    public int no(){return weights.entrySet().stream().filter(e->Boolean.FALSE.equals(votes.get(e.getKey()))).mapToInt(Map.Entry::getValue).sum();}
    public int total(){return weights.values().stream().mapToInt(Integer::intValue).sum();}
    public int choice(UUID id){return !votes.containsKey(id)?-1:votes.get(id)?1:0;}
    public boolean passed(){return yes()>total()/2;}
    public boolean finished(long tick){return passed()||no()>=(total()+1)/2||votes.size()==weights.size()||tick>=deadline;}
    public int remaining(long tick){return (int)Math.max(0,(deadline-tick+19)/20);}
}
