package com.wentory.tabletopstuff.nullgame;

import java.util.*;

/** Real card identities and preparation rules, independent of Minecraft and rendering. */
public final class DealPreparation {
    private static final class Hand {
        final List<Integer> pile=new ArrayList<>(), hand=new ArrayList<>(), pileIndices=new ArrayList<>();
        int delivered;
    }
    private final List<Integer> deck=new ArrayList<>(),discard=new ArrayList<>();
    private final Map<UUID,Hand> players=new LinkedHashMap<>();
    private final Random random;
    private int insertion,recycleSerial;
    public DealPreparation(List<UUID> participants,Random random){
        if(participants.isEmpty()||participants.size()>8||new HashSet<>(participants).size()!=participants.size())throw new IllegalArgumentException("Invalid participants");
        this.random=random;participants.forEach(id->players.put(id,new Hand()));
        for(int i=0;i<108;i++)deck.add(i);
        Collections.shuffle(deck,random);
    }
    /** Lift the top packet and insert it at a randomly chosen depth in the remainder. */
    public int shufflePacket(){
        if(deck.size()<2)return 0;
        int size=1+random.nextInt(Math.min(10,deck.size()-1));
        List<Integer> packet=new ArrayList<>(deck.subList(0,size));
        deck.subList(0,size).clear();insertion=1+random.nextInt(deck.size());
        deck.addAll(insertion,packet);return size;
    }
    public int insertionDepth(){return insertion;}
    public boolean deal(UUID target){
        Hand hand=players.get(target);if(hand==null||hand.delivered>=7||deck.isEmpty())return false;
        hand.pile.add(deck.remove(0));hand.pileIndices.add(hand.delivered++);return true;
    }
    public int take(UUID player){
        return take(player,false);
    }
    /** Transfer one physical card; NullRound owns turn legality. */
    public boolean draw(UUID player){
        Hand hand=players.get(player);if(hand==null)return false;ensureDeck();if(deck.isEmpty())return false;
        hand.hand.add(deck.remove(0));return true;
    }
    public int take(UUID player,boolean leaveNewest){
        Hand hand=players.get(player);if(hand==null)return 0;
        int count=Math.max(0,hand.pile.size()-(leaveNewest?1:0));
        hand.hand.addAll(hand.pile.subList(0,count));hand.pile.subList(0,count).clear();hand.pileIndices.subList(0,count).clear();return count;
    }
    public int play(UUID player,int index){
        Hand hand=players.get(player);if(hand==null||index<0||index>=hand.hand.size())return -1;
        int token=hand.hand.remove(index);discard.add(token);return NullDeck.classic().get(token).faceSlot();
    }
    public int discarded(){return discard.size();}
    public void returnHand(UUID player){
        Hand hand=players.get(player);if(hand==null)return;
        List<Integer> returned=new ArrayList<>(hand.hand);returned.addAll(hand.pile);Collections.shuffle(returned,random);deck.addAll(returned);
        hand.hand.clear();hand.pile.clear();hand.pileIndices.clear();
    }
    private void ensureDeck(){
        if(!deck.isEmpty()||discard.size()<2)return;
        recycle(1);
    }
    /** Called once the last thrown card has landed; retain the three visible top cards. */
    public boolean collectDiscard(){if(discard.size()<30)return false;recycle(3);return true;}
    private void recycle(int keep){
        int count=discard.size()-keep;
        List<Integer> returned=new ArrayList<>(discard.subList(0,count));
        Collections.shuffle(returned,random);deck.addAll(returned);discard.subList(0,count).clear();recycleSerial++;
    }
    public void refill(){ensureDeck();}
    public int recycleSerial(){return recycleSerial;}
    public int opening(){
        if(!discard.isEmpty()||deck.isEmpty())throw new IllegalStateException("Opening already dealt");
        while(NullDeck.classic().get(deck.get(0)).kind()==NullCard.Kind.WILD_DRAW_FOUR){int token=deck.remove(0);deck.add(token);Collections.shuffle(deck,random);}
        int token=deck.remove(0);discard.add(token);return NullDeck.classic().get(token).faceSlot();
    }
    public int topFace(){return discard.isEmpty()?-1:NullDeck.classic().get(discard.get(discard.size()-1)).faceSlot();}
    public int remaining(){return deck.size();}
    public int delivered(UUID id){Hand hand=players.get(id);return hand==null?0:hand.delivered;}
    public int[] pileIndices(UUID id){return players.get(id).pileIndices.stream().mapToInt(Integer::intValue).toArray();}
    public int[] handFaces(UUID id){return players.get(id).hand.stream().mapToInt(i->NullDeck.classic().get(i).faceSlot()).toArray();}
    public boolean allDealt(){return players.values().stream().allMatch(h->h.delivered==7);}
    /** Internal audit/testing only; this order is never included in the client snapshot. */
    public List<Integer> auditDeck(){return List.copyOf(deck);}
    public List<Integer> auditCards(){List<Integer> all=new ArrayList<>(deck);all.addAll(discard);players.values().forEach(h->{all.addAll(h.pile);all.addAll(h.hand);});return List.copyOf(all);}
}
