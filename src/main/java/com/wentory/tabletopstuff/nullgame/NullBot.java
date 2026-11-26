package com.wentory.tabletopstuff.nullgame;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

public final class NullBot {
    public enum Difficulty {
        EASY("easy"), NORMAL("normal"), HARD("hard");

        private final String key;
        Difficulty(String name) { this.key = "tabletop_stuff.lobby.bot_" + name; }
        public String key() { return key; }
    }

    public enum Action { COLOR, LAST, CHALLENGE, PLAY, DRAW, PASS }
    public record Move(Action action,int value) {}

    private NullBot() {}

    public static Move decide(NullRound rules,UUID bot,int[] hand,boolean mayCallLast,Difficulty difficulty,IntUnaryOperator random){
        if(rules.over())return null;
        if(bot.equals(rules.chooser()))return new Move(Action.COLOR,difficulty==Difficulty.EASY?random.applyAsInt(4):preferredColor(hand));
        UUID missed=rules.vulnerable();
        if(difficulty!=Difficulty.EASY&&missed!=null&&!missed.equals(bot)&&mayCallLast)return new Move(Action.LAST,0);
        if(!bot.equals(rules.turn())&&rules.debt(bot)==0)return null;
        if(difficulty==Difficulty.NORMAL&&bot.equals(rules.challenger())&&random.applyAsInt(4)==0)return new Move(Action.CHALLENGE,0);
        int card=botCard(rules,bot,hand,difficulty,random);
        if(card>=0)return hand.length==2&&mayCallLast?new Move(Action.LAST,0):new Move(Action.PLAY,card);
        if(rules.canDraw(bot))return new Move(Action.DRAW,0);
        if(rules.canPass(bot))return new Move(Action.PASS,0);
        return null;
    }
    private static int botCard(NullRound rules,UUID bot,int[] hand,Difficulty difficulty,IntUnaryOperator random){
        var candidates=new ArrayList<Integer>();
        int[] colors=countColors(hand);
        boolean urgent=difficulty==Difficulty.HARD&&rules.opponentNearWin(bot);
        int best=-1,bestScore=Integer.MIN_VALUE;
        for(int i=0;i<hand.length;i++){
            if(!rules.canPlay(bot,i))continue;
            var card=NullCard.fromFace(hand[i]);
            int score=card.color().ordinal()<4?card.points():-1;
            if(card.kind()==NullCard.Kind.WILD_DRAW_FOUR&&colors[rules.color()]>0)continue;
            if(difficulty==Difficulty.EASY){candidates.add(i);continue;}
            if(difficulty==Difficulty.HARD){
                int color=card.color().ordinal();
                if(color<colors.length)score+=colors[color]*8;
                if(urgent&&(card.kind()==NullCard.Kind.SKIP||card.kind()==NullCard.Kind.DRAW_TWO
                    ||card.kind()==NullCard.Kind.WILD_DRAW_FOUR))score+=60;
            }
            if(score>bestScore){best=i;bestScore=score;}
        }
        if(!candidates.isEmpty())return candidates.get(random.applyAsInt(candidates.size()));
        return best;
    }
    private static int[] countColors(int[] hand){
        int[] colors=new int[4];
        for(int face:hand){
            int color=NullCard.fromFace(face).color().ordinal();
            if(color<colors.length)colors[color]++;
        }
        return colors;
    }
    private static int preferredColor(int[] hand){
        int[] colors=countColors(hand);
        int best=0;
        for(int i=1;i<colors.length;i++)if(colors[i]>colors[best])best=i;
        return best;
    }
}
