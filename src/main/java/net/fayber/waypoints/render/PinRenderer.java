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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class PinRenderer {
    // The crosshair sprite is almost entirely transparent (a thin plus glyph), so mapping it
    // across a diamond marker made the pin nearly invisible. The map "target point" icon is a
    // small filled marker dot (the same one vanilla uses for lodestone tracking), which reads
    // as a clean, solid geometric marker once tinted with the waypoint color.
    private static final Identifier PIN_TEXTURE = Identifier.withDefaultNamespace("textures/map/decorations/target_point.png");

    public static void renderPin(PoseStack poseStack, SubmitNodeCollector collector, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled) {
            return;
        }

        poseStack.pushPose();

        // Billboard rotation towards camera
        poseStack.mulPose(camera.rotation());

        // Distance-compensated scale: grow proportionally with distance (with only a small floor
        // for extreme close-ups) so the pin keeps a roughly constant apparent size on screen.
        // The previous version capped growth at ~20 blocks, so anything farther away (i.e. most
        // waypoints you'd actually need a marker for) shrank towards invisibility on screen.
        float scale = (float) Math.max(0.05f, dist * 0.012f * config.pinScale);
        poseStack.scale(scale, -scale, scale);

        WaypointColor color = WaypointColor.of(wp.getEffectiveColor());
        int argb = color.getEffectiveArgb();
        float r = color.getRed();
        float g = color.getGreen();
        float b = color.getBlue();

        // 1. Draw 3D Diamond / Pin Icon
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(PIN_TEXTURE, false), (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            renderDiamondMarker(mat, consumer, r, g, b, 0.9f);
        });

        // 2. Draw Floating Name Tag & Distance
        Font font = Minecraft.getInstance().font;
        String nameText = wp.getName();
        String distText = formatDistance(dist);

        FormattedCharSequence nameSeq = FormattedCharSequence.forward(nameText, net.minecraft.network.chat.Style.EMPTY);
        FormattedCharSequence distSeq = FormattedCharSequence.forward(distText, net.minecraft.network.chat.Style.EMPTY.withColor(0xAAAAAA));

        float nameWidth = font.width(nameText);
        float distWidth = font.width(distText);

        // Name tag slightly above the pin
        collector.submitText(
                poseStack,
                -nameWidth / 2.0f,
                -22.0f,
                nameSeq,
                true,
                Font.DisplayMode.NORMAL,
                argb,
                0x70000000,
                0x00F000F0,
                0
        );

        // Distance tag
        collector.submitText(
                poseStack,
                -distWidth / 2.0f,
                -12.0f,
                distSeq,
                true,
                Font.DisplayMode.NORMAL,
                0xFFDDDDDD,
                0x70000000,
                0x00F000F0,
                0
        );

        poseStack.popPose();
    }

    private static void renderDiamondMarker(Matrix4f mat, VertexConsumer consumer, float r, float g, float b, float a) {
        float size = 4.0f;
        // Diamond 4 vertices
        consumer.addVertex(mat, 0, -size, 0).setColor(r, g, b, a).setUv(0.5f, 0.0f).setOverlay(0).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(mat, -size, 0, 0).setColor(r, g, b, a).setUv(0.0f, 0.5f).setOverlay(0).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(mat, 0, size, 0).setColor(r, g, b, a).setUv(0.5f, 1.0f).setOverlay(0).setLight(0x00F000F0).setNormal(0, 0, 1);
        consumer.addVertex(mat, size, 0, 0).setColor(r, g, b, a).setUv(1.0f, 0.5f).setOverlay(0).setLight(0x00F000F0).setNormal(0, 0, 1);
    }

    private static String formatDistance(double dist) {
        if (dist >= 1000) {
            return String.format("%.1fkm", dist / 1000.0);
        }
        return String.format("%dm", (int) Math.round(dist));
    }
}
