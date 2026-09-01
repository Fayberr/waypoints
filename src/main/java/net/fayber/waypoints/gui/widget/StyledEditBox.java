package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Glyph;
import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.Nullable;

/**
 * A text field that sits in its own card instead of vanilla's beveled sprite frame.
 *
 * <p>With the border off, EditBox places its text at exactly {@code (getX(), getY())}, so the
 * widget sits at the text origin and the card rectangle is tracked separately. {@link #isMouseOver}
 * is widened back to the card so clicking the padding still focuses the field.
 */
public class StyledEditBox extends EditBox {
    /** Glyph height and font glyph height vanilla centres against. */
    private static final int GLYPH_HEIGHT = 8;
    private static final int PAD_X = 9;
    private static final float ICON_SIZE = 8.0f;

    private final int cardX;
    private final int cardY;
    private final int cardW;
    private final int cardH;
    @Nullable
    private final Glyph leading;

    public StyledEditBox(Font font, int cardX, int cardY, int cardW, int cardH, Component narration) {
        this(font, cardX, cardY, cardW, cardH, narration, null);
    }

    public StyledEditBox(Font font, int cardX, int cardY, int cardW, int cardH, Component narration,
                         @Nullable Glyph leading) {
        super(font, cardX, cardY, cardW, cardH, narration);
        this.cardX = cardX;
        this.cardY = cardY;
        this.cardW = cardW;
        this.cardH = cardH;
        this.leading = leading;

        this.setBordered(false);
        this.setTextShadow(false);
        this.setTextColor(Theme.TEXT);
        this.setTextColorUneditable(Theme.TEXT_MUTED);
        // EditBox renders the typed value with Style.EMPTY (the vanilla font), which clashes with
        // the styled hint beside it. The formatter hook lets the value ride the UI font.
        this.addFormatter((text, pos) -> FormattedCharSequence.forward(text, Ui.uiStyle()));
        // With bordered == false EditBox puts the text at getX()/getY() verbatim, so the widget
        // origin IS the text origin.
        int textLeft = this.textLeft();
        this.setX(textLeft);
        this.setY(cardY + (cardH - GLYPH_HEIGHT) / 2);
        this.setWidth(cardX + cardW - PAD_X - textLeft);
        this.setHeight(GLYPH_HEIGHT);
    }

    private int textLeft() {
        return this.cardX + PAD_X + (this.leading != null ? Math.round(ICON_SIZE) + 6 : 0);
    }

    /** The card's top edge. The widget's own {@code getY()} is the text box, not the card. */
    public int getCardY() {
        return this.cardY;
    }

    /** The card's left edge (see {@link #getCardY()}). */
    public int getCardX() {
        return this.cardX;
    }

    /** The clickable area is the whole card, not just the one text line inside it. */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isVisible()
                && mouseX >= this.cardX && mouseX < this.cardX + this.cardW
                && mouseY >= this.cardY && mouseY < this.cardY + this.cardH;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean active = this.isFocused() || this.isMouseOver(mouseX, mouseY);
        Ui.roundRectBorder(gfx, this.cardX, this.cardY, this.cardW, this.cardH, Theme.RADIUS,
                this.isFocused() ? Theme.CARD_HOVER : Theme.CARD,
                active ? Theme.CARD_BORDER_HOVER : Theme.CARD_BORDER, 1.0f);
        if (this.leading != null) {
            this.leading.draw(gfx, this.cardX + PAD_X + ICON_SIZE / 2.0f, this.cardY + this.cardH / 2.0f,
                    ICON_SIZE, Theme.TEXT_MUTED);
        }
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);
    }
}
