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

/**
 * Renders the waypoint beam as a true round 3D cylinder (a ring of N side-quads), not the
 * vanilla beacon's "two crossed flat planes" illusion (which only looks 3D from a narrow range
 * of angles and is flat/fake-looking from above or from a shallow angle). It also uses our own
 * blank white texture instead of vanilla's animated beacon-beam stripe texture, so the beam's
 * appearance is fully custom (solid per-waypoint color with a smooth fade), not "the Minecraft
 * beacon."
 *
 * The render-state plumbing (RenderTypes.beaconBeam, depth test, blending, no-cull) is kept
 * identical to vanilla's own beacon beam because that render type is proven to behave correctly
 * as real, depth-tested, properly-occluded 3D world geometry - only the mesh shape and texture
 * are custom.
 */
public class BeamRenderer {
    private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("waypoints", "textures/environment/beam.png");

    // Sides on the cylinder ring. Higher = rounder. 20 is smooth without being expensive - this
    // only renders for waypoints that are visible and within render distance.
    private static final int SIDES = 20;

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

        // The pose origin is the centre of the waypoint's block. Base the beam just inside the
        // ground (1 block below the anchor) and fade in over 1 block, so the fade completes
        // underground: at the surface the beam is already fully opaque and reads as standing on
        // the block. The old beam spanned y-128 with a long see-through fade, which made the
        // cylinder's translucent far-wall rim ghost through the floor as a scalloped arch.
        float minY = -1.0f;
        float maxY = config.beaconHeight;

        // Nearly opaque core so the beam occludes itself cleanly (no see-through-its-own-far-wall
        // ghosting where it intersects terrain); translucent outer glow for softness.
        float coreAlpha = Math.min(1.0f, alpha * 1.5f);
        float glowAlpha = alpha * 0.30f;

        collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM_TEXTURE, false), (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            // Slim inner core cylinder.
            renderCylinder(mat, consumer, r, g, b, coreAlpha, radius * 0.35f, minY, maxY);
        });

        collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM_TEXTURE, true), (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            // Wider, softer outer glow cylinder.
            renderCylinder(mat, consumer, r, g, b, glowAlpha, radius, minY, maxY);
        });
    }

    private static void renderCylinder(Matrix4f mat, VertexConsumer consumer, float r, float g, float b, float a, float radius, float minY, float maxY) {
        float span = maxY - minY;
        float bottomFade = 1.0f;
        float topFade = Math.min(8.0f, span / 4.0f);
        float bottomFadeEnd = minY + bottomFade;
        float topFadeStart = maxY - topFade;

        if (topFadeStart <= bottomFadeEnd) {
            // Degenerate/very short beam: fall back to a single top-only fade so we don't emit
            // an inverted or zero-height segment.
            addRing(mat, consumer, r, g, b, radius, minY, a, maxY, 0.0f);
            return;
        }

        // Bottom fade-in (short, tucked inside the ground block)
        addRing(mat, consumer, r, g, b, radius, minY, 0.0f, bottomFadeEnd, a);
        // Solid middle
        addRing(mat, consumer, r, g, b, radius, bottomFadeEnd, a, topFadeStart, a);
        // Top fade-out
        addRing(mat, consumer, r, g, b, radius, topFadeStart, a, maxY, 0.0f);
    }

    /** Emits one ring of SIDES quads (a true cylindrical band) between y0 and y1. */
    private static void addRing(Matrix4f mat, VertexConsumer consumer, float r, float g, float b, float radius, float y0, float a0, float y1, float a1) {
        for (int i = 0; i < SIDES; i++) {
            double theta0 = (Math.PI * 2 * i) / SIDES;
            double theta1 = (Math.PI * 2 * (i + 1)) / SIDES;

            float x0 = (float) (Math.cos(theta0) * radius);
            float z0 = (float) (Math.sin(theta0) * radius);
            float x1 = (float) (Math.cos(theta1) * radius);
            float z1 = (float) (Math.sin(theta1) * radius);

            float u0 = (float) i / SIDES;
            float u1 = (float) (i + 1) / SIDES;

            addVertex(mat, consumer, x0, y0, z0, r, g, b, a0, u0, 0);
            addVertex(mat, consumer, x1, y0, z1, r, g, b, a0, u1, 0);
            addVertex(mat, consumer, x1, y1, z1, r, g, b, a1, u1, 1);
            addVertex(mat, consumer, x0, y1, z0, r, g, b, a1, u0, 1);
        }
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
