package com.wentory.tabletopstuff.nullgame;

import java.util.*;

/** Server-authoritative NULL round with optional house rules. */
public final class NullRound {
    private final DealPreparation cards;
    private final List<UUID> order;
    private final int[] extraDebt;
    private final Set<UUID> declared=new HashSet<>();
    private final Set<UUID> retired=new HashSet<>();
    private final Set<Integer> finished=new LinkedHashSet<>();
    private final Set<Integer> emptyHands=new LinkedHashSet<>();
    private final int houseRules;
    private int loser=-1,penaltySource=-1;
    private boolean penaltyIsFour,penaltyAccepted;
    private int turn,direction=1,color,chooser=-1,afterColor=-1,debtTarget=-1,debt,afterDebt=-1;
    private int drawnIndex=-1,winner=-1,pendingWinner=-1,missed=-1,offender=-1,challenger=-1;
    private boolean drew,bluff,challengeOpen,challengeWon;
    private int[] challengeHand=new int[0];
    private UUID revealTo,lastCaller;
    private int lastSerial;

    public NullRound(DealPreparation cards,List<UUID> clockwise,UUID dealer){
        this(cards,clockwise,dealer,0);
    }

    public NullRound(DealPreparation cards,List<UUID> clockwise,UUID dealer,int houseRules){
        if(clockwise.size()<2||clockwise.size()>8||new HashSet<>(clockwise).size()!=clockwise.size()||!clockwise.contains(dealer))throw new IllegalArgumentException("Invalid NULL participants");
        this.cards=cards;this.order=List.copyOf(clockwise);extraDebt=new int[order.size()];
        this.houseRules=houseRules;
        int dealing=order.indexOf(dealer);turn=step(dealing,1);
        NullCard top=NullCard.fromFace(cards.opening());color=top.color().ordinal();
        switch(top.kind()){
            case REVERSE -> {direction=-1;turn=dealing;}
            case SKIP -> turn=step(turn,1);
            case DRAW_TWO -> sendPenalty(dealing,turn,2,false);
            case WILD -> {color=-1;chooser=turn;afterColor=turn;}
            default -> {}
        }
    }
    private int at(UUID id){return order.indexOf(id);}
    private int step(int index,int amount){
        for(int n=0;n<amount;n++)for(int attempt=0;attempt<order.size();attempt++){
            index=Math.floorMod(index+direction,order.size());if(active(index))break;
        }
        return index;
    }
    public int remainingPlayers(){return order.size()-retired.size();}
    private boolean active(int index){return index>=0&&!retired.contains(order.get(index))&&!finished.contains(index);}
    private int playingCount(){return (int)java.util.stream.IntStream.range(0,order.size()).filter(this::active).count();}
    public UUID loser(){return loser<0?null:order.get(loser);}
    public void retire(UUID id){
        int i=at(id);if(i<0||!retired.add(id))return;
        cards.returnHand(id);declared.remove(id);extraDebt[i]=0;
        if(over())return;
        if(missed==i)missed=-1;
        if(pendingWinner==i)pendingWinner=-1;
        emptyHands.remove(i);
        if(challenger==i||offender==i){challengeOpen=false;revealTo=null;}
        if(chooser==i){chooser=-1;color=Math.max(0,Math.min(3,color));}
        if(afterColor==i)afterColor=step(i,1);
        if(afterDebt==i)afterDebt=step(i,1);
        if(debtTarget==i){debt=0;debtTarget=-1;int next=afterDebt;afterDebt=-1;move(next<0?step(i,1):next);}
        else if(turn==i)move(step(i,1));
        if(NullHouseRule.LAST_STANDING.enabled(houseRules))finishLastStanding();
        else if(remainingPlayers()==1){winner=at(order.stream().filter(p->!retired.contains(p)).findFirst().orElseThrow());chooser=-1;debt=0;}
        else finish();
    }
    private void move(int index){turn=index;drew=false;drawnIndex=-1;}
    private void penalty(int index,int amount,int next){debtTarget=index;debt=amount;afterDebt=next;move(index);}
    private void sendPenalty(int source,int target,int amount,boolean four){
        penaltySource=source;
        penaltyIsFour=four;
        penaltyAccepted=false;
        penalty(target,amount,step(target,1));
    }
    private int[] hand(int index){return cards.handFaces(order.get(index));}
    private boolean hasColor(int index,int wanted){for(int face:hand(index))if(NullCard.fromFace(face).color().ordinal()==wanted)return true;return false;}
    boolean opponentNearWin(UUID id){
        int player=at(id);
        return player>=0&&hand(step(player,1)).length<=2;
    }

    public UUID turn(){return order.get(turn);}
    public int direction(){return direction;}
    public int color(){return color;}
    public UUID chooser(){return chooser<0?null:order.get(chooser);}
    public UUID challenger(){return challengeOpen&&chooser<0?order.get(challenger):null;}
    public UUID vulnerable(){return missed<0?null:order.get(missed);}
    public UUID winner(){return winner<0?null:order.get(winner);}
    public boolean over(){return winner>=0;}
    public UUID lastCaller(){return lastCaller;}
    public int lastSerial(){return lastSerial;}
    public UUID revealTo(){return revealTo;}
    public UUID revealSource(){return revealTo==null?null:order.get(offender);}
    public int[] revealed(){return challengeHand.clone();}
    public boolean challengeWon(){return challengeWon;}
    public void dismissReveal(UUID id){if(id.equals(revealTo))revealTo=null;}
    public int debt(UUID id){int i=at(id);return i<0?0:extraDebt[i]+(debtTarget==i?debt:0);}
    public int roundPoints(){if(winner<0)return 0;int total=0;for(int i=0;i<order.size();i++)if(i!=winner)for(int face:hand(i))total+=NullCard.fromFace(face).points();return total;}
    public boolean canPlay(UUID id,int index){
        int i=at(id);if(!active(i)||over()||chooser>=0||extraDebt[i]>0)return false;
        int[] hand=hand(i);if(index<0||index>=hand.length)return false;
        NullCard card=NullCard.fromFace(hand[index]),top=NullCard.fromFace(cards.topFace());
        if(debt>0)return i==debtTarget&&!penaltyAccepted&&canAnswerPenalty(card,top);
        if(pendingWinner>=0)return false;
        if(i!=turn)return NullHouseRule.JUMP_IN.enabled(houseRules)&&hand[index]==cards.topFace();
        if(drew&&index!=drawnIndex)return false;
        return card.color()==NullCard.Color.WILD||card.color().ordinal()==color||card.kind()==top.kind()&&(card.kind()!=NullCard.Kind.NUMBER||card.number()==top.number());
    }
    private boolean canAnswerPenalty(NullCard card,NullCard top){
        if(card.kind()==NullCard.Kind.REVERSE){
            return NullHouseRule.NO_U.enabled(houseRules)&&active(penaltySource)
                &&penaltySource!=debtTarget&&(card.color().ordinal()==color||top.kind()==NullCard.Kind.REVERSE);
        }
        boolean stacking=NullHouseRule.STACKING.enabled(houseRules);
        if(!stacking&&!NullHouseRule.DEFLECT.enabled(houseRules))return false;
        if(card.kind()==NullCard.Kind.DRAW_TWO)return !penaltyIsFour||card.color().ordinal()==color;
        return stacking&&penaltyIsFour&&card.kind()==NullCard.Kind.WILD_DRAW_FOUR;
    }
    private void beginsAction(int player){if(player==turn)missed=-1;}
    private void clearDeclaration(int player){declared.remove(order.get(player));}
    public int play(UUID id,int index){
        if(!canPlay(id,index))return -1;
        int player=at(id);missed=-1;int[] before=hand(player);NullCard card=NullCard.fromFace(before[index]);
        int incoming=debt;
        int sender=penaltySource;
        challengeOpen=false;
        int previousColor=color;boolean illegalFour=card.kind()==NullCard.Kind.WILD_DRAW_FOUR&&hasColor(player,previousColor);
        boolean announced=declared.remove(id);int face=cards.play(id,index);if(face<0)return -1;
        if(before.length==2&&!announced)missed=player;
        if(hand(player).length==0){
            if(NullHouseRule.LAST_STANDING.enabled(houseRules))emptyHands.add(player);
            else if(pendingWinner<0)pendingWinner=player;
        }
        drew=false;drawnIndex=-1;
        int carried=NullHouseRule.STACKING.enabled(houseRules)?incoming:0;
        if(incoming>0&&card.kind()==NullCard.Kind.REVERSE){
            color=card.color().ordinal();
            direction=-direction;
            sendPenalty(player,sender,incoming,penaltyIsFour);
            finish();
            return face;
        }
        if(card.color()==NullCard.Color.WILD){
            chooser=player;afterColor=step(player,1);
            if(card.kind()==NullCard.Kind.WILD_DRAW_FOUR){
                offender=player;challenger=afterColor;bluff=illegalFour;challengeOpen=incoming==0;challengeHand=hand(player).clone();revealTo=null;
                sendPenalty(player,afterColor,4+carried,true);
            }
        }else{
            color=card.color().ordinal();
            switch(card.kind()){
                case REVERSE -> {direction=-direction;move(playingCount()==2?player:step(player,1));}
                case SKIP -> move(step(player,2));
                case DRAW_TWO -> sendPenalty(player,step(player,1),2+carried,false);
                default -> move(step(player,1));
            }
        }
        finish();return face;
    }
    public boolean chooseColor(UUID id,int selected){
        if(over()||chooser!=at(id)||chooser<0||selected<0||selected>3)return false;
        color=selected;chooser=-1;if(debt==0)move(afterColor);afterColor=-1;finish();return true;
    }
    public boolean canDraw(UUID id){
        int i=at(id);if(!active(i)||over()||chooser>=0)return false;
        if(extraDebt[i]>0)return true;
        return debt>0?i==debtTarget:pendingWinner<0&&i==turn&&!drew;
    }
    /** One physical pickup per click. Returns true only when a card was added to the hand. */
    public boolean draw(UUID id){
        if(!canDraw(id))return false;
        int i=at(id);boolean extra=extraDebt[i]>0,forced=extra||debt>0;
        if(!extra){beginsAction(i);if(!forced)clearDeclaration(i);}
        boolean added=cards.draw(id);
        if(extra){if(added)extraDebt[i]--;else extraDebt[i]=0;}
        else if(debt>0){
            challengeOpen=false;penaltyAccepted=true;if(added)debt--;else debt=0;
            if(debt==0){int next=afterDebt;debtTarget=-1;afterDebt=-1;move(next);}
        }else{drew=true;drawnIndex=added?hand(i).length-1:-1;}
        finish();return added;
    }
    public boolean canPass(UUID id){return !over()&&pendingWinner<0&&chooser<0&&debt==0&&at(id)==turn&&extraDebt[turn]==0&&drew;}
    public boolean pass(UUID id){if(!canPass(id))return false;clearDeclaration(turn);beginsAction(turn);move(step(turn,1));finish();return true;}
    public boolean challenge(UUID id){
        if(!challengeOpen||chooser>=0||at(id)!=challenger||debt!=4)return false;
        revealTo=id;challengeWon=bluff;challengeOpen=false;
        penaltyAccepted=true;
        if(bluff)penalty(offender,4,challenger);else penalty(challenger,6,step(challenger,1));
        return true;
    }
    /** Return whether this call declares/catches LAST. Unnecessary calls have no penalty. */
    public boolean callLast(UUID id){
        int i=at(id);if(!active(i)||over())return false;
        if(missed>=0){
            if(i!=missed)extraDebt[missed]+=2;missed=-1;lastCaller=id;lastSerial++;return true;
        }
        if(hand(i).length==2&&(canPlay(id,0)||canPlay(id,1)||i==turn&&chooser<0&&debt==0&&extraDebt[i]==0)){
            if(declared.add(id)){lastCaller=id;lastSerial++;return true;}return false;
        }
        return false;
    }
    private void finish(){
        if(NullHouseRule.LAST_STANDING.enabled(houseRules)){
            finishLastStanding();
            return;
        }
        if(pendingWinner<0||chooser>=0||debt>0)return;
        for(int extra:extraDebt)if(extra>0)return;
        if(hand(pendingWinner).length==0)winner=pendingWinner;else pendingWinner=-1;
    }
    private void finishLastStanding(){
        if(over()||chooser>=0||debt>0)return;
        for(int extra:extraDebt)if(extra>0)return;
        for(int player:emptyHands)if(active(player)&&hand(player).length==0)finished.add(player);
        emptyHands.clear();
        int last=-1;
        for(int i=0;i<order.size();i++){
            if(!active(i))continue;
            if(last>=0){
                if(!active(turn))move(step(turn,1));
                return;
            }
            last=i;
        }
        loser=last>=0&&!finished.isEmpty()?last:-1;
        if(!finished.isEmpty())winner=finished.iterator().next();
        else if(last>=0)winner=last;
    }
}
