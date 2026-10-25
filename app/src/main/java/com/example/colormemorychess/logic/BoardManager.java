package com.example.colormemorychess.logic;

import com.example.colormemorychess.model.Peg;
import com.example.colormemorychess.model.PegColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the 24 circular wooden pegs layout, shuffling, and board state.
 */
public class BoardManager {

    private static final int TOTAL_PEGS = 24;
    private static final int OUTER_PEG_COUNT = 16;
    private static final int INNER_PEG_COUNT = 8;

    private static final float OUTER_RADIUS_RATIO = 0.72f;
    private static final float INNER_RADIUS_RATIO = 0.42f;

    private final List<Peg> pegs = new ArrayList<>();
    private final Random random = new Random();

    public BoardManager() {
        initBoard();
    }

    /**
     * Initializes or resets the board with 24 pegs (4 of each of the 6 colors)
     * shuffled into outer (16) and inner (8) concentric rings.
     */
    public void initBoard() {
        pegs.clear();

        // 1. Prepare 24 peg colors (4 of each color)
        List<PegColor> colorList = new ArrayList<>();
        for (PegColor color : PegColor.values()) {
            for (int i = 0; i < 4; i++) {
                colorList.add(color);
            }
        }

        // 2. Shuffle colors
        Collections.shuffle(colorList, random);

        // 3. Create Outer Ring (16 pegs)
        int colorIdx = 0;
        for (int i = 0; i < OUTER_PEG_COUNT; i++) {
            float angle = i * (360.0f / OUTER_PEG_COUNT);
            pegs.add(new Peg(colorIdx, colorList.get(colorIdx), OUTER_RADIUS_RATIO, angle));
            colorIdx++;
        }

        // 4. Create Inner Ring (8 pegs), slightly offset angle for balanced visuals
        float offsetAngle = 360.0f / (OUTER_PEG_COUNT * 2);
        for (int i = 0; i < INNER_PEG_COUNT; i++) {
            float angle = i * (360.0f / INNER_PEG_COUNT) + offsetAngle;
            pegs.add(new Peg(colorIdx, colorList.get(colorIdx), INNER_RADIUS_RATIO, angle));
            colorIdx++;
        }
    }

    public List<Peg> getPegs() {
        return pegs;
    }

    public Peg getPegById(int id) {
        for (Peg peg : pegs) {
            if (peg.getId() == id) {
                return peg;
            }
        }
        return null;
    }

    public List<Peg> getUnrevealedPegs() {
        List<Peg> unrevealed = new ArrayList<>();
        for (Peg peg : pegs) {
            if (peg.isHidden()) {
                unrevealed.add(peg);
            }
        }
        return unrevealed;
    }

    public boolean isBoardEmpty() {
        for (Peg peg : pegs) {
            if (!peg.isRemoved()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rolls a color dice. Ensures the rolled color exists among remaining pegs
     * to keep the game playable and exciting.
     */
    public PegColor rollDice() {
        List<Peg> unrevealed = getUnrevealedPegs();
        if (unrevealed.isEmpty()) {
            return PegColor.getRandom();
        }
        // Pick random peg among unrevealed to get a guaranteed remaining color
        Peg randomPeg = unrevealed.get(random.nextInt(unrevealed.size()));
        return randomPeg.getColor();
    }
}
