package net.fayber.waypoints.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.fayber.waypoints.render.EdgePointerRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Screen-space HUD pass for off-screen waypoints: clamped edge-pointer arrows
 * pointing towards waypoints currently outside the camera's field of view.
 *
 * On-screen billboard pins/name/distance text are handled in-world (in 3D,
 * so they scale and occlude naturally) by {@link net.fayber.waypoints.render.PinRenderer}.
 * This pass only covers the gap that in-world rendering can't: indicators for
 * waypoints the camera isn't currently looking at.
 */
public class WaypointHudElement implements HudElement {

    private static final int EDGE_MARGIN = 12;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ModConfig config = ConfigManager.get();
        if (!config.showOffscreenPointers) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) {
            return;
        }
        // Don't clutter the overlay while a screen (e.g. our own menu) is open.
        if (mc.gui.screen() != null) {
            return;
        }

        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) {
            return;
        }

        String currentDim = mc.level.dimension().identifier().toString();
        List<Waypoint> waypoints = WaypointStore.get().getVisibleForDimension(currentDim);
        if (waypoints.isEmpty()) {
            return;
        }

        Vec3 camPos = camera.position();
        Vector3fc forward = camera.forwardVector();
        Vector3fc left = camera.leftVector();
        Vector3fc up = camera.upVector();

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        double fovRad = Math.toRadians(Math.max(1.0f, camera.getFov()));
        double tanHalfFovY = Math.tan(fovRad / 2.0);
        double aspect = screenHeight > 0 ? (double) screenWidth / screenHeight : 1.0;
        double tanHalfFovX = tanHalfFovY * aspect;

        for (Waypoint wp : waypoints) {
            double dx = wp.getX() - camPos.x;
            double dy = wp.getY() - camPos.y;
            double dz = wp.getZ() - camPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (config.renderDistance > 0 && distance > config.renderDistance) {
                continue;
            }

            double f = dx * forward.x() + dy * forward.y() + dz * forward.z();
            double l = dx * left.x() + dy * left.y() + dz * left.z();
            double u = dx * up.x() + dy * up.y() + dz * up.z();

            boolean inFront = f > 0.05;
            double ndcX;
            double ndcY;
            if (inFront) {
                ndcX = (-l / f) / tanHalfFovX;
                ndcY = (u / f) / tanHalfFovY;
            } else {
                double horizontalMag = Math.sqrt(f * f + l * l);
                double bearing = Math.atan2(-l, f);
                double verticalAngle = Math.atan2(u, Math.max(0.0001, horizontalMag));
                ndcX = Math.sin(bearing) * 4.0;
                ndcY = Math.sin(verticalAngle) * 4.0;
            }

            boolean onScreen = inFront && ndcX >= -1.0 && ndcX <= 1.0 && ndcY >= -1.0 && ndcY <= 1.0;
            if (onScreen) {
                // Already covered by the in-world 3D billboard pin/text.
                continue;
            }

            double dirX = ndcX * (screenWidth / 2.0);
            double dirY = -ndcY * (screenHeight / 2.0);

            EdgePointerRenderer.ClampResult clamp = EdgePointerRenderer.clampToScreenEdge(
                    centerX, centerY, dirX, dirY, EDGE_MARGIN, screenWidth, screenHeight);

            int argb = wp.getEffectiveColor();
            EdgePointerRenderer.drawArrow(graphics, clamp.x, clamp.y, clamp.side, argb);

            if (wp.isShowDistance()) {
                String distText = formatDistance(distance);
                int labelY = switch (clamp.side) {
                    case UP -> clamp.y + 10;
                    case DOWN -> clamp.y - 12;
                    default -> clamp.y + 8;
                };
                graphics.centeredText(mc.font, distText, clamp.x, labelY, 0xFFFFFFFF);
            }
        }
    }

    private static String formatDistance(double distance) {
        if (distance >= 1000) {
            return String.format("%.1fkm", distance / 1000.0);
        }
        return String.format("%dm", Math.round(distance));
    }
}
