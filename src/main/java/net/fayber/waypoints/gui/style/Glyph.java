package net.fayber.waypoints.gui.style;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * One of the {@link Icons} sprites, so buttons can take an icon as a parameter
 * instead of each needing its own subclass.
 */
@FunctionalInterface
public interface Glyph {
    /** Draws the glyph centred on {@code (cx, cy)} inside a box of side {@code size}. */
    void draw(GuiGraphicsExtractor gfx, float cx, float cy, float size, int color);
}
