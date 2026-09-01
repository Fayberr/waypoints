package net.fayber.waypoints.compat.xaero;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Per-frame state shared between the renderer, provider and reader of the World Map element.
 * Xaero hands the same context object to all three, and {@link WaypointMapRenderer#preRender}
 * parks the map dimension here before the provider iterates.
 *
 * <p>No Xaero types in here on purpose, so nothing Xaero-specific gets classloaded when
 * the World Map is not installed.
 */
public final class WaypointMapContext {
    /** Dimension whose map is being drawn, e.g. {@code minecraft:overworld}. */
    private String dimensionId = "";

    void setDimension(ResourceKey<Level> dimension) {
        this.dimensionId = dimension == null ? "" : dimension.identifier().toString();
    }

    /** Empty when the map dimension is unknown, in which case nothing should be drawn. */
    public String dimensionId() {
        return this.dimensionId;
    }
}
