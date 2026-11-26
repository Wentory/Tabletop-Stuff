package com.wentory.tabletopstuff.prototype;

import java.util.List;

/** Game-specific labels and limits; the lobby and its renderer remain shared. */
public record LobbyGame(String nameKey, String icon, int maxPlayers, List<String> rules) {
    public static final LobbyGame NULL = new LobbyGame("tabletop_stuff.game.null", "tabletop_stuff:textures/cards/null/classic/back.png", 4,
        com.wentory.tabletopstuff.nullgame.NullHouseRule.keys());
}
