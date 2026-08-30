package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class PinRenderer {
    // Blank fully-opaque white texture: all marker color comes from vertex colors, so the
    // marker always matches the waypoint color exactly. (The previous texture was vanilla's
    // red map "target point" decoration sprite, which painted the marker red no matter what
    // color the vertex data asked for.)
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("waypoints", "textures/environment/beam.png");

    // Marker geometry, in local units at the pin's world scale (0.08 world units per unit).
    private static final float DOT_RADIUS = 1.3f;
    private static final float DOT_RIM = 1.8f;
    private static final int DISC_SEGMENTS = 12;

    public static void renderPin(PoseStack poseStack, SubmitNodeCollector collector, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled) {
            return;
        }

        poseStack.pushPose();

        // Billboard rotation towards camera
        poseStack.mulPose(camera.rotation());

        // Fixed world-space scale (no distance multiplier). This is the actual root cause of the
        // "not really 3D, feels stuck to my screen" complaint: the previous "distance-compensated"
        // scale grew the pin's world size in direct proportion to distance, which cancels out
        // perspective foreshortening (screen size = world size / distance) and makes the pin hold
        // a near-constant apparent size on screen regardless of how far away it really is - i.e.
        // it behaves like a flat HUD compass marker glued to the viewport instead of a real object
        // sitting out in the world. A fixed world-space size lets normal perspective projection
        // shrink it naturally with distance, exactly like vanilla entity nametags.
        float scale = Math.max(0.01f, 0.08f * config.pinScale);
        poseStack.scale(scale, -scale, scale);

        WaypointColor color = WaypointColor.of(wp.getEffectiveColor());
        float r = color.getRed();
        float g = color.getGreen();
        float b = color.getBlue();

        // 1. Marker dot: dark backing disc for contrast plus a colored core, at the anchor point.
        // textSeeThrough has no depth state (no test, no write), so the marker stays visible
        // through walls like modern client mods' markers; config.alwaysOnTop=false opts out into
        // the depth-tested variant.
        RenderType dotType = config.alwaysOnTop
                ? RenderTypes.textSeeThrough(WHITE_TEXTURE)
                : RenderTypes.text(WHITE_TEXTURE);
        collector.submitCustomGeometry(poseStack, dotType, (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            renderMarkerDot(mat, consumer, r, g, b);
        });

        // 2. Floating name & distance above the dot. White/gray text with a soft shadow and no
        // background box reads modern; the waypoint color is carried by the beam and dot.
        if (wp.isShowLabel() || wp.isShowDistance()) {
            Font font = Minecraft.getInstance().font;
            Font.DisplayMode mode = config.alwaysOnTop
                    ? Font.DisplayMode.SEE_THROUGH
                    : Font.DisplayMode.NORMAL;

            poseStack.pushPose();
            poseStack.scale(config.textScale, config.textScale, config.textScale);

            if (wp.isShowLabel()) {
                String nameText = wp.getName();
                FormattedCharSequence nameSeq = FormattedCharSequence.forward(nameText, net.minecraft.network.chat.Style.EMPTY);
                float nameWidth = font.width(nameText);
                // 26.x submitText int order is (lightCoords, color, background, outline) - light
                // comes BEFORE color (verified against SubmitNodeStorage$TextSubmit's record
                // fields). The 1.21-era order (color, background, light, overlay) silently fed
                // 0xFFFFFFFF into lightCoords and 0x00000000 into color: fully transparent text.
                collector.submitText(
                        poseStack,
                        -nameWidth / 2.0f,
                        -16.0f,
                        nameSeq,
                        true,
                        mode,
                        0x00F000F0,
                        0xFFFFFFFF,
                        0x00000000,
                        0
                );
            }

            if (wp.isShowDistance()) {
                String distText = formatDistance(dist);
                FormattedCharSequence distSeq = FormattedCharSequence.forward(distText, net.minecraft.network.chat.Style.EMPTY);
                float distWidth = font.width(distText);
                collector.submitText(
                        poseStack,
                        -distWidth / 2.0f,
                        -5.0f,
                        distSeq,
                        true,
                        mode,
                        0x00F000F0,
                        0xFFB8C4CC,
                        0x00000000,
                        0
                );
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /** Draws the marker dot: a dark backing disc under a smaller colored disc, both facing the camera. */
    private static void renderMarkerDot(Matrix4f mat, VertexConsumer consumer, float r, float g, float b) {
        // Dark rim disc first (slightly larger, under the core via draw order), then the colored core.
        filledDisc(mat, consumer, DOT_RIM, 0.0f, 0.0f, 0.0f, 0.70f);
        filledDisc(mat, consumer, DOT_RADIUS, r, g, b, 1.0f);
    }

    /**
     * Emits a filled N-gon disc centered on the origin in the billboard plane (degenerate quads).
     *
     * Vertex order is deliberately reversed (clockwise when viewed from +Z) so the disc's front
     * face is -Z before the billboard mirror. Vanilla glyph quads face -Z here too, and the
     * scale(s, -s, s) flip (the same recipe vanilla nametags use, verified in the 26.1
     * NameTagFeatureRenderer$Storage disassembly) then turns them to face the camera. Wound the
     * other way, the text pipelines - which cull back faces - discard the entire disc.
     */
    private static void filledDisc(Matrix4f mat, VertexConsumer consumer, float radius, float r, float g, float b, float a) {
        for (int i = 0; i < DISC_SEGMENTS; i++) {
            double a0 = (Math.PI * 2 * i) / DISC_SEGMENTS;
            double a1 = (Math.PI * 2 * (i + 1)) / DISC_SEGMENTS;
            float x0 = (float) Math.cos(a0) * radius;
            float y0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;

            addVertex(mat, consumer, 0, 0, 0, r, g, b, a);
            addVertex(mat, consumer, x1, y1, 0, r, g, b, a);
            addVertex(mat, consumer, x0, y0, 0, r, g, b, a);
            addVertex(mat, consumer, x0, y0, 0, r, g, b, a);
        }
    }

    private static void addVertex(Matrix4f mat, VertexConsumer consumer, float x, float y, float z, float r, float g, float b, float a) {
        consumer.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(0)
                .setLight(0x00F000F0)
                .setNormal(0, 0, 1);
    }

    private static String formatDistance(double dist) {
        if (dist >= 1000) {
            return String.format("%.1fkm", dist / 1000.0);
        }
        return String.format("%dm", (int) Math.round(dist));
    }
}
