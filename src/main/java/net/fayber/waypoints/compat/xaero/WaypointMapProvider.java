package net.fayber.waypoints.compat.xaero;

import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

import java.util.List;

/**
 * Feeds our waypoints to Xaero's World Map one element at a time.
 *
 * <p>{@code begin} snapshots the store for the dimension on screen instead of iterating the live
 * list: the store is mutated from the client tick thread while this iterates on the render thread,
 * and {@code getVisibleForDimension} already returns a fresh list under the store's lock.
 */
public final class WaypointMapProvider extends ElementRenderProvider<Waypoint, WaypointMapContext> {
    private List<Waypoint> snapshot = List.of();
    private int index;

    @Override
    public void begin(ElementRenderLocation location, WaypointMapContext context) {
        String dimension = context.dimensionId();
        this.snapshot = dimension.isEmpty()
                ? List.of()
                : WaypointStore.get().getVisibleForDimension(dimension);
        this.index = 0;
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, WaypointMapContext context) {
        return this.index < this.snapshot.size();
    }

    @Override
    public Waypoint getNext(ElementRenderLocation location, WaypointMapContext context) {
        return this.snapshot.get(this.index++);
    }

    @Override
    public void end(ElementRenderLocation location, WaypointMapContext context) {
        // Drop the snapshot so a stale frame's waypoints cannot be retained after deletion.
        this.snapshot = List.of();
        this.index = 0;
    }
}
