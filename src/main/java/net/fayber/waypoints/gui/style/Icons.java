package net.fayber.waypoints.gui.style;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * The UI glyphs, as sprites from a pre-rendered atlas ({@code textures/gui/icons.png}, generated
 * by {@code tools/gen-icons.java} from the Lucide icon set, ISC license, see NOTICE).
 *
 * <p>This replaced drawing glyphs from Ui primitives each frame, which alias badly: strokes are
 * re-rasterised at whatever odd size a button happens to be, so curves come out lumpy and round
 * joints develop slivers. The atlas instead holds each icon rasterised once at high resolution
 * with real anti-aliasing, and drawing it is a bilinear-filtered tinted quad, which stays smooth
 * at every GUI scale and every button size, the way a UI toolkit does it.
 *
 * <p>The atlas texture registers itself with a {@code LINEAR} sampler: {@link DynamicTexture}
 * hard-codes the blocky {@code NEAREST} one, and in this rendering stack filtering is a property
 * of the sampler, not the texture, so the subclass below swaps it after construction.
 */
public final class Icons {
    /** Atlas cell, 3x the 24-unit Lucide grid; must match tools/gen-icons.java. */
    private static final int CELL = 72;
    private static final int COLS = 4;
    private static final int ATLAS_W = CELL * COLS;
    private static final int ATLAS_H = CELL * 2;
    /** Where the PNG lives in resources. */
    private static final Identifier ATLAS_ASSET =
            Identifier.fromNamespaceAndPath("waypoints", "textures/gui/icons.png");
    /** The registered texture name used when blitting. */
    private static final Identifier ATLAS_TEXTURE =
            Identifier.fromNamespaceAndPath("waypoints", "gui/icons");

    /** Plus sign, for "new waypoint". */
    public static final Glyph PLUS = glyph(0);
    /** Magnifier, for the search field. */
    public static final Glyph SEARCH = glyph(1);
    /** Open eye (waypoint shown). */
    public static final Glyph EYE = glyph(2);
    /** Crossed-out eye (waypoint hidden). */
    public static final Glyph EYE_OFF = glyph(3);
    /** Waste bin, for "delete waypoint". */
    public static final Glyph TRASH = glyph(4);
    /** Gear, for "settings". */
    public static final Glyph GEAR = glyph(5);
    /** Paper-plane style navigation arrow, for "teleport here". */
    public static final Glyph TELEPORT = glyph(6);
    /** Solid map pin, used as the colour swatch on a waypoint card. */
    public static final Glyph PIN = glyph(7);

    private static boolean registered;

    private Icons() {
    }

    private static Glyph glyph(int index) {
        return (gfx, cx, cy, size, color) -> draw(gfx, index, cx, cy, size, color);
    }

    private static void draw(GuiGraphicsExtractor gfx, int index, float cx, float cy, float size, int color) {
        ensureRegistered();
        float s = Ui.scale();
        int px = Math.max(1, Math.round(size * s));
        int x0 = Math.round(cx * s) - px / 2;
        int y0 = Math.round(cy * s) - px / 2;
        int u = (index % COLS) * CELL;
        int v = (index / COLS) * CELL;
        gfx.pose().pushMatrix();
        gfx.pose().scale(1.0f / s, 1.0f / s);
        // The long blit overload separates destination size from source texels: the full CELLxCELL
        // cell is sampled into a px-px quad. (The short overload samples w x h texels, which would
        // magnify one corner of the cell.)
        gfx.blit(RenderPipelines.GUI_TEXTURED, ATLAS_TEXTURE, x0, y0, u, v,
                px, px, CELL, CELL, ATLAS_W, ATLAS_H, color);
        gfx.pose().popMatrix();
    }

    /** Loads and registers the atlas the first time an icon is drawn (always on the render thread). */
    private static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(ATLAS_ASSET)
                    .orElseThrow(() -> new IllegalStateException("missing " + ATLAS_ASSET));
            NativeImage image;
            try (InputStream in = resource.open()) {
                image = NativeImage.read(in);
            }
            Minecraft.getInstance().getTextureManager().register(ATLAS_TEXTURE, new LinearTexture(image));
        } catch (Exception e) {
            LoggerFactory.getLogger("waypoints").error("Failed to load the icon atlas", e);
        }
    }

    /** {@link DynamicTexture} always installs a NEAREST sampler; icons need bilinear. */
    private static final class LinearTexture extends DynamicTexture {
        private LinearTexture(NativeImage image) {
            super(() -> "waypoints/icons", image);
            this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        }
    }
}
