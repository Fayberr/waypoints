package net.fayber.waypoints.model;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class WaypointColor {
    private final int argb;
    private final boolean chroma;

    public static final WaypointColor CYAN = of(0xFF00E5FF);
    public static final WaypointColor EMERALD = of(0xFF00E676);
    public static final WaypointColor RED = of(0xFFFF1744);
    public static final WaypointColor AMETHYST = of(0xFFD500F9);
    public static final WaypointColor GOLD = of(0xFFFFD600);
    public static final WaypointColor ORANGE = of(0xFFFF6D00);
    public static final WaypointColor BLUE = of(0xFF2979FF);
    public static final WaypointColor WHITE = of(0xFFFFFFFF);

    public WaypointColor(int argb, boolean chroma) {
        this.argb = argb;
        this.chroma = chroma;
    }

    public static WaypointColor of(int argb) {
        return new WaypointColor(argb, false);
    }

    public static WaypointColor dynamicChroma() {
        return new WaypointColor(0xFFFFFFFF, true);
    }

    public int getArgb() {
        return argb;
    }

    public boolean isChroma() {
        return chroma;
    }

    public int getEffectiveArgb() {
        if (!chroma) {
            return argb;
        }
        float hue = (Util.getMillis() % 4000L) / 4000.0f;
        int rgb = Mth.hsvToRgb(hue, 0.85f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public float getRed() {
        return ((getEffectiveArgb() >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((getEffectiveArgb() >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (getEffectiveArgb() & 0xFF) / 255.0f;
    }

    public float getAlpha() {
        return ((getEffectiveArgb() >> 24) & 0xFF) / 255.0f;
    }
}
