package com.example.colormemorychess.model;

/**
 * Difficulty settings for the AI opponent in Vs Bot mode.
 */
public enum BotDifficulty {
    EASY("Easy", 0.25f),     // 25% memory retention
    MEDIUM("Medium", 0.60f), // 60% memory retention
    HARD("Hard", 0.90f);     // 90% memory retention

    private final String name;
    private final float retentionProbability;

    BotDifficulty(String name, float retentionProbability) {
        this.name = name;
        this.retentionProbability = retentionProbability;
    }

    public String getName() {
        return name;
    }

    public float getRetentionProbability() {
        return retentionProbability;
    }
}
