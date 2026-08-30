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
    // Distance in blocks at which the label stops shrinking with perspective: past this the
    // label holds a constant readable on-screen size instead of collapsing to a pixel at
    // long range. Closer than this it scales normally.
    public float labelScaleDistance = 24.0f;
    // On-screen size clamps for the label card, as multipliers of the size the card holds past
    // labelScaleDistance. labelMaxScale caps how big the card gets when very close (perspective
    // otherwise blows it up right next to it); labelMinScale floors how small it gets when very
    // far. Defaults 4.0 / 1.0: the far behavior stays identical to the plain hold, the close
    // blowup is capped at 4x the held size. labelMaxScale <= 0 means uncapped.
    public float labelMinScale = 1.0f;
    public float labelMaxScale = 4.0f;

    // Death Waypoints
    public boolean deathWaypointEnabled = true;
    public boolean deathWaypointAutoRemove = true;
    public double deathWaypointAutoRemoveDist = 6.0;
    public int deathWaypointMaxCount = 1;
}
