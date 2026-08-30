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

    // Death Waypoints
    public boolean deathWaypointEnabled = true;
    public boolean deathWaypointAutoRemove = true;
    public double deathWaypointAutoRemoveDist = 6.0;
    public int deathWaypointMaxCount = 1;
}
