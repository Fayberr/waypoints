package net.fayber.waypoints.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public class WorldIdResolver {

    public static String resolveCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return "default_world";
        }

        // Singleplayer
        IntegratedServer singleplayer = mc.getSingleplayerServer();
        if (singleplayer != null) {
            String levelName = singleplayer.getWorldData().getLevelName();
            return "sp_" + sanitize(levelName);
        }

        // Multiplayer Dedicated Server
        ServerData serverData = mc.getCurrentServer();
        if (serverData != null) {
            return "mp_" + sanitize(serverData.ip);
        }

        // Fallback
        return "default_world";
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
