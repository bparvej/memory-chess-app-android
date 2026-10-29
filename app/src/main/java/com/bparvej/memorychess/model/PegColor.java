package com.bparvej.memorychess.model;

import android.graphics.Color;
import java.util.Random;

public enum PegColor {
    RED("Red", Color.parseColor("#E53935")),
    BLUE("Blue", Color.parseColor("#1E88E5")),
    GREEN("Green", Color.parseColor("#43A047")),
    YELLOW("Yellow", Color.parseColor("#FDD835")),
    WHITE("White", Color.parseColor("#FAFAFA")),
    BLACK("Black", Color.parseColor("#212121"));

    private final String displayName;
    private final int colorInt;

    PegColor(String displayName, int colorInt) {
        this.displayName = displayName;
        this.colorInt = colorInt;
    }

    public String getDisplayName() { return displayName; }
    public int getColorInt() { return colorInt; }

    private static final Random RANDOM = new Random();

    public static PegColor getRandom() {
        PegColor[] colors = values();
        return colors[RANDOM.nextInt(colors.length)];
    }
}
