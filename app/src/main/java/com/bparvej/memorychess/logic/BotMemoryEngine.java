package com.bparvej.memorychess.logic;

import com.bparvej.memorychess.model.BotDifficulty;
import com.bparvej.memorychess.model.Peg;
import com.bparvej.memorychess.model.PegColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BotMemoryEngine {

    private BotDifficulty difficulty;
    private final Map<Integer, PegColor> rememberedPegs = new HashMap<>();
    private final Random random = new Random();

    public BotMemoryEngine(BotDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setDifficulty(BotDifficulty difficulty) { this.difficulty = difficulty; }
    public void resetMemory() { rememberedPegs.clear(); }

    public void onPegRevealed(Peg peg) {
        if (peg == null || peg.isRemoved()) return;

        if (random.nextFloat() <= difficulty.getRetentionProbability()) {
            rememberedPegs.put(peg.getId(), peg.getColor());
        } else {
            rememberedPegs.remove(peg.getId());
        }
    }

    public void onPegRemoved(Peg peg) {
        if (peg != null) rememberedPegs.remove(peg.getId());
    }

    public Peg selectPeg(PegColor targetColor, List<Peg> unrevealedPegs) {
        if (unrevealedPegs == null || unrevealedPegs.isEmpty()) return null;

        for (Peg peg : unrevealedPegs) {
            if (rememberedPegs.containsKey(peg.getId())) {
                if (rememberedPegs.get(peg.getId()) == targetColor) {
                    return peg;
                }
            }
        }

        List<Peg> possiblePegs = new ArrayList<>();
        for (Peg peg : unrevealedPegs) {
            if (!rememberedPegs.containsKey(peg.getId())) {
                possiblePegs.add(peg);
            }
        }

        if (!possiblePegs.isEmpty()) {
            return possiblePegs.get(random.nextInt(possiblePegs.size()));
        }

        return unrevealedPegs.get(random.nextInt(unrevealedPegs.size()));
    }
}
