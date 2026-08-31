package net.fayber.waypoints.gui.style;

/**
 * The waypoint screens' palette: one neutral dark ramp and nothing else.
 *
 * <p>Every surface grey has equal R/G/B, so no part of the chrome reads as tinted. There is
 * deliberately <b>no accent colour</b>: emphasis is carried by lightness alone (a brighter card, a
 * near-white "confirm" button), which keeps the screens quiet and lets the only real colour on
 * them be the waypoints' own colours and the colour picker.
 *
 * <p>This mirrors {@code net.fayber.fayberconfig.gui.GuiUtil} on purpose, so the waypoint list and
 * the settings screen look like one product. It is duplicated rather than imported because Fayber
 * Config is an optional (compileOnly) dependency and these screens are core: they have to render
 * with the library absent.
 */
public final class Theme {
    private Theme() {
    }

    /** Dim laid over the world behind the screens. */
    public static final int SCRIM = 0xB3000000;

    public static final int CARD = 0xFF1A1A1A;
    public static final int CARD_HOVER = 0xFF222222;
    public static final int CARD_BORDER = 0xFF262626;
    public static final int CARD_BORDER_HOVER = 0xFF3A3A3A;
    /** Fill for a card that is switched on / currently selected. */
    public static final int CARD_ACTIVE = 0xFF2E2E2E;

    /** Near-white fill for the single confirming button on a screen. */
    public static final int STRONG = 0xFFE6E6E6;
    public static final int STRONG_HOVER = 0xFFFFFFFF;
    /** Label colour for text sitting on {@link #STRONG}. */
    public static final int TEXT_ON_STRONG = 0xFF121212;

    public static final int TEXT = 0xFFF0F0F0;
    public static final int TEXT_SECONDARY = 0xFFA3A3A3;
    public static final int TEXT_MUTED = 0xFF6E6E6E;

    public static final int TRACK = 0xFF3A3A3A;
    public static final int SCROLLBAR = 0xFF3A3A3A;
    public static final int SCROLLBAR_HOVER = 0xFF4D4D4D;

    /** Corner radius shared by every card on these screens. */
    public static final float RADIUS = 6.0f;
    /** Corner radius for the small square icon cards. */
    public static final float RADIUS_SMALL = 5.0f;
}
