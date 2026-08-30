package net.fayber.waypoints.compat.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.model.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/**
 * Draws our waypoints onto Xaero's World Map.
 *
 * <p>Xaero's element framework has already translated the pose to the waypoint's screen position
 * by the time {@link #renderElement} runs, so everything here is drawn in element-local pixels
 * around the origin: an outlined dot in the waypoint colour, with the name on a dark plate above
 * it. Only vanilla primitives via {@link MapElementGraphics} are used (no Xaero texture atlases,
 * no {@code MultiTextureRenderTypeRenderer}), which keeps this independent of Xaero's internal
 * render plumbing and therefore much less likely to break on their next update.
 *
 * <p>This class is only ever loaded when Xaero's World Map is installed: see
 * {@link XaeroWorldMapCompat}.
 */
public final class WaypointMapRenderer extends ElementRenderer<Waypoint, WaypointMapContext, WaypointMapRenderer> {
    /** Ring around every marker, so bright waypoint colours stay readable on bright terrain. */
    private static final int OUTLINE = 0xFF12141B;
    /** Death waypoints get a red ring instead, matching the in-world card accent. */
    private static final int DEATH_OUTLINE = 0xFFFF4D4D;
    private static final int HOVER_RING = 0xFFF0F0F0;
    private static final int PLATE_BACKGROUND = 0xC812141B;
    private static final int PLATE_TEXT = 0xFFFFFFFF;

    /**
     * Draw order among map elements. Above Xaero's tracked players (200) so our markers are not
     * hidden underneath player heads on a busy server.
     */
    private static final int ORDER = 250;

    public WaypointMapRenderer() {
        super(new WaypointMapContext(), new WaypointMapProvider(), new WaypointMapReader());
    }

    /**
     * False: the provider only yields waypoints from the dimension currently on screen, so their
     * coordinates need no nether/overworld conversion. Leaving the default (true) would divide
     * every coordinate by Xaero's dimension divisor and scatter the markers when the map is
     * viewed with dimension scaling on.
     */
    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean hovered) {
        return location == ElementRenderLocation.WORLD_MAP && ConfigManager.get().xaeroWorldMapEnabled;
    }

    @Override
    public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource bufferSource,
                          MultiTextureRenderTypeRendererProvider rendererProvider, boolean hovered) {
        // Runs before the provider is asked for elements, so this is where the frame's dimension
        // is handed to the provider and the reader.
        this.context.setDimension(info.mapDimension);
    }

    @Override
    public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource bufferSource,
                           MultiTextureRenderTypeRendererProvider rendererProvider, boolean hovered) {
        // Nothing to tear down: we only used the shared vanilla buffers, which Xaero flushes.
    }

    @Override
    public void renderElementShadow(Waypoint waypoint, boolean hovered, float screenSizeBasedScale,
                                    double subPixelX, double subPixelZ, ElementRenderInfo info,
                                    MapElementGraphics graphics, MultiBufferSource.BufferSource bufferSource,
                                    MultiTextureRenderTypeRendererProvider rendererProvider) {
        // No drop shadow: the dark outline ring already separates markers from the terrain.
    }

    @Override
    public boolean renderElement(Waypoint waypoint, boolean hovered, double depth, float screenSizeBasedScale,
                                 double subPixelX, double subPixelZ, ElementRenderInfo info,
                                 MapElementGraphics graphics, MultiBufferSource.BufferSource bufferSource,
                                 MultiTextureRenderTypeRendererProvider rendererProvider) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        // Xaero spaces elements out in depth so later ones win the depth test; skipping this makes
        // overlapping markers flicker.
        pose.translate(0.0, 0.0, depth);

        int radius = WaypointMapReader.MARKER_RADIUS;
        if (hovered) {
            disc(graphics, radius + 2, HOVER_RING);
        }
        disc(graphics, radius, waypoint.isDeathWaypoint() ? DEATH_OUTLINE : OUTLINE);
        disc(graphics, radius - 2, WaypointMapReader.opaque(waypoint.getEffectiveColor()));

        Font font = minecraft.font;
        if (waypoint.isShowLabel() && font != null) {
            String name = waypoint.getName();
            int width = font.width(name);
            int textY = -(radius + WaypointMapReader.LABEL_GAP + font.lineHeight);
            int half = width / 2;
            graphics.fill(-half - WaypointMapReader.LABEL_PADDING, textY - 1,
                    half + WaypointMapReader.LABEL_PADDING, textY + font.lineHeight, PLATE_BACKGROUND);
            graphics.drawString(font, name, -half, textY, PLATE_TEXT, false);
        }

        pose.popPose();
        return true;
    }

    /**
     * Filled circle from horizontal bands. {@link MapElementGraphics} only offers axis-aligned
     * rectangles, and a stack of rows sampled at each row's centre gives a clean, vertically
     * symmetric dot for the small radii used here.
     */
    private static void disc(MapElementGraphics graphics, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        double rSquared = (double) radius * radius;
        for (int y = -radius; y < radius; y++) {
            double centre = y + 0.5;
            int half = (int) Math.round(Math.sqrt(Math.max(0.0, rSquared - centre * centre)));
            if (half <= 0) {
                continue;
            }
            graphics.fill(-half, y, half, y + 1, color);
        }
    }
}
