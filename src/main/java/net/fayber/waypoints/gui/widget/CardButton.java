package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Glyph;
import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * A button that is its own card: rounded dark surface, hairline border, optional leading glyph,
 * centred label. Two weights, neither an accent colour: {@link Style#GHOST} is the standard dark
 * card, {@link Style#STRONG} is the near-white fill for the one confirming action on a screen.
 *
 * <p>{@link AbstractButton#extractWidgetRenderState} is final but only dispatches to
 * {@link #extractContents}, so overriding that replaces the vanilla sprite rendering while
 * keeping click sounds, enter/space activation and narration.
 */
public class CardButton extends AbstractButton {
    public enum Style {
        GHOST,
        STRONG
    }

    private final Runnable onPress;
    private final Style style;
    @Nullable
    private final Glyph glyph;

    public CardButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        this(x, y, w, h, message, onPress, Style.GHOST, null);
    }

    public CardButton(int x, int y, int w, int h, Component message, Runnable onPress, Style style) {
        this(x, y, w, h, message, onPress, style, null);
    }

    public CardButton(int x, int y, int w, int h, Component message, Runnable onPress, Style style,
                      @Nullable Glyph glyph) {
        super(x, y, w, h, Ui.ui(message.getString()));
        this.onPress = onPress;
        this.style = style;
        this.glyph = glyph;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        int fg;
        if (this.style == Style.STRONG) {
            Ui.roundRect(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Theme.RADIUS,
                    hovered ? Theme.STRONG_HOVER : Theme.STRONG);
            fg = Theme.TEXT_ON_STRONG;
        } else {
            Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Theme.RADIUS,
                    hovered ? Theme.CARD_HOVER : Theme.CARD,
                    hovered ? Theme.CARD_BORDER_HOVER : Theme.CARD_BORDER, 1.0f);
            fg = hovered ? Theme.TEXT : Theme.TEXT_SECONDARY;
        }

        int textWidth = Ui.width(this.getMessage());
        float glyphSize = 9.0f;
        float glyphGap = 5.0f;
        float contentWidth = textWidth + (this.glyph != null ? glyphSize + glyphGap : 0.0f);
        float left = this.getX() + (this.getWidth() - contentWidth) / 2.0f;
        float midY = this.getY() + this.getHeight() / 2.0f;

        if (this.glyph != null) {
            this.glyph.draw(gfx, left + glyphSize / 2.0f, midY, glyphSize, fg);
            left += glyphSize + glyphGap;
        }
        Ui.text(gfx, this.getMessage(), Math.round(left),
                Math.round(midY - Ui.font().lineHeight / 2.0f) + 1, fg);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
