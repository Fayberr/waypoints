package net.fayber.waypoints.model;

import java.util.UUID;

public class Waypoint {
    private UUID id;
    private String name;
    private double x;
    private double y;
    private double z;
    private String dimension;
    private int colorArgb;
    private boolean chroma;
    private boolean visible;
    private boolean showBeam;
    private boolean showLabel;
    private boolean showDistance;
    private WaypointIcon icon;
    private long createdAt;
    private boolean deathWaypoint;

    public Waypoint(UUID id, String name, double x, double y, double z, String dimension, int colorArgb, boolean chroma) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name != null ? name : "Waypoint";
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension != null ? dimension : "minecraft:overworld";
        this.colorArgb = colorArgb;
        this.chroma = chroma;
        this.visible = true;
        this.showBeam = true;
        this.showLabel = true;
        this.showDistance = true;
        this.icon = WaypointIcon.PIN;
        this.createdAt = System.currentTimeMillis();
        this.deathWaypoint = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public int getColor() {
        return colorArgb;
    }

    public void setColorArgb(int colorArgb) {
        this.colorArgb = colorArgb;
    }

    public void setColor(int color) {
        this.colorArgb = color;
    }

    public int getEffectiveColor() {
        return new WaypointColor(colorArgb, chroma).getEffectiveArgb();
    }

    public boolean isChroma() {
        return chroma;
    }

    public void setChroma(boolean chroma) {
        this.chroma = chroma;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isShowBeam() {
        return showBeam;
    }

    public boolean isBeaconBeam() {
        return showBeam;
    }

    public void setShowBeam(boolean showBeam) {
        this.showBeam = showBeam;
    }

    public void setBeaconBeam(boolean beaconBeam) {
        this.showBeam = beaconBeam;
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }

    public boolean isShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
    }

    public WaypointIcon getIcon() {
        return icon;
    }

    public void setIcon(WaypointIcon icon) {
        this.icon = icon;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeathWaypoint() {
        return deathWaypoint;
    }

    public void setDeathWaypoint(boolean deathWaypoint) {
        this.deathWaypoint = deathWaypoint;
    }
}
