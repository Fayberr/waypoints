package net.fayber.waypoints.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.fayber.waypoints.model.Waypoint;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WaypointStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File WAYPOINTS_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "waypoints/worlds");
    private static final Type WAYPOINT_LIST_TYPE = new TypeToken<ArrayList<Waypoint>>() {}.getType();

    public static List<Waypoint> loadWaypoints(String worldId) {
        File file = getFileForWorld(worldId);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileReader reader = new FileReader(file)) {
            List<Waypoint> list = GSON.fromJson(reader, WAYPOINT_LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveWaypoints(String worldId, List<Waypoint> waypoints) {
        File file = getFileForWorld(worldId);
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(waypoints, WAYPOINT_LIST_TYPE, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File getFileForWorld(String worldId) {
        return new File(WAYPOINTS_DIR, worldId + ".json");
    }
}
