package com.wentory.tabletopstuff.nullgame;

/** Card identity is independent of texture placement and Minecraft versions. */
public record NullCard(Color color, Kind kind, int number) {
    public enum Color { RED, BLUE, GREEN, YELLOW, WILD }
    public enum Kind { NUMBER, SKIP, REVERSE, DRAW_TWO, WILD, WILD_DRAW_FOUR }

    public NullCard {
        if (color == null || kind == null) throw new IllegalArgumentException("Missing card identity");
        boolean wild = kind == Kind.WILD || kind == Kind.WILD_DRAW_FOUR;
        if (wild != (color == Color.WILD)) throw new IllegalArgumentException("Invalid card color");
        if (kind == Kind.NUMBER ? number < 0 || number > 9 : number != -1) throw new IllegalArgumentException("Invalid card number");
    }

    public static NullCard fromFace(int face){
        if(face==52)return new NullCard(Color.WILD,Kind.WILD,-1);
        if(face==53)return new NullCard(Color.WILD,Kind.WILD_DRAW_FOUR,-1);
        if(face<0||face>=52)throw new IllegalArgumentException("Invalid face");
        int symbol=face%13;return new NullCard(Color.values()[face/13],symbol<10?Kind.NUMBER:symbol==10?Kind.SKIP:symbol==11?Kind.REVERSE:Kind.DRAW_TWO,symbol<10?symbol:-1);
    }
    public int points(){return kind==Kind.NUMBER?number:color==Color.WILD?50:20;}
    public int symbolSlot() {
        return switch (kind) {
            case NUMBER -> number;
            case SKIP -> 10;
            case REVERSE -> 11;
            case DRAW_TWO -> 12;
            case WILD_DRAW_FOUR -> 13;
            case WILD -> 14;
        };
    }

    public int faceSlot() {
        if (kind == Kind.WILD) return 52;
        if (kind == Kind.WILD_DRAW_FOUR) return 53;
        return color.ordinal() * 13 + symbolSlot();
    }
}
