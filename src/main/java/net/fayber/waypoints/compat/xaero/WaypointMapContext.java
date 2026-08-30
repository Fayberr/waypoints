package net.fayber.waypoints.compat.xaero;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Per-frame state shared between the renderer, the provider and the reader of the World Map
 * element. Xaero's element framework hands the same context object to all three, so this is where
 * anything the frame needs (currently: which dimension's map is on screen) is parked by
 * {@link WaypointMapRenderer#preRender}, which the framework calls before the provider iterates.
 *
 * <p>Kept free of Xaero types on purpose: only the renderer/provider/reader touch Xaero classes,
 * which keeps the class-loading blast radius small if the World Map is not installed.
 */
public final class WaypointMapContext {
    /** Identifier of the dimension whose map is being drawn, e.g. {@code minecraft:overworld}. */
    private String dimensionId = "";

    void setDimension(ResourceKey<Level> dimension) {
        this.dimensionId = dimension == null ? "" : dimension.identifier().toString();
    }

    /** Empty when the map dimension is unknown, in which case nothing should be drawn. */
    public String dimensionId() {
        return this.dimensionId;
    }
}
