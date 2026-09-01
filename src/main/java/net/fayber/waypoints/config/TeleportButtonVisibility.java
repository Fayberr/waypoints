package net.fayber.waypoints.config;

/**
 * When the "teleport to waypoint" button on a waypoint card is hidden. The names follow how the
 * setting is phrased in the config screen; {@link #shows} is the inverted answer.
 */
public enum TeleportButtonVisibility {
    NEVER("Never"),
    ALWAYS("Always"),
    SINGLEPLAYER("In Single Player"),
    SERVERS("On Servers"),
    NO_PERMISSION("On Servers Without Operator"),
    /**
     * Shown only where the player can actually teleport: single player with cheats on, or a
     * server where they are an operator.
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
     * @param canTeleport true when the server grants the teleport command to this player.
     *     Callers derive this from the synced command tree: the client-side permission set is
     *     only ever populated by the integrated server, nothing syncs permissions from remote
     *     servers.
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
