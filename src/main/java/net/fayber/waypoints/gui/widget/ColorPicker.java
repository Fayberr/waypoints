package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A full HSV colour picker: a saturation/value square with a vertical hue bar beside it, so a
 * waypoint can be any colour instead of one of a handful of presets.
 *
 * <p><b>Why the square is drawn as gradient columns.</b> The GUI can only fill rectangles and
 * vertical gradients, but the square happens to be exactly bilinear in RGB: for a fixed hue,
 * {@code hsvToRgb(h, s, v)} is linear in {@code v} (it scales the whole colour) and linear in
 * {@code s} at {@code v == 1} (it mixes white towards the pure hue). So one vertical gradient per
 * column, from {@code hsv(h, s, 1)} down to black, reproduces the square exactly. The columns are
 * one <em>physical</em> pixel wide (the matrix is scaled by 1/guiScale, the same trick
 * {@link Ui} uses), so the ramp is smooth on the monitor rather than banded on the GUI grid.
 *
 * <p>Both the square and the hue bar get rounded corners by insetting each column/row by the
 * corner arc, and the gradient end colours are re-evaluated at the clipped fractions so the ramp
 * stays continuous where it is cut.
 *
 * <p>{@link #setDimmed} de-emphasises the whole picker (drawn at a fraction of its brightness)
 * while keeping every interaction alive. It marks "this control is not the thing winning right
 * now" without locking it: picking a colour while dimmed is exactly how the caller ends that
 * state, so the input stays on.
 */
public class ColorPicker extends AbstractWidget {
    /** Width of the hue bar and the gap between it and the square. */
    private static final int HUE_WIDTH = 14;
    private static final int GAP = 10;
    private static final float SQUARE_RADIUS = 5.0f;
    /** Brightness fraction of every colour while {@link #setDimmed de-emphasised}. */
    private static final float DIM = 0.5f;

    private float hue;
    private float saturation;
    private float value;
    private boolean dimmed;

    /** Which sub-control the current drag started on, so leaving it mid-drag still tracks. */
    private enum Drag {
        NONE,
        SQUARE,
        HUE
    }

    private Drag drag = Drag.NONE;
    private Runnable onChange = () -> {};

    public ColorPicker(int x, int y, int width, int height, int argb) {
        super(x, y, width, height, Component.literal("Colour picker"));
        this.setColor(argb);
    }

    public ColorPicker onChange(Runnable onChange) {
        this.onChange = onChange;
        return this;
    }

    /**
     * De-emphasises the picker: everything is drawn at {@link #DIM} of its brightness, but the
     * picker stays interactive, so clicking it still picks a colour (which is how the caller
     * ends the dimmed state).
     */
    public ColorPicker setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        return this;
    }

    // ------------------------------------------------------------------ model

    /** The picked colour, always fully opaque. */
    public int getColor() {
        return 0xFF000000 | (Mth.hsvToRgb(this.hue, this.saturation, this.value) & 0x00FFFFFF);
    }

    /**
     * Moves the picker to {@code argb}. Hue is only overwritten when the colour actually has one:
     * a grey (saturation 0) or black (value 0) carries no hue, and resetting it to 0 there would
     * make the hue bar jump to red whenever the user dragged into a corner of the square.
     */
    public void setColor(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        this.value = max;
        this.saturation = max <= 0.0f ? 0.0f : delta / max;
        if (delta > 0.0f) {
            float h;
            if (max == r) {
                h = (g - b) / delta;
            } else if (max == g) {
                h = 2.0f + (b - r) / delta;
            } else {
                h = 4.0f + (r - g) / delta;
            }
            h /= 6.0f;
            this.hue = h < 0.0f ? h + 1.0f : h;
        }
    }

    // ------------------------------------------------------------------ layout

    private int squareSize() {
        return Math.min(this.getHeight(), this.getWidth() - HUE_WIDTH - GAP);
    }

    private int hueX() {
        return this.getX() + this.squareSize() + GAP;
    }

    // ------------------------------------------------------------------ input

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        int square = this.squareSize();
        if (event.x() < this.getX() + square + GAP / 2.0) {
            this.drag = Drag.SQUARE;
        } else {
            this.drag = Drag.HUE;
        }
        this.apply(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        this.apply(event.x(), event.y());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.drag = Drag.NONE;
    }

    private void apply(double mouseX, double mouseY) {
        int square = this.squareSize();
        if (this.drag == Drag.SQUARE) {
            this.saturation = (float) Math.clamp((mouseX - this.getX()) / square, 0.0, 1.0);
            this.value = 1.0f - (float) Math.clamp((mouseY - this.getY()) / square, 0.0, 1.0);
        } else if (this.drag == Drag.HUE) {
            this.hue = (float) Math.clamp((mouseY - this.getY()) / this.getHeight(), 0.0, 0.9999);
        } else {
            return;
        }
        this.onChange.run();
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int square = this.squareSize();
        float s = Ui.scale();

        gfx.pose().pushMatrix();
        gfx.pose().scale(1.0f / s, 1.0f / s);
        this.drawSquare(gfx, s, square);
        this.drawHueBar(gfx, s);
        gfx.pose().popMatrix();

        this.drawHandles(gfx, square);
    }

    /** Saturation/value square: one vertical gradient per physical pixel column. */
    private void drawSquare(GuiGraphicsExtractor gfx, float s, int square) {
        int x0 = Math.round(this.getX() * s);
        int y0 = Math.round(this.getY() * s);
        int size = Math.round(square * s);
        int radius = Math.round(SQUARE_RADIUS * s);
        if (size <= 0) {
            return;
        }

        for (int col = 0; col < size; col++) {
            float sat = (col + 0.5f) / size;
            int top = 0xFF000000 | (Mth.hsvToRgb(this.hue, sat, 1.0f) & 0x00FFFFFF);
            if (this.dimmed) {
                top = scaleRgb(top, DIM);
            }
            int inset = cornerInset(col + 0.5f, size - col - 0.5f, radius);
            if (2 * inset >= size) {
                continue;
            }
            // Value falls linearly from 1 at the top to 0 at the bottom, so clipping the column
            // for the corner arc just means starting/ending the ramp part-way through.
            int colTop = scaleRgb(top, 1.0f - (float) inset / size);
            int colBottom = scaleRgb(top, (float) inset / size);
            gfx.fillGradient(x0 + col, y0 + inset, x0 + col + 1, y0 + size - inset, colTop, colBottom);
        }
    }

    /** Hue bar: one flat fill per physical pixel row, capsule-capped at both ends. */
    private void drawHueBar(GuiGraphicsExtractor gfx, float s) {
        int x0 = Math.round(this.hueX() * s);
        int y0 = Math.round(this.getY() * s);
        int w = Math.round(HUE_WIDTH * s);
        int h = Math.round(this.getHeight() * s);
        if (w <= 0 || h <= 0) {
            return;
        }
        int radius = w / 2;

        for (int row = 0; row < h; row++) {
            int color = 0xFF000000 | (Mth.hsvToRgb((row + 0.5f) / h, 1.0f, 1.0f) & 0x00FFFFFF);
            if (this.dimmed) {
                color = scaleRgb(color, DIM);
            }
            int inset = cornerInset(row + 0.5f, h - row - 0.5f, radius);
            if (2 * inset >= w) {
                continue;
            }
            gfx.fill(x0 + inset, y0 + row, x0 + w - inset, y0 + row + 1, color);
        }
    }

    private void drawHandles(GuiGraphicsExtractor gfx, int square) {
        float dim = this.dimmed ? DIM : 1.0f;
        // Square handle: filled discs, a dark rim inside a white ring, so it reads over both the
        // white and the black corners of the square. The centre dot is the colour under the
        // handle, so the cursor doubles as a preview of what you are pointing at.
        float hx = this.getX() + this.saturation * square;
        float hy = this.getY() + (1.0f - this.value) * square;
        int under = scaleRgb(0xFF000000 | (Mth.hsvToRgb(this.hue, this.saturation, this.value) & 0x00FFFFFF), dim);
        Ui.circle(gfx, hx, hy, 5.0f, 0x99000000);
        Ui.circle(gfx, hx, hy, 4.0f, scaleRgb(0xFFFFFFFF, dim));
        Ui.circle(gfx, hx, hy, 2.5f, under);

        // Hue handle: a slim bar straddling the whole width of the bar. It is built from two filled
        // capsules, the inner one painted in the hue it sits on, because a transparent middle
        // cannot be punched out of a filled shape.
        float by = this.getY() + this.hue * this.getHeight();
        float bx = this.hueX() - 2.0f;
        float bw = HUE_WIDTH + 4.0f;
        int hueColor = scaleRgb(0xFF000000 | (Mth.hsvToRgb(this.hue, 1.0f, 1.0f) & 0x00FFFFFF), dim);
        Ui.pill(gfx, bx, by - 3.5f, bw, 7.0f, scaleRgb(0xFFFFFFFF, dim));
        Ui.pill(gfx, bx + 1.5f, by - 2.0f, bw - 3.0f, 4.0f, hueColor);
    }

    /**
     * How far a column/row that sits {@code near}/{@code far} pixels from the two ends has to be
     * pulled in so the shape's corners follow an arc of {@code radius}.
     */
    private static int cornerInset(float near, float far, int radius) {
        float d = Math.min(near, far);
        if (d >= radius || radius <= 0) {
            return 0;
        }
        float dx = radius - d;
        return Math.round(radius - (float) Math.sqrt(Math.max(0.0f, radius * radius - dx * dx)));
    }

    /** Multiplies the RGB channels by {@code f}, which is exactly what lowering HSV value does. */
    private static int scaleRgb(int argb, float f) {
        int r = Math.round(((argb >> 16) & 0xFF) * f);
        int g = Math.round(((argb >> 8) & 0xFF) * f);
        int b = Math.round((argb & 0xFF) * f);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}
