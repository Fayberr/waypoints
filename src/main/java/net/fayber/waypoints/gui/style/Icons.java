package net.fayber.waypoints.gui.style;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The small vector glyphs used on the waypoint screens, drawn from {@link Ui} primitives rather
 * than a sprite sheet.
 *
 * <p>Every icon is defined inside a square box of side {@code size} centred on {@code (cx, cy)} and
 * scales with it, so the same code is sharp at any GUI scale and any button size. Stroke weight is
 * derived from the size so the icons keep their proportions instead of getting spindly when large.
 */
public final class Icons {
    private Icons() {
    }

    private static float stroke(float size) {
        return Math.max(1.0f, size / 9.0f);
    }

    /** Plus sign, for "new waypoint". */
    public static void plus(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        float arm = size / 2.0f;
        Ui.pill(gfx, cx - arm, cy - t / 2.0f, size, t, color);
        Ui.pill(gfx, cx - t / 2.0f, cy - arm, t, size, color);
    }

    /** Magnifier, used as the search field's leading glyph. */
    public static void search(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        float r = size * 0.32f;
        float lensX = cx - size * 0.08f;
        float lensY = cy - size * 0.08f;
        Ui.ring(gfx, lensX, lensY, r, t, color);
        float d = r * 0.7071f;
        Ui.stroke(gfx, lensX + d, lensY + d, cx + size * 0.42f, cy + size * 0.42f, t, color);
    }

    /** Open eye (waypoint shown): a ring with a filled pupil. */
    public static void eye(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        Ui.ring(gfx, cx, cy, size * 0.42f, t, color);
        Ui.circle(gfx, cx, cy, size * 0.16f, color);
    }

    /** Crossed-out eye (waypoint hidden). */
    public static void eyeOff(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        Ui.ring(gfx, cx, cy, size * 0.42f, t, color);
        float d = size * 0.46f;
        Ui.stroke(gfx, cx - d, cy - d, cx + d, cy + d, t, color);
    }

    /** Arrow pointing up and to the right, for "teleport here". */
    public static void teleport(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        float a = size * 0.38f;
        Ui.stroke(gfx, cx - a, cy + a, cx + a, cy - a, t, color);
        Ui.stroke(gfx, cx + a, cy - a, cx - size * 0.02f, cy - a, t, color);
        Ui.stroke(gfx, cx + a, cy - a, cx + a, cy + size * 0.02f, t, color);
    }

    /** Waste bin, for "delete waypoint". */
    public static void trash(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        float halfW = size * 0.34f;
        float lidY = cy - size * 0.28f;
        // Lid bar plus the little handle above it.
        Ui.pill(gfx, cx - halfW - t, lidY, (halfW + t) * 2.0f, t, color);
        Ui.pill(gfx, cx - size * 0.14f, lidY - t * 1.6f, size * 0.28f, t, color);
        // Body drawn as three strokes (two slightly tapered walls and a base) rather than a
        // rounded rect with a transparent middle: alpha blending cannot punch a hole, so an
        // "outline" made that way would come out as a solid block.
        float bodyTop = lidY + t * 1.4f;
        float bodyBottom = bodyTop + size * 0.58f;
        float taper = size * 0.05f;
        Ui.stroke(gfx, cx - halfW * 0.86f, bodyTop, cx - halfW * 0.86f + taper, bodyBottom, t, color);
        Ui.stroke(gfx, cx + halfW * 0.86f, bodyTop, cx + halfW * 0.86f - taper, bodyBottom, t, color);
        Ui.stroke(gfx, cx - halfW * 0.86f + taper, bodyBottom, cx + halfW * 0.86f - taper, bodyBottom, t, color);
    }

    /** Gear, for "settings". */
    public static void gear(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float t = stroke(size);
        float r = size * 0.42f;
        Ui.ring(gfx, cx, cy, r * 0.62f, t, color);
        // Six teeth around the hub.
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * i / 3.0;
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            Ui.stroke(gfx, cx + dx * r * 0.68f, cy + dy * r * 0.68f, cx + dx * r, cy + dy * r, t, color);
        }
    }

    /** Solid location pin, used as the colour swatch on a waypoint card. */
    public static void pin(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color) {
        float r = size * 0.34f;
        Ui.circle(gfx, cx, cy - size * 0.12f, r, color);
        // Tapering tail: a short stack of shrinking bars reads as a point at this size.
        int steps = Math.max(3, Math.round(size / 2.0f));
        for (int i = 0; i < steps; i++) {
            float f = (float) i / steps;
            float w = r * 1.15f * (1.0f - f);
            float y = cy - size * 0.12f + r * 0.55f + f * size * 0.42f;
            Ui.rect(gfx, cx - w / 2.0f, y, w, size * 0.42f / steps + 0.5f, color);
        }
    }
}
