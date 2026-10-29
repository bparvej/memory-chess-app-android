package com.bparvej.memorychess.model;

public class Peg {

    public enum State {
        HIDDEN, REVEALED, REMOVED
    }

    private final int id;
    private final PegColor color;
    private State state;
    private final float ringRadiusRatio;
    private final float angleDegrees;

    private float animProgress;
    private boolean isHighlighted;

    public Peg(int id, PegColor color, float ringRadiusRatio, float angleDegrees) {
        this.id = id;
        this.color = color;
        this.ringRadiusRatio = ringRadiusRatio;
        this.angleDegrees = angleDegrees;
        this.state = State.HIDDEN;
        this.animProgress = 0.0f;
        this.isHighlighted = false;
    }

    public int getId() { return id; }
    public PegColor getColor() { return color; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public float getRingRadiusRatio() { return ringRadiusRatio; }
    public float getAngleDegrees() { return angleDegrees; }
    public float getAnimProgress() { return animProgress; }
    public void setAnimProgress(float animProgress) { this.animProgress = animProgress; }
    public boolean isHighlighted() { return isHighlighted; }
    public void setHighlighted(boolean highlighted) { isHighlighted = highlighted; }
    public boolean isHidden() { return state == State.HIDDEN; }
    public boolean isRevealed() { return state == State.REVEALED; }
    public boolean isRemoved() { return state == State.REMOVED; }
}
