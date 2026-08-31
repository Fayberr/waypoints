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
    NO_PERMISSION("On Servers Without Operator"),
    /**
     * Shown only where the player can actually teleport: single player with cheats on, or a
     * server where they are an operator. Hidden in single player without cheats.
     */
    PERMISSION_ONLY("Without Cheats or Operator");

    /** Display name in the config screen. */
    public final String label;

    TeleportButtonVisibility(String label) {
        this.label = label;
    }

    /**
     * Whether the teleport button is visible in a given context.
     *
     * @param singleplayer true when the integrated server is running (own world, LAN included)
     * @param canTeleport true when the server grants the teleport command to this player
     *     (operator on vanilla servers, cheats enabled in singleplayer). The client-side
     *     permission set cannot provide this: only the integrated server ever populates it, no
     *     packet syncs permissions on remote servers, so callers derive it from the synced
     *     command tree.
     */
    public boolean shows(boolean singleplayer, boolean canTeleport) {
        return switch (this) {
            case NEVER -> true;
            case ALWAYS -> false;
            case SINGLEPLAYER -> !singleplayer;
            case SERVERS -> singleplayer;
            case NO_PERMISSION -> singleplayer || canTeleport;
            case PERMISSION_ONLY -> canTeleport;
        };
    }

    @Override
    public String toString() {
        // Display name; Gson serialises name(), not toString, so this is safe for config files.
        return this.label;
    }
}
