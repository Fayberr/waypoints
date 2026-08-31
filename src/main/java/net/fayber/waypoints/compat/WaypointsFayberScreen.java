package net.fayber.waypoints.compat;

import net.fayber.fayberconfig.api.ConfigEntry;
import net.fayber.fayberconfig.api.FayberConfigScreen;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.config.TeleportButtonVisibility;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Fayber Config binding for {@link ModConfig}: all fields across five categories, live
 * preview while the screen is open, Cancel reverts, Save persists via ConfigManager.
 *
 * <p>This class is only referenced from {@link ConfigScreenRouter} after
 * {@code FabricLoader.isModLoaded("fayberconfig")} passed, so its fayberconfig imports are only
 * classloaded when the mod is actually installed (fayberconfig is a compileOnly dependency).
 */
public final class WaypointsFayberScreen {
    private WaypointsFayberScreen() {}

    public static Screen create(Screen parent) {
        ModConfig c = ConfigManager.get();
        return FayberConfigScreen.builder(Component.literal("Modern Waypoints Settings"), parent, ConfigManager::save)

                .category("General")
                .intSlider("Max Render Distance", () -> c.renderDistance, v -> c.renderDistance = v, 0, 50000, 100)
                .tooltip("Maximum distance in blocks to render waypoints in-world (0 = unlimited).")
                .bool("Always on Top", () -> c.alwaysOnTop, v -> c.alwaysOnTop = v)
                .tooltip("Render waypoints through blocks/walls without depth obstruction.")
                .bool("Screen-Edge Arrows", () -> c.showOffscreenPointers, v -> c.showOffscreenPointers = v)
                .tooltip("Show pointer arrows at screen edges for off-screen waypoints.")
                .cycle("Hide Teleport Button", () -> c.teleportButtonVisibility, v -> c.teleportButtonVisibility = v,
                        TeleportButtonVisibility.values(), v -> v.label)
                .tooltip("When to hide the teleport button on waypoint cards. \"Without Cheats or Operator\" shows it only in single player with cheats on, or on servers where you are an operator.")

                .category("Beam")
                .bool("Enable Beacon Beam", () -> c.beaconBeamEnabled, v -> c.beaconBeamEnabled = v)
                .tooltip("Show a glowing vertical beam at every visible waypoint.")
                .floatSlider("Beam Width", () -> c.beaconWidth, v -> c.beaconWidth = v, 0.05f, 2.0f, 0.05f)
                .tooltip("Thickness/radius of the vertical glowing beam.")
                .floatSlider("Beam Opacity", () -> c.beaconAlpha, v -> c.beaconAlpha = v, 0.1f, 1.0f, 0.05f)
                .tooltip("Transparency of the beacon beam.")
                .floatSlider("Beam Height", () -> c.beaconHeight, v -> c.beaconHeight = v, 0.0f, 1024.0f, 16.0f)
                .tooltip("Height of the beacon beam above the waypoint.")

                .category("Label Card")
                .bool("Floating Labels", () -> c.floatingPinsEnabled, v -> c.floatingPinsEnabled = v)
                .tooltip("Show the floating name/distance card above each waypoint.")
                .floatSlider("Pin Scale", () -> c.pinScale, v -> c.pinScale = v, 0.2f, 3.0f, 0.05f)
                .tooltip("Scale factor for in-world billboard pins.")
                .floatSlider("Text Scale", () -> c.textScale, v -> c.textScale = v, 0.5f, 2.0f, 0.05f)
                .tooltip("Scale factor for the label card text.")
                .floatSlider("Label Hold Distance", () -> c.labelScaleDistance, v -> c.labelScaleDistance = v, 4.0f, 128.0f, 1.0f)
                .tooltip("Past this distance labels stop shrinking and hold a readable size.")
                .floatSlider("Max Size (Close)", () -> c.labelMaxScale, v -> c.labelMaxScale = v, 1.0f, 16.0f, 0.25f)
                .tooltip("Largest on-screen label size when very close, as a multiple of the size the card holds far away. Caps the perspective blow-up right next to a waypoint.")
                .floatSlider("Min Size (Far)", () -> c.labelMinScale, v -> c.labelMinScale = v, 0.25f, 3.0f, 0.05f)
                .tooltip("Smallest on-screen label size when very far, as a multiple of the held size. 1.0 holds that size, smaller keeps shrinking to this floor.")

                .category("Integrations")
                .bool("Xaero World Map Markers", () -> c.xaeroWorldMapEnabled, v -> c.xaeroWorldMapEnabled = v)
                .tooltip("Draw your waypoints on Xaero's World Map. Only has an effect when that mod is installed.")
                .floatSlider("Xaero Marker Size", () -> c.xaeroMarkerScale, v -> c.xaeroMarkerScale = v, 0.5f, 3.0f, 0.05f)
                .tooltip("Size of the waypoint markers on Xaero's World Map, as a multiple of the default. Scales the dot and its name plate together.")

                .category("Death Waypoints")
                .bool("Enable Death Waypoints", () -> c.deathWaypointEnabled, v -> c.deathWaypointEnabled = v)
                .tooltip("Automatically place a waypoint where you die.")
                .bool("Auto-Remove on Arrival", () -> c.deathWaypointAutoRemove, v -> c.deathWaypointAutoRemove = v)
                .tooltip("Automatically delete death waypoint when approaching your corpse.")
                .doubleSlider("Auto-Remove Distance", () -> c.deathWaypointAutoRemoveDist, v -> c.deathWaypointAutoRemoveDist = v, 1.0, 32.0, 0.5)
                .tooltip("Distance in blocks to trigger death waypoint auto-removal.")
                .intSlider("Max Death Waypoints", () -> c.deathWaypointMaxCount, v -> c.deathWaypointMaxCount = v, 1, 10, 1)
                .tooltip("Number of past death points to keep (1 = latest only).")

                .build();
    }
}
