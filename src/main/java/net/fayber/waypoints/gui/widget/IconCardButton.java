package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Glyph;
import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * A square card holding a single {@link Glyph}: the compact action buttons on a waypoint row
 * (show/hide, teleport, delete).
 *
 * <p>The glyph is supplied lazily so a button can change what it draws with the model (the
 * visibility button swaps between the open and crossed-out eye) without being rebuilt.
 */
public class IconCardButton extends AbstractButton {
    /** Glyph box as a fraction of the card, so the icon keeps its padding at any size. */
    private static final float GLYPH_FRACTION = 0.52f;

    private final Supplier<Glyph> glyph;
    private final Runnable onPress;

    public IconCardButton(int x, int y, int size, Supplier<Glyph> glyph, Runnable onPress, Component narration) {
        super(x, y, size, size, narration);
        this.glyph = glyph;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(Ui.ui(narration.getString())));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Theme.RADIUS_SMALL,
                hovered ? Theme.CARD_HOVER : Theme.CARD,
                hovered ? Theme.CARD_BORDER_HOVER : Theme.CARD_BORDER, 1.0f);

        int color = hovered ? Theme.TEXT : Theme.TEXT_SECONDARY;
        this.glyph.get().draw(gfx,
                this.getX() + this.getWidth() / 2.0f,
                this.getY() + this.getHeight() / 2.0f,
                this.getWidth() * GLYPH_FRACTION,
                color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
