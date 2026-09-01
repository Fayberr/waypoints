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

import java.util.ArrayList;
import java.util.List;

/**
 * Hooked from the Fabric COLLECT_SUBMITS event (registered in WaypointsClient). Submits the
 * beam geometry and the three card pieces into submit-order buckets:
 *
 * - bucket 0 (default collector): beam + glow + every card's depth-writing underlay
 * - bucket 2i+1: the visible card fill/border of the i-th farthest visible waypoint
 * - bucket 2i+2: that waypoint's name/distance text
 *
 * The translucent feature stage draws buckets in ascending order, so the beam glow can never
 * blend over a card and the glyphs always draw on top of their own fill. The per-waypoint
 * allocation is the occlusion mechanism: the see-through card and text do no depth testing
 * (that is what makes them render through walls), so the only way a nearer waypoint can fully
 * cover a farther one, text included, is painter's order. Hence the far-to-near sort and one
 * bucket pair per waypoint; see PinRenderer's class doc for the details.
 */
public class WaypointRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaypointRenderer.class);

    /**
     * How many waypoints get their own card bucket pair (2 per waypoint, buckets up to 2N+1).
     * Beyond this many visible waypoints the tail shares the last pair and merely keeps the
     * old submission-order layering among themselves; real worlds stay far below this.
     */
    private static final int MAX_CARD_SLOTS = 32;

    private static boolean warnedNoBuckets = false;

    /** One waypoint that passed the cull, with its camera distance. */
    private record Renderable(Waypoint wp, double dist) {
    }

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
        if (storage == null && !warnedNoBuckets) {
            warnedNoBuckets = true;
            LOGGER.warn("SubmitNodeStorage not available; waypoint label cards render without their card body");
        }

        // Cull first, then sort far to near: with the see-through (depth-test-free) card and
        // text, submission order is the only layering, and painter's algorithm needs the far
        // ones submitted (drawn) first so the near ones draw over them.
        List<Renderable> renderables = new ArrayList<>();
        for (Waypoint wp : WaypointStore.get().getVisibleForDimension(currentDim)) {
            double dist = cullAndDistance(wp, camPos, config);
            if (dist >= 0.0) {
                renderables.add(new Renderable(wp, dist));
            }
        }
        renderables.sort((a, b) -> Double.compare(b.dist(), a.dist()));

        int slot = 0;
        for (Renderable renderable : renderables) {
            Waypoint wp = renderable.wp();
            double dist = renderable.dist();

            double dx = Math.floor(wp.getX()) + 0.5 - camPos.x;
            double dy = Math.floor(wp.getY()) + 0.5 - camPos.y;
            double dz = Math.floor(wp.getZ()) + 0.5 - camPos.z;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            // 1. Sleek glowing vertical beacon beam (bucket 0).
            BeamRenderer.renderBeam(poseStack, collector, wp, config);

            // 2. Faint depth-writing card underlay (bucket 0, invisible; only writes depth).
            //    Underlay-vs-underlay needs no sorting: they depth-test against each other and
            //    the nearer card's plane wins the depth buffer either way.
            PinRenderer.renderUnderlay(poseStack, collector, camera, wp, config, dist);

            // 3+4. Card body and text, each waypoint in its own bucket pair (2i+1 / 2i+2) so a
            //     nearer waypoint's card and text draw over a farther waypoint's card and text.
            if (storage != null) {
                int cappedSlot = Math.min(slot, MAX_CARD_SLOTS - 1);
                OrderedSubmitNodeCollector bodyCollector = storage.order(2 * cappedSlot + 1);
                OrderedSubmitNodeCollector textCollector = storage.order(2 * cappedSlot + 2);
                PinRenderer.renderCardBody(bodyCollector, poseStack, camera, wp, config, dist);
                PinRenderer.renderCardText(textCollector, poseStack, camera, wp, config, dist);
            } else {
                PinRenderer.renderCardText(collector, poseStack, camera, wp, config, dist);
            }

            poseStack.popPose();
            slot++;
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
