package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;

public class BeamRenderer {
    // Reuse vanilla's own beacon beam texture constant instead of hardcoding the path:
    // a hand-typed "textures/entity/beacon_beam.png" does NOT exist in this version (the real
    // asset lives under textures/entity/beacon/beacon_beam.png), which made every beam render
    // as the missing-texture checkerboard. BeaconRenderer.BEAM_LOCATION is guaranteed correct.
    private static final net.minecraft.resources.Identifier BEAM_TEXTURE = BeaconRenderer.BEAM_LOCATION;

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

        // Per SPEC.md: beam spans from y-128 to y+beaconHeight (relative to the waypoint), with
        // a smooth fade at both the top and the bottom rather than an abrupt cutoff.
        float minY = -128.0f;
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
        float fade = Math.min(8.0f, (maxY - minY) / 4.0f);
        float bottomFadeEnd = minY + fade;
        float topFadeStart = maxY - fade;

        if (topFadeStart <= bottomFadeEnd) {
            // Degenerate/very short beam: fall back to a single top-only fade so we don't emit
            // an inverted or zero-height segment.
            addFaceSegment(mat, consumer, r, g, b, rad, minY, a, maxY, 0.0f);
            return;
        }

        // Bottom fade-in
        addFaceSegment(mat, consumer, r, g, b, rad, minY, 0.0f, bottomFadeEnd, a);
        // Solid middle
        addFaceSegment(mat, consumer, r, g, b, rad, bottomFadeEnd, a, topFadeStart, a);
        // Top fade-out
        addFaceSegment(mat, consumer, r, g, b, rad, topFadeStart, a, maxY, 0.0f);
    }

    private static void addFaceSegment(Matrix4f mat, VertexConsumer consumer, float r, float g, float b, float rad, float y0, float a0, float y1, float a1) {
        // Face 1 (North-South)
        addVertex(mat, consumer, -rad, y0, 0, r, g, b, a0, 0, 0);
        addVertex(mat, consumer, rad, y0, 0, r, g, b, a0, 1, 0);
        addVertex(mat, consumer, rad, y1, 0, r, g, b, a1, 1, 1);
        addVertex(mat, consumer, -rad, y1, 0, r, g, b, a1, 0, 1);

        // Face 2 (East-West)
        addVertex(mat, consumer, 0, y0, -rad, r, g, b, a0, 0, 0);
        addVertex(mat, consumer, 0, y0, rad, r, g, b, a0, 1, 0);
        addVertex(mat, consumer, 0, y1, rad, r, g, b, a1, 1, 1);
        addVertex(mat, consumer, 0, y1, -rad, r, g, b, a1, 0, 1);

        // Face 3 (Reverse North-South)
        addVertex(mat, consumer, rad, y0, 0, r, g, b, a0, 0, 0);
        addVertex(mat, consumer, -rad, y0, 0, r, g, b, a0, 1, 0);
        addVertex(mat, consumer, -rad, y1, 0, r, g, b, a1, 1, 1);
        addVertex(mat, consumer, rad, y1, 0, r, g, b, a1, 0, 1);

        // Face 4 (Reverse East-West)
        addVertex(mat, consumer, 0, y0, rad, r, g, b, a0, 0, 0);
        addVertex(mat, consumer, 0, y0, -rad, r, g, b, a0, 1, 0);
        addVertex(mat, consumer, 0, y1, -rad, r, g, b, a1, 1, 1);
        addVertex(mat, consumer, 0, y1, rad, r, g, b, a1, 0, 1);
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
