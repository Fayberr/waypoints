package net.fayber.waypoints.compat.xaero;

import net.fayber.waypoints.model.Waypoint;
import net.minecraft.client.Minecraft;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderLocation;

/**
 * Tells Xaero's World Map where each waypoint sits and how much room its marker needs.
 *
 * <p>Coordinates: {@code getRenderX/Y/Z} are world block coordinates. The framework turns them
 * into screen space itself ({@code (renderX / dimDiv - cameraX) * zoom}), so nothing here knows
 * about panning or zoom. {@link WaypointMapRenderer#shouldBeDimScaled()} is false and the provider
 * only ever yields waypoints belonging to the dimension currently on screen, so {@code dimDiv} is
 * 1 and the raw coordinates are already correct.
 *
 * <p>Box coordinates are element-local pixels around the marker origin (negative is left/up),
 * exactly like Xaero's own readers. The render box is the cull box (it must cover the label, or
 * markers pop out early when scrolled to the screen edge); the interaction box is deliberately
 * only the dot, so hovering a label does not steal the hover from a marker underneath it.
 */
public final class WaypointMapReader extends ElementReader<Waypoint, WaypointMapContext, WaypointMapRenderer> {
    /** Radius of the outlined dot drawn at the waypoint position, in map-GUI pixels. */
    static final int MARKER_RADIUS = 5;
    /** Vertical gap between the top of the dot and the bottom of the name plate. */
    static final int LABEL_GAP = 3;
    /** Padding either side of the name inside its plate. */
    static final int LABEL_PADDING = 2;

    @Override
    public boolean isHidden(Waypoint waypoint, WaypointMapContext context) {
        return !waypoint.isVisible() || !waypoint.getDimension().equals(context.dimensionId());
    }

    /** Enables hover highlighting; right-click menus stay off (isRightClickValid defaults false). */
    @Override
    public boolean isInteractable(ElementRenderLocation location, Waypoint waypoint) {
        return true;
    }

    @Override
    public double getRenderX(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return waypoint.getX();
    }

    @Override
    public double getRenderY(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return waypoint.getY();
    }

    @Override
    public boolean hasYCoordinate() {
        return true;
    }

    @Override
    public double getRenderZ(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return waypoint.getZ();
    }

    @Override
    public int getInteractionBoxLeft(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return -MARKER_RADIUS - 1;
    }

    @Override
    public int getInteractionBoxRight(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return MARKER_RADIUS + 1;
    }

    @Override
    public int getInteractionBoxTop(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return -MARKER_RADIUS - 1;
    }

    @Override
    public int getInteractionBoxBottom(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return MARKER_RADIUS + 1;
    }

    @Override
    public int getRenderBoxLeft(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return -halfPlateWidth(waypoint);
    }

    @Override
    public int getRenderBoxRight(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return halfPlateWidth(waypoint);
    }

    @Override
    public int getRenderBoxTop(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return -(MARKER_RADIUS + LABEL_GAP + lineHeight() + 2);
    }

    @Override
    public int getRenderBoxBottom(Waypoint waypoint, WaypointMapContext context, float partialTicks) {
        return MARKER_RADIUS + 1;
    }

    @Override
    public int getLeftSideLength(Waypoint waypoint, Minecraft minecraft) {
        return MARKER_RADIUS + 4 + minecraft.font.width(waypoint.getName());
    }

    @Override
    public String getMenuName(Waypoint waypoint) {
        return waypoint.getName();
    }

    @Override
    public String getFilterName(Waypoint waypoint) {
        return waypoint.getName();
    }

    @Override
    public int getMenuTextFillLeftPadding(Waypoint waypoint) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(Waypoint waypoint) {
        return opaque(waypoint.getEffectiveColor());
    }

    /**
     * False: the marker is drawn at a fixed pixel size, so its boxes must not be multiplied by the
     * map's optional element scale (that scale is only applied by renderers that scale their own
     * pose by it, which we do not).
     */
    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    /** Half the width of the name plate, never narrower than the dot itself. */
    static int halfPlateWidth(Waypoint waypoint) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null || !waypoint.isShowLabel()) {
            return MARKER_RADIUS + 1;
        }
        int half = minecraft.font.width(waypoint.getName()) / 2 + LABEL_PADDING + 1;
        return Math.max(MARKER_RADIUS + 1, half);
    }

    private static int lineHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.font == null ? 9 : minecraft.font.lineHeight;
    }

    /** Waypoint colours are stored as ARGB but map markers always draw fully opaque. */
    static int opaque(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }
}
