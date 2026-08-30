package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
        String currentDim = mc.level.dimension().identifier().toString();
        ModConfig config = ConfigManager.get();

        List<Waypoint> visibleWaypoints = WaypointStore.get().getVisibleForDimension(currentDim);

        for (Waypoint wp : visibleWaypoints) {
            double dx = wp.getX() - camPos.x;
            double dy = wp.getY() - camPos.y;
            double dz = wp.getZ() - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            double dist = Math.sqrt(distSq);

            if (config.renderDistance > 0 && dist > config.renderDistance) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            // Scaffold hook for detailed rendering
            // BeamRenderer.renderBeam(poseStack, context, wp, config);
            // PinRenderer.renderPin(poseStack, context, camera, wp, config, dist);

            poseStack.popPose();
        }
    }
}
