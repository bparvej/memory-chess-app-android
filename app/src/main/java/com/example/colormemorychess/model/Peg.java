package com.example.colormemorychess.model;

/**
 * Represents an individual wooden peg on the chess board.
 */
public class Peg {

    public enum State {
        HIDDEN,   // Wooden top facing up, color hidden underneath
        REVEALED, // Peg lifted/flipped, color visible
        REMOVED   // Successfully matched and removed from board
    }

    private final int id;
    private final PegColor color;
    private State state;
    private final float ringRadiusRatio; // Distance from board center (0.0 to 1.0 scale)
    private final float angleDegrees;    // Radial angle in degrees (0..360)

    private float animProgress;          // 0.0f = resting, 1.0f = fully lifted/flipped
    private boolean isHighlighted;       // True when peek hint is active

    public Peg(int id, PegColor color, float ringRadiusRatio, float angleDegrees) {
        this.id = id;
        this.color = color;
        this.ringRadiusRatio = ringRadiusRatio;
        this.angleDegrees = angleDegrees;
        this.state = State.HIDDEN;
        this.animProgress = 0.0f;
        this.isHighlighted = false;
    }

    public int getId() {
        return id;
    }

    public PegColor getColor() {
        return color;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public float getRingRadiusRatio() {
        return ringRadiusRatio;
    }

    public float getAngleDegrees() {
        return angleDegrees;
    }

    public float getAnimProgress() {
        return animProgress;
    }

    public void setAnimProgress(float animProgress) {
        this.animProgress = animProgress;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public boolean isHidden() {
        return state == State.HIDDEN;
    }

    public boolean isRevealed() {
        return state == State.REVEALED;
    }

    public boolean isRemoved() {
        return state == State.REMOVED;
    }
}
