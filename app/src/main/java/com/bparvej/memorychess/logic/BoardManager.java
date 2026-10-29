package com.bparvej.memorychess.logic;

import com.bparvej.memorychess.model.Peg;
import com.bparvej.memorychess.model.PegColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BoardManager {

    private static final int OUTER_PEG_COUNT = 16;
    private static final int INNER_PEG_COUNT = 8;
    private static final float OUTER_RADIUS_RATIO = 0.72f;
    private static final float INNER_RADIUS_RATIO = 0.42f;

    private final List<Peg> pegs = new ArrayList<>();
    private final Random random = new Random();

    public BoardManager() {
        initBoard();
    }

    public void initBoard() {
        pegs.clear();

        List<PegColor> colorList = new ArrayList<>();
        for (PegColor color : PegColor.values()) {
            for (int i = 0; i < 4; i++) {
                colorList.add(color);
            }
        }
        Collections.shuffle(colorList, random);

        int colorIdx = 0;
        for (int i = 0; i < OUTER_PEG_COUNT; i++) {
            float angle = i * (360.0f / OUTER_PEG_COUNT);
            pegs.add(new Peg(colorIdx, colorList.get(colorIdx), OUTER_RADIUS_RATIO, angle));
            colorIdx++;
        }

        float offsetAngle = 360.0f / (OUTER_PEG_COUNT * 2);
        for (int i = 0; i < INNER_PEG_COUNT; i++) {
            float angle = i * (360.0f / INNER_PEG_COUNT) + offsetAngle;
            pegs.add(new Peg(colorIdx, colorList.get(colorIdx), INNER_RADIUS_RATIO, angle));
            colorIdx++;
        }
    }

    public List<Peg> getPegs() { return pegs; }

    public List<Peg> getUnrevealedPegs() {
        List<Peg> unrevealed = new ArrayList<>();
        for (Peg peg : pegs) {
            if (peg.isHidden()) unrevealed.add(peg);
        }
        return unrevealed;
    }

    public boolean isBoardEmpty() {
        for (Peg peg : pegs) {
            if (!peg.isRemoved()) return false;
        }
        return true;
    }

    public PegColor rollDice() {
        List<Peg> unrevealed = getUnrevealedPegs();
        if (unrevealed.isEmpty()) return PegColor.getRandom();
        Peg randomPeg = unrevealed.get(random.nextInt(unrevealed.size()));
        return randomPeg.getColor();
    }
}
