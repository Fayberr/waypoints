package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A labelled boolean option in its own card: label on the left, pill toggle on the right, and the
 * whole card is the click target (a 32x17 pill is a small thing to hit). State is read through the
 * supplier every frame, so a change from elsewhere shows up without the screen being rebuilt.
 */
public class ToggleCard extends AbstractButton {
    private static final int PAD_X = 11;
    private static final int TRACK_W = 32;
    private static final int TRACK_H = 17;
    private static final float KNOB_INSET = 2.5f;

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    /** 0 = off position, 1 = on position; negative means "not initialised yet". */
    private float knobPos = -1.0f;

    public ToggleCard(int x, int y, int width, int height, Component label,
                      BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, label);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.setter.accept(!this.getter.getAsBoolean());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean on = this.getter.getAsBoolean();
        boolean hovered = this.isHoveredOrFocused();

        float target = on ? 1.0f : 0.0f;
        if (this.knobPos < 0.0f) {
            this.knobPos = target; // opening the screen should not animate every toggle
        } else {
            this.knobPos += (target - this.knobPos) * 0.35f;
        }

        Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Theme.RADIUS,
                hovered ? Theme.CARD_HOVER : Theme.CARD,
                hovered ? Theme.CARD_BORDER_HOVER : Theme.CARD_BORDER, 1.0f);

        int labelWidth = this.getWidth() - 2 * PAD_X - TRACK_W - 8;
        Ui.text(gfx, Ui.ellipsize(this.getMessage(), Math.max(10, labelWidth)),
                this.getX() + PAD_X,
                this.getY() + (this.getHeight() - Ui.font().lineHeight) / 2 + 1,
                on || hovered ? Theme.TEXT : Theme.TEXT_SECONDARY);

        float trackX = this.getX() + this.getWidth() - PAD_X - TRACK_W;
        float trackY = this.getY() + (this.getHeight() - TRACK_H) / 2.0f;
        int track = on
                ? (hovered ? Theme.STRONG_HOVER : Theme.STRONG)
                : (hovered ? Theme.CARD_BORDER_HOVER : Theme.TRACK);
        int knob = on ? Theme.TEXT_ON_STRONG : (hovered ? Theme.TEXT : Theme.TEXT_SECONDARY);
        Ui.pill(gfx, trackX, trackY, TRACK_W, TRACK_H, track);

        float knobRadius = TRACK_H / 2.0f - KNOB_INSET;
        float left = trackX + KNOB_INSET + knobRadius;
        float right = trackX + TRACK_W - KNOB_INSET - knobRadius;
        Ui.circle(gfx, left + (right - left) * this.knobPos, trackY + TRACK_H / 2.0f, knobRadius, knob);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
