package net.fayber.waypoints.config;

/**
 * When the "teleport to waypoint" button on a waypoint card is shown. The names follow how the
 * setting is phrased in the config screen (which contexts hide the button); {@link #shows} is
 * the inverted answer, "is the button visible right now".
 */
public enum TeleportButtonVisibility {
    /** Never hidden (default). */
    NEVER("Never"),
    /** Hidden everywhere. */
    ALWAYS("Always"),
    /** Hidden in single player, shown on servers. */
    SINGLEPLAYER("In Single Player"),
    /** Hidden on servers, shown in single player. */
    SERVERS("On Servers"),
    /** Hidden on servers where the player has no operator rights; always shown in single player. */
    NO_PERMISSION("Without Operator");

    /** Display name in the config screen. */
    public final String label;

    TeleportButtonVisibility(String label) {
        this.label = label;
    }

    /**
     * Whether the teleport button is visible in a given context.
     *
     * @param singleplayer true when the integrated server is running (own world, LAN included)
     * @param hasPermission true when the server would allow the player to run the teleport
     *     command itself (operator, permission level 2+)
     */
    public boolean shows(boolean singleplayer, boolean hasPermission) {
        return switch (this) {
            case NEVER -> true;
            case ALWAYS -> false;
            case SINGLEPLAYER -> !singleplayer;
            case SERVERS -> singleplayer;
            case NO_PERMISSION -> singleplayer || hasPermission;
        };
    }

    @Override
    public String toString() {
        // Display name; Gson serialises name(), not toString, so this is safe for config files.
        return this.label;
    }
}
