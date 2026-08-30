package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Renders the waypoint label as a Feather-style card: the waypoint name and the distance on a
 * single line ("Name (123m)"), enclosed in a dark rounded-corner card with a subtle lighter
 * border, floating above the beam anchor. No separate marker dot.
 *
 * The card is pure vertex-colored geometry (blank white texture + vertex colors), so no extra
 * assets are needed. Corner rounding is built from quarter-circle fans, so it is a true rounded
 * rectangle at any size.
 */
public class PinRenderer {
    // Blank fully-opaque white texture: all card color comes from vertex colors.
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("waypoints", "textures/environment/beam.png");

    // Card layout, in local units (the same pixel-ish units font glyphs use; the whole pin is
    // scaled to world size below). Card floats above the anchor: y=-22 (top) to y=-7 (bottom).
    private static final float CARD_TOP = -22.0f;
    private static final float CARD_BOTTOM = -7.0f;
    private static final float CARD_PADDING_H = 4.0f;
    private static final float CARD_PADDING_V = 3.0f;
    private static final float CARD_CORNER_RADIUS = 2.5f;
    private static final int CORNER_SEGMENTS = 5;
    private static final float NAME_DIST_GAP = 4.0f;

    // Card colors: near-black fill with a subtle lighter border (Feather-like).
    private static final float FILL_R = 0.070f;
    private static final float FILL_G = 0.078f;
    private static final float FILL_B = 0.090f;
    private static final float FILL_A = 0.88f;
    private static final float BORDER_R = 0.320f;
    private static final float BORDER_G = 0.350f;
    private static final float BORDER_B = 0.390f;
    private static final float BORDER_A = 0.90f;

    // Alpha of the faint depth-writing card underlay (see renderPin). Must stay above the text
    // shaders' `if (color.a < 0.1) discard;` threshold or it writes no depth at all; kept as
    // low as possible so it is visually invisible under the real card.
    private static final float DEPTH_UNDERLAY_ALPHA = 0.15f;

    public static void renderPin(PoseStack poseStack, SubmitNodeCollector collector, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled) {
            return;
        }
        boolean showLabel = wp.isShowLabel();
        boolean showDistance = wp.isShowDistance();
        if (!showLabel && !showDistance) {
            return;
        }

        poseStack.pushPose();

        // Billboard rotation towards camera, then the vanilla nametag scale recipe
        // (scale(s, -s, s), verified against NameTagFeatureRenderer$Storage in 26.1).
        //
        // Fixed world-space size, with a far clamp: past labelScaleDistance blocks the world
        // size grows proportionally with distance, so perspective stops shrinking the card and
        // it holds a readable on-screen size instead of collapsing to a pixel (Feather-style).
        // The card's world offset above the anchor grows by the same factor, so its on-screen
        // offset above the beam holds constant too. Close by, normal perspective applies.
        //
        // pinScale and textScale are folded into one factor now that there is no separate dot:
        // both config knobs still work, they just scale the whole label together.
        float baseScale = Math.max(0.01f, 0.08f * config.pinScale * config.textScale);
        float farHold = Math.max(1.0f, (float) dist / Math.max(1.0f, config.labelScaleDistance));
        float scale = baseScale * farHold;
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, -scale, scale);

        Font font = Minecraft.getInstance().font;
        Font.DisplayMode mode = config.alwaysOnTop
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.NORMAL;

        // Through-wall variant has no depth state (no test, no write); the depth-tested variant
        // is the opt-out via config.alwaysOnTop=false.
        RenderType cardType = config.alwaysOnTop
                ? RenderTypes.textSeeThrough(WHITE_TEXTURE)
                : RenderTypes.text(WHITE_TEXTURE);

        String nameText = wp.getName();
        String distText = "(" + formatDistance(dist) + ")";
        float nameWidth = font.width(nameText);
        float distWidth = font.width(distText);

        float contentW = 0.0f;
        if (showLabel && showDistance) {
            contentW = nameWidth + NAME_DIST_GAP + distWidth;
        } else if (showLabel) {
            contentW = nameWidth;
        } else {
            contentW = distWidth;
        }

        float cardLeft = -contentW / 2.0f - CARD_PADDING_H;
        float cardRight = contentW / 2.0f + CARD_PADDING_H;
        float contentTop = CARD_TOP + CARD_PADDING_V;
        float startX = -contentW / 2.0f;

        // 1. Card. First a faint depth-writing underlay through the depth-tested text pipeline:
        // the see-through pipeline writes no depth, so translucent world geometry rendered
        // later (water/rivers, spider eyes, clouds) blended OVER the card and it looked
        // transparent. The underlay (alpha just above the text shaders' 0.1 discard threshold)
        // writes depth at the card's distance, so those translucents are correctly blocked
        // where the card is visible, while where the card is behind a wall the underlay fails
        // its own depth test and the see-through copy below still renders through it.
        if (config.alwaysOnTop) {
            collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE_TEXTURE), (pose, consumer) -> {
                Matrix4f mat = pose.pose();
                emitCard(mat, consumer, cardLeft, cardRight, DEPTH_UNDERLAY_ALPHA, DEPTH_UNDERLAY_ALPHA);
            });
        }

        // Lighter border rounded-rect first, then the near-black fill inset by 1px on top of
        // it, leaving a 1px border ring visible. With SEE_THROUGH (no depth test) the
        // later-drawn fill wins where they overlap.
        collector.submitCustomGeometry(poseStack, cardType, (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            emitCard(mat, consumer, cardLeft, cardRight, BORDER_A, FILL_A);
        });

        // 2. Text on the card: name in white, distance in dimmer gray, both fully opaque.
        // 26.x submitText int order is (lightCoords, color, background, outline) - light comes
        // BEFORE color (verified against SubmitNodeStorage$TextSubmit's record fields). The
        // 1.21-era order silently fed the background into color: fully transparent text.
        // dropShadow=false: the card provides the contrast, and a shadow ghost reads blurry.
        float textY = contentTop;
        if (showLabel) {
            FormattedCharSequence nameSeq = FormattedCharSequence.forward(nameText, net.minecraft.network.chat.Style.EMPTY);
            collector.submitText(
                    poseStack,
                    startX,
                    textY,
                    nameSeq,
                    false,
                    mode,
                    0x00F000F0,
                    0xFFFFFFFF,
                    0x00000000,
                    0
            );
        }
        if (showDistance) {
            float distX = showLabel ? startX + nameWidth + NAME_DIST_GAP : startX;
            FormattedCharSequence distSeq = FormattedCharSequence.forward(distText, net.minecraft.network.chat.Style.EMPTY);
            collector.submitText(
                    poseStack,
                    distX,
                    textY,
                    distSeq,
                    false,
                    mode,
                    0x00F000F0,
                    0xFFA8B0BA,
                    0x00000000,
                    0
            );
        }

        poseStack.popPose();
    }

    /**
     * Emits the full card (lighter border rounded-rect first, then the near-black fill inset by
     * 1px, leaving a 1px border ring visible). Shared by the depth-writing underlay and the
     * visible card submission so both are always the same shape.
     */
    private static void emitCard(Matrix4f mat, VertexConsumer consumer, float cardLeft, float cardRight, float borderA, float fillA) {
        roundedRect(mat, consumer,
                cardLeft - 1.0f, CARD_TOP - 1.0f, cardRight + 1.0f, CARD_BOTTOM + 1.0f,
                CARD_CORNER_RADIUS + 1.0f,
                BORDER_R, BORDER_G, BORDER_B, borderA);
        roundedRect(mat, consumer,
                cardLeft, CARD_TOP, cardRight, CARD_BOTTOM,
                CARD_CORNER_RADIUS,
                FILL_R, FILL_G, FILL_B, fillA);
    }

    /**
     * Emits a rounded rectangle (center band + top/bottom bands + four quarter-circle corner
     * fans) in the billboard plane. All winding faces the camera, matching the vanilla glyph
     * convention: through the scale(s, -s, s) billboard mirror, quads must be emitted
     * (left,top) -> (left,bottom) -> (right,bottom) -> (right,top) and fans
     * (center, next, current) with the angle increasing, or the text pipelines' backface
     * culling discards them.
     */
    private static void roundedRect(Matrix4f mat, VertexConsumer consumer, float x0, float y0, float x1, float y1, float radius, float r, float g, float b, float a) {
        float rClamped = Math.min(radius, Math.min(x1 - x0, y1 - y0) / 2.0f);

        // Middle band (full width, inset vertically)
        addQuad(mat, consumer, x0, y0 + rClamped, x1, y1 - rClamped, r, g, b, a);
        // Top band (inset horizontally)
        addQuad(mat, consumer, x0 + rClamped, y0, x1 - rClamped, y0 + rClamped, r, g, b, a);
        // Bottom band (inset horizontally)
        addQuad(mat, consumer, x0 + rClamped, y1 - rClamped, x1 - rClamped, y1, r, g, b, a);

        // Corner quarter-fans. Each spans PI/2 of arc; (cx, cy, thetaStart) chosen so the arc
        // bulges outward at the corner. Angles increase (the fan winding depends on it).
        cornerFan(mat, consumer, x0 + rClamped, y0 + rClamped, (float) Math.PI, r, g, b, a, rClamped);
        cornerFan(mat, consumer, x1 - rClamped, y0 + rClamped, (float) (Math.PI * 1.5), r, g, b, a, rClamped);
        cornerFan(mat, consumer, x1 - rClamped, y1 - rClamped, 0.0f, r, g, b, a, rClamped);
        cornerFan(mat, consumer, x0 + rClamped, y1 - rClamped, (float) (Math.PI * 0.5), r, g, b, a, rClamped);
    }

    /** One quarter-circle fan centered at (cx, cy), starting at thetaStart and sweeping +PI/2. */
    private static void cornerFan(Matrix4f mat, VertexConsumer consumer, float cx, float cy, float thetaStart, float r, float g, float b, float a, float radius) {
        float prevX = cx + (float) Math.cos(thetaStart) * radius;
        float prevY = cy + (float) Math.sin(thetaStart) * radius;
        for (int i = 1; i <= CORNER_SEGMENTS; i++) {
            float theta = thetaStart + (float) (Math.PI / 2) * i / CORNER_SEGMENTS;
            float px = cx + (float) Math.cos(theta) * radius;
            float py = cy + (float) Math.sin(theta) * radius;
            addVertex(mat, consumer, cx, cy, r, g, b, a);
            addVertex(mat, consumer, px, py, r, g, b, a);
            addVertex(mat, consumer, prevX, prevY, r, g, b, a);
            addVertex(mat, consumer, prevX, prevY, r, g, b, a);
            prevX = px;
            prevY = py;
        }
    }

    /**
     * Emits one axis-aligned quad (degenerate quad pair) in the billboard plane, wound to face
     * the camera through the billboard mirror: (x0,y0) -> (x0,y1) -> (x1,y1) -> (x1,y0).
     */
    private static void addQuad(Matrix4f mat, VertexConsumer consumer, float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        addVertex(mat, consumer, x0, y0, r, g, b, a);
        addVertex(mat, consumer, x0, y1, r, g, b, a);
        addVertex(mat, consumer, x1, y1, r, g, b, a);
        addVertex(mat, consumer, x1, y0, r, g, b, a);
    }

    private static void addVertex(Matrix4f mat, VertexConsumer consumer, float x, float y, float r, float g, float b, float a) {
        consumer.addVertex(mat, x, y, 0)
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
