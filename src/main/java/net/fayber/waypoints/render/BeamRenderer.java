package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class BeamRenderer {
    private static final Identifier BEAM_TEXTURE = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");

    public static void renderBeam(PoseStack poseStack, SubmitNodeCollector collector, Waypoint wp, ModConfig config) {
        if (!wp.isBeaconBeam() || !config.beaconBeamEnabled) {
            return;
        }

        WaypointColor color = WaypointColor.of(wp.getEffectiveColor());
        float r = color.getRed();
        float g = color.getGreen();
        float b = color.getBlue();
        float alpha = config.beaconAlpha;
        float radius = config.beaconWidth;

        float minY = 0.0f;
        float maxY = config.beaconHeight;

        collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM_TEXTURE, true), (pose, consumer) -> {
            Matrix4f mat = pose.pose();

            // Inner core
            renderBeamQuads(mat, consumer, r, g, b, alpha, radius * 0.5f, minY, maxY);

            // Outer glowing aura
            renderBeamQuads(mat, consumer, r, g, b, alpha * 0.4f, radius, minY, maxY);
        });
    }

    private static void renderBeamQuads(Matrix4f mat, VertexConsumer consumer, float r, float g, float b, float a, float rad, float minY, float maxY) {
        // 4 vertical quad faces forming a cross/box beam
        // Face 1 (North-South)
        addVertex(mat, consumer, -rad, minY, 0, r, g, b, a, 0, 0);
        addVertex(mat, consumer, rad, minY, 0, r, g, b, a, 1, 0);
        addVertex(mat, consumer, rad, maxY, 0, r, g, b, 0.0f, 1, 1);
        addVertex(mat, consumer, -rad, maxY, 0, r, g, b, 0.0f, 0, 1);

        // Face 2 (East-West)
        addVertex(mat, consumer, 0, minY, -rad, r, g, b, a, 0, 0);
        addVertex(mat, consumer, 0, minY, rad, r, g, b, a, 1, 0);
        addVertex(mat, consumer, 0, maxY, rad, r, g, b, 0.0f, 1, 1);
        addVertex(mat, consumer, 0, maxY, -rad, r, g, b, 0.0f, 0, 1);

        // Face 3 (Reverse North-South)
        addVertex(mat, consumer, rad, minY, 0, r, g, b, a, 0, 0);
        addVertex(mat, consumer, -rad, minY, 0, r, g, b, a, 1, 0);
        addVertex(mat, consumer, -rad, maxY, 0, r, g, b, 0.0f, 1, 1);
        addVertex(mat, consumer, rad, maxY, 0, r, g, b, 0.0f, 0, 1);

        // Face 4 (Reverse East-West)
        addVertex(mat, consumer, 0, minY, rad, r, g, b, a, 0, 0);
        addVertex(mat, consumer, 0, minY, -rad, r, g, b, a, 1, 0);
        addVertex(mat, consumer, 0, maxY, -rad, r, g, b, 0.0f, 1, 1);
        addVertex(mat, consumer, 0, maxY, rad, r, g, b, 0.0f, 0, 1);
    }

    private static void addVertex(Matrix4f mat, VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        consumer.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(0, 1, 0);
    }
}
