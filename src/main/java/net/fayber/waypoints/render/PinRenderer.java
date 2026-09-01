package net.fayber.waypoints.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Renders the waypoint label as a Feather-style card: the waypoint name and the distance on a
 * single line ("Name (123m)"), enclosed in a dark rounded-corner card with a subtle lighter
 * border, floating straight above the beam anchor. No separate marker dot.
 *
 * The card is drawn into submit-order buckets (all from COLLECT_SUBMITS, via WaypointRenderer).
 * Vanilla submits every feature into order bucket 0; the translucent feature stage executes
 * buckets in ascending order. Because the see-through card and text do no depth testing at all
 * (that is what makes them render through walls), the only thing that can layer one waypoint's
 * card over another's is draw order. So the buckets are allocated per waypoint, after sorting
 * the visible waypoints far to near (painter's algorithm), by WaypointRenderer:
 *
 *   bucket 0:      beam glow + depth underlays (and every other mod's order-0 translucents)
 *   bucket 2i+1:   card body (fill + border) of the i-th farthest visible waypoint
 *   bucket 2i+2:   that waypoint's card text (submitText)
 *
 * which guarantees, without any depth testing: a nearer card draws over a farther card (2i+3
 * over 2i+1), nearer text over farther text (2i+4 over 2i+2), a nearer card over a farther
 * card's text (2i+3 over 2i+2, so the front waypoint fully occludes the back one), each text
 * on top of its own card (2i+2 over 2i+1), and the beam glow never blends over any card
 * (0 below 2i+1).
 *
 * (In 26.1 the glow-vs-card draw order inside a single bucket was hash-arbitrary; separating
 * the buckets removes that nondeterminism entirely. It also works on 26.2, where per-bucket
 * phase sweeps execute texts before translucent custom geometry.)
 *
 * The card also has a faint depth-writing underlay (renderUnderlay, bucket 0). The see-through
 * card itself writes no depth, so depth-tested translucent world passes drawn later (water,
 * translucent terrain) would blend over the card and make it look transparent. The underlay
 * writes depth at the card's plane so those passes are blocked where the card is visible,
 * while where the card is behind a wall the underlay fails its own depth test and the
 * see-through card still renders through.
 *
 * The card geometry itself is pure vertex-colored quads (blank white texture + vertex colors),
 * so no extra assets are needed. Corner rounding is built from quarter-circle fans, so it is a
 * true rounded rectangle at any size.
 */
public class PinRenderer {
    // Blank fully-opaque white texture: all card color comes from vertex colors.
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("waypoints", "textures/environment/beam.png");

    // Submit-order buckets are allocated per waypoint by WaypointRenderer (see class doc):
    // the i-th farthest visible waypoint gets its card body in bucket 2i+1 and its text in
    // bucket 2i+2. Vanilla uses bucket 0.

    // Card layout, in local units (the same pixel-ish units font glyphs use; the whole card is
    // scaled to world size in applyCardTransform). The card is centered on the local origin:
    // y=-7.5 (top) to y=+7.5 (bottom), and lifted straight up in WORLD space before the
    // billboard rotation so it sits directly above the anchor from every viewing angle (a
    // billboard-space offset would point along the camera's up vector, which from above
    // displaces the card sideways instead of vertically).
    private static final float CARD_TOP = -7.5f;
    private static final float CARD_BOTTOM = 7.5f;
    // World-space lift of the card center above the anchor, multiplied by the scale.
    private static final float CARD_LIFT = 14.5f;
    private static final float CARD_PADDING_H = 4.0f;
    private static final float CARD_PADDING_V = 3.0f;
    private static final float CARD_CORNER_RADIUS = 2.5f;
    private static final int CORNER_SEGMENTS = 5;
    private static final float NAME_DIST_GAP = 4.0f;

    // Card colors: near-black fill with a subtle lighter border (Feather-like). Fully opaque:
    // a see-through card lets the world bleed through and reads as a rendering glitch, and the
    // depth underlay is what keeps the world from showing through where the card overlaps it.
    private static final float FILL_R = 0.070f;
    private static final float FILL_G = 0.078f;
    private static final float FILL_B = 0.090f;
    private static final float FILL_A = 1.0f;
    private static final float BORDER_R = 0.320f;
    private static final float BORDER_G = 0.350f;
    private static final float BORDER_B = 0.390f;
    private static final float BORDER_A = 1.0f;

    // Alpha of the faint depth-writing card underlay (see class doc). Must stay above the text
    // shaders' `if (color.a < 0.1) discard;` threshold or it writes no depth at all; kept as
    // low as possible so it is visually invisible under the real card.
    private static final float DEPTH_UNDERLAY_ALPHA = 0.15f;

    /** Measured layout of one card: body bounds + text positions, in billboard-local units. */
    private record CardLayout(
            float cardLeft, float cardRight,
            float startX, float nameX, float distX,
            String nameText, String distText,
            boolean showLabel, boolean showDistance) {
    }

    /** Bucket 0: submits the faint depth-writing card underlay (see class doc). */
    public static void renderUnderlay(PoseStack poseStack, SubmitNodeCollector collector, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled || !config.alwaysOnTop) {
            return;
        }
        CardLayout layout = layout(wp, dist);
        if (layout == null) {
            return;
        }
        poseStack.pushPose();
        applyCardTransform(poseStack, camera, config, dist);
        collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE_TEXTURE), (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            emitCard(mat, consumer, layout.cardLeft(), layout.cardRight(), DEPTH_UNDERLAY_ALPHA, DEPTH_UNDERLAY_ALPHA);
        });
        poseStack.popPose();
    }

    /**
     * Submits the visible card body (border + fill) as custom geometry into the collector the
     * caller chose for this waypoint (bucket 2i+1, see class doc). Runs after every bucket-0
     * draw, so the beam glow never blends over the card.
     */
    public static void renderCardBody(OrderedSubmitNodeCollector collector, PoseStack poseStack, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled) {
            return;
        }
        CardLayout layout = layout(wp, dist);
        if (layout == null) {
            return;
        }
        // Through-wall variant has no depth state (no test, no write); the depth-tested variant
        // is the opt-out via config.alwaysOnTop=false.
        RenderType cardType = config.alwaysOnTop
                ? RenderTypes.textSeeThrough(WHITE_TEXTURE)
                : RenderTypes.text(WHITE_TEXTURE);

        poseStack.pushPose();
        applyCardTransform(poseStack, camera, config, dist);
        collector.submitCustomGeometry(poseStack, cardType, (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            // Lighter border rounded-rect first, then the near-black fill inset by 1px on top
            // of it, leaving a 1px border ring visible.
            emitCard(mat, consumer, layout.cardLeft(), layout.cardRight(), BORDER_A, FILL_A);
        });
        poseStack.popPose();
    }

    /**
     * Submits the name/distance text into the collector the caller chose for this waypoint
     * (bucket 2i+2, see class doc). Runs after the card body in every version (bucket order on
     * 26.2's phase sweep, buffer flush order on 26.1), so the glyphs always draw on top of the
     * fill, and after every farther waypoint's card and text.
     */
    public static void renderCardText(OrderedSubmitNodeCollector collector, PoseStack poseStack, Camera camera, Waypoint wp, ModConfig config, double dist) {
        if (!config.floatingPinsEnabled) {
            return;
        }
        CardLayout layout = layout(wp, dist);
        if (layout == null) {
            return;
        }
        Font.DisplayMode mode = config.alwaysOnTop
                ? Font.DisplayMode.SEE_THROUGH
                : Font.DisplayMode.NORMAL;

        poseStack.pushPose();
        applyCardTransform(poseStack, camera, config, dist);

        // Text on the card: name in white, distance in dimmer gray, both fully opaque.
        // dropShadow=false: the card provides the contrast, and a shadow ghost reads blurry.
        // 26.x submitText int order is: light, color, backgroundColor, outlineColor.
        float textY = CARD_TOP + CARD_PADDING_V;
        if (layout.showLabel()) {
            collector.submitText(
                    poseStack,
                    layout.startX(), textY,
                    FormattedCharSequence.forward(layout.nameText(), Style.EMPTY),
                    false,
                    mode,
                    0x00F000F0, 0xFFFFFFFF, 0, 0
            );
        }
        if (layout.showDistance()) {
            collector.submitText(
                    poseStack,
                    layout.distX(), textY,
                    FormattedCharSequence.forward(layout.distText(), Style.EMPTY),
                    false,
                    mode,
                    0x00F000F0, 0xFFA8B0BA, 0, 0
            );
        }

        poseStack.popPose();
    }

    /**
     * Shared card transform: scale (with the on-screen min/max clamps), then a straight-up
     * world-space lift of the card center, then the billboard rotation and the vanilla nametag
     * scale recipe (scale(s, -s, s), verified against NameTagFeatureRenderer$Storage in 26.1).
     *
     * <p>The natural on-screen scale factor is pure perspective (base/dist). Two clamps bound it,
     * both expressed as multipliers of the "held" size the card settles at past
     * labelScaleDistance (base/labelScaleDistance), so minScale=1.0 reproduces the old far hold
     * exactly. Max (close): perspective would blow the card up right next to it, so the factor is
     * capped at held*labelMaxScale. Min (far): the factor is floored at held*labelMinScale, so
     * the card keeps shrinking to that floor (minScale below 1) or holds a bigger readable size.
     *
     * <p>The lift grows with the scale, so the card's on-screen offset above the beam stays
     * constant inside a clamped zone too. pinScale and textScale scale the whole card as one
     * factor.
     */
    private static void applyCardTransform(PoseStack poseStack, Camera camera, ModConfig config, double dist) {
        float baseScale = Math.max(0.01f, 0.08f * config.pinScale * config.textScale);
        float holdDist = Math.max(1.0f, config.labelScaleDistance);
        float held = baseScale / holdDist;
        float d = Math.max((float) dist, 1e-3f);
        float lo = held * config.labelMinScale;
        float hi = config.labelMaxScale > 0.0f ? held * config.labelMaxScale : Float.MAX_VALUE;
        float screen = Math.clamp(baseScale / d, Math.min(lo, hi), Math.max(lo, hi));
        float scale = screen * d;
        poseStack.translate(0, CARD_LIFT * scale, 0);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, -scale, scale);
    }

    /** Measures the card body and text positions for one waypoint; null if nothing to draw. */
    private static CardLayout layout(Waypoint wp, double dist) {
        boolean showLabel = wp.isShowLabel();
        boolean showDistance = wp.isShowDistance();
        if (!showLabel && !showDistance) {
            return null;
        }
        Font font = Minecraft.getInstance().font;
        String nameText = wp.getName();
        String distText = "(" + formatDistance(dist) + ")";
        float nameWidth = font.width(nameText);
        float distWidth = font.width(distText);

        float contentW;
        if (showLabel && showDistance) {
            contentW = nameWidth + NAME_DIST_GAP + distWidth;
        } else if (showLabel) {
            contentW = nameWidth;
        } else {
            contentW = distWidth;
        }

        float cardLeft = -contentW / 2.0f - CARD_PADDING_H;
        float cardRight = contentW / 2.0f + CARD_PADDING_H;
        float startX = -contentW / 2.0f;
        float distX = showLabel ? startX + nameWidth + NAME_DIST_GAP : startX;
        return new CardLayout(cardLeft, cardRight, startX, startX, distX, nameText, distText, showLabel, showDistance);
    }

    /**
     * Emits the full card (lighter border rounded-rect first, then the near-black fill inset by
     * 1px, leaving a 1px border ring visible). Shared by the depth-writing underlay and the
     * visible card body so both are always the same shape.
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
