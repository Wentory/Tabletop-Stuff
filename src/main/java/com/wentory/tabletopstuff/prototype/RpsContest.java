package com.wentory.tabletopstuff.prototype;

import java.util.*;

/** Simultaneous, secret choices. Rock=0, paper=1, scissors=2. */
public final class RpsContest {
    private List<UUID> candidates,survivors;
    private final Map<UUID,Integer> choices=new HashMap<>();
    private boolean revealed;
    private int round=1;
    public RpsContest(List<UUID> candidates){
        if(candidates.isEmpty()||candidates.size()>8||new HashSet<>(candidates).size()!=candidates.size())throw new IllegalArgumentException("Invalid RPS participants");
        this.candidates=List.copyOf(candidates);survivors=this.candidates;
    }
    public List<UUID> candidates(){return candidates;}
    public int submitted(){return choices.size();}
    public int choice(UUID id){return choices.getOrDefault(id,-1);}
    public boolean revealed(){return revealed;}
    public int round(){return round;}
    public int remaining(){return survivors.size();}
    public UUID winner(){return survivors.size()==1?survivors.get(0):null;}
    public boolean choose(UUID id,int choice){
        if(revealed||winner()!=null||!candidates.contains(id)||choice<0||choice>2||choices.putIfAbsent(id,choice)!=null)return false;
        if(choices.size()==candidates.size()){
            revealed=true;var signs=new HashSet<>(choices.values());
            if(signs.size()==2){int winning=signs.contains(0)&&signs.contains(2)?0:signs.contains(1)&&signs.contains(0)?1:2;survivors=candidates.stream().filter(p->choices.get(p)==winning).toList();}
        }
        return true;
    }
    public void nextRound(){if(!revealed||winner()!=null)return;candidates=survivors;choices.clear();revealed=false;round++;}
}
