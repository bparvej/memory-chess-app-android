package com.bparvej.memorychess.model;

public enum GameMode {
    SOLO_TIME_ATTACK("Solo Time Attack"),
    PASS_AND_PLAY_2P("Pass & Play (2 Players)"),
    PASS_AND_PLAY_3P("Pass & Play (3 Players)"),
    VS_BOT("Vs AI Bot");

    private final String title;

    GameMode(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
