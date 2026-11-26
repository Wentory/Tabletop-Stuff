package com.wentory.tabletopstuff.nullgame;

import java.util.ArrayList;
import java.util.List;

/** Immutable classic 108-card composition; round logic lives in NullRound. */
public final class NullDeck {
    private static final NullCard.Color[] COLORS = {
        NullCard.Color.RED, NullCard.Color.BLUE, NullCard.Color.GREEN, NullCard.Color.YELLOW
    };
    private static final List<NullCard> CLASSIC = create();

    private static List<NullCard> create() {
        List<NullCard> cards = new ArrayList<>(108);
        for (var color : COLORS) cards.add(new NullCard(color, NullCard.Kind.NUMBER, 0));
        for (int number = 1; number <= 9; number++) for (int copy = 0; copy < 2; copy++) {
            for (var color : COLORS) cards.add(new NullCard(color, NullCard.Kind.NUMBER, number));
        }
        for (var kind : new NullCard.Kind[] {NullCard.Kind.SKIP, NullCard.Kind.REVERSE, NullCard.Kind.DRAW_TWO}) {
            for (int copy = 0; copy < 2; copy++) for (var color : COLORS) cards.add(new NullCard(color, kind, -1));
        }
        for (int copy = 0; copy < 4; copy++) {
            cards.add(new NullCard(NullCard.Color.WILD, NullCard.Kind.WILD, -1));
            cards.add(new NullCard(NullCard.Color.WILD, NullCard.Kind.WILD_DRAW_FOUR, -1));
        }
        return List.copyOf(cards);
    }

    public static List<NullCard> classic() { return CLASSIC; }

    /** Debug hands can exceed one deck; repeat its identities without pretending to deal a game. */
    public static NullCard previewCard(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative card index");
        return CLASSIC.get(index % CLASSIC.size());
    }

    private NullDeck() {}
}
