package com.example.colormemorychess.model;

/**
 * Supported game modes for Color Memory Chess.
 */
public enum GameMode {
    SOLO_TIME_ATTACK("Solo Time Attack"),
    PASS_AND_PLAY("Pass & Play (2 Player)"),
    VS_BOT("Vs AI Bot");

    private final String title;

    GameMode(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
