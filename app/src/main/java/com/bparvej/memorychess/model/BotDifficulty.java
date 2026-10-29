package com.bparvej.memorychess.model;

public enum BotDifficulty {
    EASY("Easy", 0.25f),
    MEDIUM("Medium", 0.60f),
    HARD("Hard", 0.90f);

    private final String name;
    private final float retentionProbability;

    BotDifficulty(String name, float retentionProbability) {
        this.name = name;
        this.retentionProbability = retentionProbability;
    }

    public String getName() { return name; }
    public float getRetentionProbability() { return retentionProbability; }
}
