package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Hooked from the Fabric COLLECT_SUBMITS event (registered in WaypointsClient). Submits the
 * beam geometry and the three card pieces into separate submit-order buckets:
 *
 * - bucket 0 (default collector): beam + glow + the card's depth-writing underlay
 * - bucket 1 (PinRenderer.ORDER_CARD_BODY): the visible card fill/border
 * - bucket 2 (PinRenderer.ORDER_CARD_TEXT): the card's name/distance text
 *
 * The translucent feature stage draws buckets in ascending order, so the beam glow can never
 * blend over the card and the glyphs always draw on top of the fill (see PinRenderer's class
 * doc for the version-specific mechanics).
 */
public class WaypointRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointRenderer.class);

    private static boolean warnedNoBuckets = false;

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Camera camera = context.gameRenderer().getMainCamera();
        if (camera == null) {
            return;
        }

        Vec3 camPos = camera.position();
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        String currentDim = mc.level.dimension().identifier().toString();
        ModConfig config = ConfigManager.get();

        // The card pieces need their own order buckets, but the plain SubmitNodeCollector
        // interface only reaches bucket 0. Fabric passes the LevelRenderer's own
        // SubmitNodeStorage here, so cast to it; if a future version breaks that assumption,
        // degrade gracefully to bucket-0-only rendering (underlay + text, no card body).
        SubmitNodeStorage storage = collector instanceof SubmitNodeStorage s ? s : null;
        OrderedSubmitNodeCollector bodyCollector = storage != null ? storage.order(PinRenderer.ORDER_CARD_BODY) : null;
        OrderedSubmitNodeCollector textCollector = storage != null ? storage.order(PinRenderer.ORDER_CARD_TEXT) : collector;
        if (storage == null && !warnedNoBuckets) {
            warnedNoBuckets = true;
            LOGGER.warn("SubmitNodeStorage not available; waypoint label cards render without their card body");
        }

        List<Waypoint> visibleWaypoints = WaypointStore.get().getVisibleForDimension(currentDim);

        for (Waypoint wp : visibleWaypoints) {
            double dist = cullAndDistance(wp, camPos, config);
            if (dist < 0.0) {
                continue;
            }

            double dx = Math.floor(wp.getX()) + 0.5 - camPos.x;
            double dy = Math.floor(wp.getY()) + 0.5 - camPos.y;
            double dz = Math.floor(wp.getZ()) + 0.5 - camPos.z;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            // 1. Sleek glowing vertical beacon beam (bucket 0).
            BeamRenderer.renderBeam(poseStack, collector, wp, config);

            // 2. Faint depth-writing card underlay (bucket 0, invisible; only writes depth).
            PinRenderer.renderUnderlay(poseStack, collector, camera, wp, config, dist);

            // 3. Visible card body (bucket 1) - after every bucket-0 draw, so the glow never
            //    blends over the card.
            if (bodyCollector != null) {
                PinRenderer.renderCardBody(bodyCollector, poseStack, camera, wp, config, dist);
            }

            // 4. Card text (bucket 2) - after the card body.
            PinRenderer.renderCardText(textCollector, poseStack, camera, wp, config, dist);

            poseStack.popPose();
        }
    }

    /**
     * Shared visibility gate: block-center snap + distance cull. Returns the distance to the
     * waypoint's block centre, or -1 when the waypoint is culled (beyond renderDistance).
     */
    private static double cullAndDistance(Waypoint wp, Vec3 camPos, ModConfig config) {
        // Snap to the centre of the containing block so the beam/card are visually centered on
        // the block, even if the stored coordinate is fractional (e.g. created at the player's
        // exact eye position). Idempotent for already-normalized waypoints.
        double bx = Math.floor(wp.getX()) + 0.5;
        double by = Math.floor(wp.getY()) + 0.5;
        double bz = Math.floor(wp.getZ()) + 0.5;

        double dx = bx - camPos.x;
        double dy = by - camPos.y;
        double dz = bz - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (config.renderDistance > 0 && dist > config.renderDistance) {
            return -1.0;
        }
        return dist;
    }
}
