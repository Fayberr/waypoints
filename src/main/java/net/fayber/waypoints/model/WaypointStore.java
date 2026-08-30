package net.fayber.waypoints.model;

import net.fayber.waypoints.storage.WaypointStorage;
import net.fayber.waypoints.storage.WorldIdResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WaypointStore {
    private static final WaypointStore INSTANCE = new WaypointStore();

    private String activeWorldId = null;
    private final List<Waypoint> waypoints = new ArrayList<>();

    public static WaypointStore get() {
        return INSTANCE;
    }

    public synchronized void reload() {
        String worldId = WorldIdResolver.resolveCurrentWorldId();
        if (worldId.equals(activeWorldId)) {
            return;
        }
        if (activeWorldId != null) {
            save();
        }
        activeWorldId = worldId;
        waypoints.clear();
        waypoints.addAll(WaypointStorage.loadWaypoints(worldId));
    }

    public synchronized void save() {
        if (activeWorldId != null) {
            WaypointStorage.saveWaypoints(activeWorldId, waypoints);
        }
    }

    public synchronized List<Waypoint> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(waypoints));
    }

    public synchronized List<Waypoint> getVisibleForDimension(String dimension) {
        return waypoints.stream()
                .filter(Waypoint::isVisible)
                .filter(w -> w.getDimension().equals(dimension))
                .collect(Collectors.toList());
    }

    public synchronized void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public synchronized void remove(UUID id) {
        waypoints.removeIf(w -> w.getId().equals(id));
        save();
    }

    public synchronized void clearDeathWaypoints() {
        waypoints.removeIf(Waypoint::isDeathWaypoint);
        save();
    }
}
