package net.fayber.waypoints.config;

public class ModConfig {
    public int renderDistance = 10000;
    public boolean beaconBeamEnabled = true;
    public float beaconWidth = 0.20f;
    public float beaconAlpha = 0.65f;
    public float beaconHeight = 384.0f;

    public boolean floatingPinsEnabled = true;
    public float pinScale = 1.0f;
    public float textScale = 1.0f;
    public boolean alwaysOnTop = true;
    public boolean showOffscreenPointers = true;
    // Hides the teleport button on a waypoint card; see TeleportButtonVisibility.
    public TeleportButtonVisibility teleportButtonVisibility = TeleportButtonVisibility.NEVER;
    // Distance in blocks past which the label stops shrinking with perspective and holds a
    // constant readable on-screen size. Closer than this it scales normally.
    public float labelScaleDistance = 24.0f;
    // Size clamps for the label card past labelScaleDistance, as multipliers of the held size.
    // labelMaxScale caps the blowup when very close, labelMinScale floors the far size.
    // labelMaxScale <= 0 means uncapped.
    public float labelMinScale = 1.0f;
    public float labelMaxScale = 4.0f;

    // Integrations
    // Draw waypoints on Xaero's World Map (only has an effect when that mod is installed).
    public boolean xaeroWorldMapEnabled = true;
    // Marker size on Xaero's World Map as a multiplier of the default. Scales the dot, the name
    // plate and the cull/hover boxes together.
    public float xaeroMarkerScale = 1.0f;

    // Death waypoints
    public boolean deathWaypointEnabled = true;
    public boolean deathWaypointAutoRemove = true;
    public double deathWaypointAutoRemoveDist = 6.0;
    public int deathWaypointMaxCount = 1;
}
