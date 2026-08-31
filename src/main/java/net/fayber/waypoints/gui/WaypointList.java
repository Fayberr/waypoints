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
 * <p>Mouse-wheel scrolling is animated, because vanilla's is instant: the wheel only moves a
 * target, and every frame the actual amount eases toward it with a time-normalised factor, so the
 * glide feels identical at any frame rate. Scrollbar drags and keyboard navigation bypass the
 * animation and stay 1:1 with the pointer.
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

    private static final double SCROLL_SPEED = 20.0;
    private static final double SCROLL_SETTLE = 0.5;
    private static final double MAX_FRAME_SECONDS = 0.1;

    private final int rowWidth;

    private double scrollTarget;
    private double scrollEased;
    private boolean applyingEasedScroll;
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        if (!this.scrollable()) {
            return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
        }
        this.scrollTarget = Math.clamp(this.scrollTarget - yDelta * this.scrollRate(),
                0.0, this.maxScrollAmount());
        return true;
    }

    /**
     * GUI pixels per wheel notch. Vanilla's own rate is {@code entryHeight / 2} (half a row),
     * which reads as sluggish here; two rows per notch matches the config screen and the glide
     * animation keeps fast multi-notch spins fluid.
     */
    @Override
    protected double scrollRate() {
        return 2.0 * ROW_HEIGHT;
    }

    @Override
    public void setScrollAmount(double amount) {
        if (this.applyingEasedScroll) {
            super.setScrollAmount(amount);
            return;
        }
        this.scrollEased = this.scrollTarget = amount;
        super.setScrollAmount(amount);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.advanceSmoothScroll();
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void advanceSmoothScroll() {
        long now = Util.getMillis();
        double dt = this.lastFrameMs < 0
                ? 0.0
                : Math.min((now - this.lastFrameMs) / 1000.0, MAX_FRAME_SECONDS);
        this.lastFrameMs = now;
        if (this.scrollable()) {
            this.scrollTarget = Math.clamp(this.scrollTarget, 0.0, this.maxScrollAmount());
            if (Math.abs(this.scrollTarget - this.scrollEased) <= SCROLL_SETTLE) {
                this.scrollEased = this.scrollTarget;
            } else {
                this.scrollEased += (this.scrollTarget - this.scrollEased)
                        * (1.0 - Math.exp(-dt * SCROLL_SPEED));
            }
        } else {
            this.scrollTarget = 0.0;
            this.scrollEased = 0.0;
        }
        if (this.scrollEased != this.scrollAmount()) {
            this.applyingEasedScroll = true;
            super.setScrollAmount(this.scrollEased);
            this.applyingEasedScroll = false;
            this.scrollEased = this.scrollTarget = this.scrollAmount();
        }
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
