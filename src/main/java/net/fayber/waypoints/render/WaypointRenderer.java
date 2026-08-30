package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaypointRenderer {

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Camera camera = context.gameRenderer().mainCamera();
        if (camera == null) {
            return;
        }

        Vec3 camPos = camera.position();
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        String currentDim = mc.level.dimension().identifier().toString();
        ModConfig config = ConfigManager.get();

        List<Waypoint> visibleWaypoints = WaypointStore.get().getVisibleForDimension(currentDim);

        for (Waypoint wp : visibleWaypoints) {
            // Snap to the centre of the containing block so the beam/pin are visually centered on
            // the block, even if the stored coordinate is fractional (e.g. created at the player's
            // exact eye position). Idempotent for already-normalized waypoints.
            double bx = Math.floor(wp.getX()) + 0.5;
            double by = Math.floor(wp.getY()) + 0.5;
            double bz = Math.floor(wp.getZ()) + 0.5;

            double dx = bx - camPos.x;
            double dy = by - camPos.y;
            double dz = bz - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            double dist = Math.sqrt(distSq);

            if (config.renderDistance > 0 && dist > config.renderDistance) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            // 1. Render sleek glowing vertical beacon beam
            BeamRenderer.renderBeam(poseStack, collector, wp, config);

            // 2. Render billboard pin icon & floating distance text
            PinRenderer.renderPin(poseStack, collector, camera, wp, config, dist);

            poseStack.popPose();
        }
    }
}
