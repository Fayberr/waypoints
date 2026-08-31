package net.fayber.waypoints.gui;

import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.util.Util;

import java.util.List;

/**
 * The scrolling body of {@link WaypointScreen}: one row per waypoint, each row a strip of separate
 * cards (show/hide, the waypoint itself, teleport, delete) rather than one big container card.
 *
 * <p>All the vanilla list chrome is switched off (background, row separators, sprite scrollbar) and
 * replaced with a slim rounded scrollbar, so the rows float directly on the dimmed world.
 *
 * <p>Mouse-wheel scrolling has momentum, because vanilla's is instant: the wheel adds velocity and
 * the position coasts with an exponential decay, the way a website eases a wheel step to rest.
 * One notch travels {@code scrollRate()} pixels in total, but spread over a fraction of a second
 * of deceleration instead of snapping there; fast spins accumulate velocity (capped). The decay is
 * time-normalised, so the feel is identical at any frame rate. Scrollbar drags and keyboard
 * navigation cancel the glide and stay 1:1 with the pointer.
 *
 * <p>Vanilla positions rows at {@code firstEntryY - (int) scrollAmount} and derives the thumb from
 * an integer division: both drop the fraction of the scroll amount, which turns any fractional
 * scroll into whole-GUI-pixel stair-steps. Rows are therefore drawn with the pose shifted back by
 * that dropped fraction, and the thumb is drawn from the continuous formula.
 */
public class WaypointList extends ContainerObjectSelectionList<WaypointList.Row> {
    public static final int CARD_HEIGHT = 32;
    public static final int ROW_GAP = 5;
    public static final int ROW_HEIGHT = CARD_HEIGHT + ROW_GAP;

    /** Exponential velocity decay of the wheel glide (per second); a notch coasts ~0.4s. */
    private static final double SCROLL_FRICTION = 10.0;
    /** Glide speed below which the coast has visibly ended and stops. */
    private static final double SCROLL_STOP = 6.0;
    /** Velocity cap so a fast spin does not launch the list off-screen. */
    private static final double SCROLL_MAX_SPEED = 4000.0;
    private static final double MAX_FRAME_SECONDS = 0.1;

    private final int rowWidth;

    /** Current glide velocity in GUI px/s; zero when the list is at rest. */
    private double glideVelocity;
    private long lastFrameMs = -1L;

    public WaypointList(Minecraft mc, int width, int height, int y0, int rowWidth, List<Row> rows) {
        super(mc, width, height, y0, ROW_HEIGHT);
        this.rowWidth = rowWidth;
        for (Row row : rows) {
            this.addEntry(row);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(this.rowWidth, this.getWidth() - 24);
    }

    /**
     * Swaps the whole set of rows in place. The list widget itself survives, which is what lets the
     * search field keep keyboard focus while it filters (rebuilding the screen's widgets on every
     * keystroke would drop it after each character).
     */
    public void setRows(List<Row> rows) {
        this.clearEntries();
        for (Row row : rows) {
            this.addEntry(row);
        }
        this.setScrollAmount(0.0);
    }

    /**
     * Wheel input adds glide velocity; the position coasts in {@link #advanceGlide}. One notch
     * travels {@code scrollRate()} pixels in total because v0 = distance * friction for an
     * exponential decay.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        if (!this.scrollable()) {
            return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
        }
        this.glideVelocity = Math.clamp(
                this.glideVelocity - yDelta * this.scrollRate() * SCROLL_FRICTION,
                -SCROLL_MAX_SPEED, SCROLL_MAX_SPEED);
        return true;
    }

    /**
     * GUI pixels per wheel notch. Vanilla's own rate is {@code entryHeight / 2} (half a row),
     * which reads as sluggish here; two rows per notch matches the config screen.
     */
    @Override
    protected double scrollRate() {
        return 2.0 * ROW_HEIGHT;
    }

    /**
     * Every scroll change that is not ours (scrollbar drag, keyboard) is authoritative: cancel
     * the glide and apply instantly.
     */
    @Override
    public void setScrollAmount(double amount) {
        this.glideVelocity = 0.0;
        super.setScrollAmount(amount);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.advanceGlide();
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Advances the momentum glide and applies it to the list. */
    private void advanceGlide() {
        long now = Util.getMillis();
        double dt = this.lastFrameMs < 0
                ? 0.0
                : Math.min((now - this.lastFrameMs) / 1000.0, MAX_FRAME_SECONDS);
        this.lastFrameMs = now;
        if (this.glideVelocity == 0.0 || !this.scrollable()) {
            return;
        }
        double scrolled = this.scrollAmount() + this.glideVelocity * dt;
        this.glideVelocity *= Math.exp(-dt * SCROLL_FRICTION);
        if (Math.abs(this.glideVelocity) < SCROLL_STOP) {
            this.glideVelocity = 0.0;
        }
        double clamped = Math.clamp(scrolled, 0.0, this.maxScrollAmount());
        if (clamped != scrolled) {
            // Reached an end of the list: stop dead instead of pressing against the edge.
            this.glideVelocity = 0.0;
        }
        // The super setter, not the override: the glide must not cancel itself.
        super.setScrollAmount(clamped);
    }

    @Override
    protected void extractItem(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick, Row entry) {
        double frac = this.scrollAmount() - Math.floor(this.scrollAmount());
        gfx.pose().pushMatrix();
        gfx.pose().translate(0.0f, (float) -frac);
        super.extractItem(gfx, mouseX, mouseY, partialTick, entry);
        gfx.pose().popMatrix();
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor gfx) {
        // The rows are the only surfaces on this screen; there is no panel behind them.
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor gfx) {
        // No vanilla row separators.
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        if (!this.scrollable()) {
            return;
        }
        float w = 4.0f;
        float x = this.scrollBarX() + (this.scrollbarWidth() - w) / 2.0f;
        boolean hovered = mouseX >= x - 3.0f && mouseX <= x + w + 3.0f
                && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
        float trackTop = this.getY() + 2.0f;
        float trackH = this.getHeight() - 4.0f;
        Ui.pill(gfx, x, trackTop, w, trackH, Theme.CARD);
        float span = trackH - this.scrollerHeight();
        float thumbY = trackTop + (float) (this.scrollAmount() * span / this.maxScrollAmount());
        Ui.pill(gfx, x, thumbY, w, this.scrollerHeight(),
                hovered ? Theme.SCROLLBAR_HOVER : Theme.SCROLLBAR);
    }

    /**
     * A row of independent cards. The row itself draws nothing: it only lays its children out from
     * the current content coordinates every frame, which is what keeps them glued to the cards
     * while the list scrolls or the window resizes.
     */
    public static class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final List<AbstractWidget> widgets;
        /** Widths in the order the widgets appear; a negative width means "take the slack". */
        private final int[] widths;

        public Row(List<AbstractWidget> widgets, int[] widths) {
            this.widgets = widgets;
            this.widths = widths;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends NarratableEntry> narratables() {
            // Row children are always AbstractWidgets, which are GuiEventListener AND
            // NarratableEntry, so the same list serves both dispatch paths.
            return (List<? extends NarratableEntry>) (List<?>) this.widgets;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int fixed = 0;
            for (int width : this.widths) {
                if (width > 0) {
                    fixed += width;
                }
            }
            int slack = this.getWidth() - fixed - ROW_GAP * (this.widths.length - 1);

            int x = this.getX();
            for (int i = 0; i < this.widgets.size(); i++) {
                AbstractWidget widget = this.widgets.get(i);
                int w = this.widths[i] > 0 ? this.widths[i] : Math.max(20, slack);
                widget.setPosition(x, this.getY());
                widget.setWidth(w);
                widget.setHeight(CARD_HEIGHT);
                widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
                x += w + ROW_GAP;
            }
        }
    }
}
