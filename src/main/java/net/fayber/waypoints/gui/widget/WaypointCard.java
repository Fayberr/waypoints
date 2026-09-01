package net.fayber.waypoints.gui.widget;

import net.fayber.waypoints.gui.style.Icons;
import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.fayber.waypoints.model.Waypoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The wide card in the middle of a waypoint row: a pin in the waypoint's own colour, its name, and
 * its coordinates plus dimension underneath. Pressing it opens the edit screen. The waypoint's
 * colour is the only colour on the whole screen, so the eye goes straight to the pins; a hidden
 * waypoint is drawn muted.
 */
public class WaypointCard extends AbstractButton {
    private static final int PAD_X = 11;
    private static final float PIN_SIZE = 11.0f;
    private static final int TEXT_LEFT = PAD_X + 16;

    private final Waypoint waypoint;
    private final Runnable onPress;

    public WaypointCard(int x, int y, int w, int h, Waypoint waypoint, Runnable onPress) {
        super(x, y, w, h, Component.literal(waypoint.getName()));
        this.waypoint = waypoint;
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        boolean visible = this.waypoint.isVisible();

        Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Theme.RADIUS,
                hovered ? Theme.CARD_HOVER : Theme.CARD,
                hovered ? Theme.CARD_BORDER_HOVER : Theme.CARD_BORDER, 1.0f);

        int pinColor = this.waypoint.getEffectiveColor() | 0xFF000000;
        if (!visible) {
            pinColor = fade(pinColor, 0.35f);
        }
        Icons.PIN.draw(gfx, this.getX() + PAD_X + PIN_SIZE / 2.0f, this.getY() + this.getHeight() / 2.0f,
                PIN_SIZE, pinColor);

        int textX = this.getX() + TEXT_LEFT;
        int maxWidth = this.getWidth() - TEXT_LEFT - PAD_X;
        Component name = Ui.ellipsize(Ui.uiBold(this.waypoint.getName()), maxWidth);
        Component detail = Ui.ellipsize(Ui.ui(this.describe()), maxWidth);

        int nameColor = visible ? Theme.TEXT : Theme.TEXT_MUTED;
        int detailColor = visible ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED;
        Ui.text(gfx, name, textX, this.getY() + 6, nameColor);
        Ui.text(gfx, detail, textX, this.getY() + this.getHeight() - Ui.font().lineHeight - 5, detailColor);
    }

    /** "1234, 64, -87 in Overworld", with the namespace stripped off the dimension id. */
    private String describe() {
        String dimension = this.waypoint.getDimension();
        int colon = dimension.indexOf(':');
        if (colon >= 0) {
            dimension = dimension.substring(colon + 1);
        }
        dimension = dimension.replace('_', ' ');
        if (!dimension.isEmpty()) {
            dimension = dimension.substring(0, 1).toUpperCase(Locale.ROOT) + dimension.substring(1);
        }
        return String.format(Locale.ROOT, "%d, %d, %d  in  %s",
                (int) Math.floor(this.waypoint.getX()),
                (int) Math.floor(this.waypoint.getY()),
                (int) Math.floor(this.waypoint.getZ()),
                dimension);
    }

    private static int fade(int argb, float f) {
        int r = Math.round(((argb >> 16) & 0xFF) * f);
        int g = Math.round(((argb >> 8) & 0xFF) * f);
        int b = Math.round((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
