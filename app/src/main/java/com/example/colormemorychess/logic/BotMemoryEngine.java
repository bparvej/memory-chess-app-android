package com.example.colormemorychess.logic;

import com.example.colormemorychess.model.BotDifficulty;
import com.example.colormemorychess.model.Peg;
import com.example.colormemorychess.model.PegColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Probabilistic AI Memory Engine for Vs Bot mode.
 * Remembers revealed peg locations according to difficulty accuracy rate.
 */
public class BotMemoryEngine {

    private BotDifficulty difficulty;
    private final Map<Integer, PegColor> rememberedPegs = new HashMap<>();
    private final Random random = new Random();

    public BotMemoryEngine(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setDifficulty(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void resetMemory() {
        rememberedPegs.clear();
    }

    /**
     * Called whenever any peg is revealed on the board.
     * Evaluates probabilistic retention based on difficulty level.
     */
    public void onPegRevealed(Peg peg) {
        if (peg == null || peg.isRemoved()) return;

        float roll = random.nextFloat(); // 0.0 to 1.0
        if (roll <= difficulty.getRetentionProbability()) {
            rememberedPegs.put(peg.getId(), peg.getColor());
        } else {
            // Optional: forget if retention roll fails
            rememberedPegs.remove(peg.getId());
        }
    }

    /**
     * Called when a peg is removed from the board.
     */
    public void onPegRemoved(Peg peg) {
        if (peg != null) {
            rememberedPegs.remove(peg.getId());
        }
    }

    /**
     * Selects the best peg for AI turn given the current target die color and available pegs.
     */
    public Peg selectPeg(PegColor targetColor, List<Peg> unrevealedPegs) {
        if (unrevealedPegs == null || unrevealedPegs.isEmpty()) {
            return null;
        }

        // 1. Search memory for a peg matching targetColor that is still unrevealed
        for (Peg peg : unrevealedPegs) {
            if (rememberedPegs.containsKey(peg.getId())) {
                PegColor rememberedColor = rememberedPegs.get(peg.getId());
                if (rememberedColor == targetColor) {
                    return peg; // Guaranteed match based on AI memory!
                }
            }
        }

        // 2. Filter out pegs remembered as WRONG color for target
        List<Peg> unknownOrPossiblePegs = new ArrayList<>();
        for (Peg peg : unrevealedPegs) {
            if (!rememberedPegs.containsKey(peg.getId())) {
                unknownOrPossiblePegs.add(peg);
            }
        }

        // If we have unknown pegs, pick randomly from them
        if (!unknownOrPossiblePegs.isEmpty()) {
            return unknownOrPossiblePegs.get(random.nextInt(unknownOrPossiblePegs.size()));
        }

        // 3. Fallback: pick any random unrevealed peg
        return unrevealedPegs.get(random.nextInt(unrevealedPegs.size()));
    }
}
