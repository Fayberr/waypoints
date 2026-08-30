package net.fayber.waypoints.model;

public enum WaypointIcon {
    PIN("Pin", "📍"),
    STAR("Star", "⭐"),
    SKULL("Skull", "💀"),
    HOUSE("House", "🏠"),
    DIAMOND("Diamond", "💎"),
    FLAG("Flag", "🚩");

    private final String displayName;
    private final String emoji;

    WaypointIcon(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }
}
