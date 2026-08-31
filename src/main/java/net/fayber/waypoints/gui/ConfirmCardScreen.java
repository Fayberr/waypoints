package net.fayber.waypoints.gui;

import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.fayber.waypoints.gui.widget.CardButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

/**
 * A yes/no prompt in the waypoint screens' own style, so a destructive confirmation does not throw
 * the player back onto a vanilla-looking screen. Two cards and a couple of lines of text.
 */
public class ConfirmCardScreen extends Screen {
    private static final int CONTENT_WIDTH = 320;
    private static final int BUTTON_W = 120;
    private static final int BUTTON_H = 28;

    private final Component question;
    private final Component confirmLabel;
    private final Consumer<Boolean> callback;

    private List<FormattedCharSequence> lines = List.of();

    public ConfirmCardScreen(Component title, Component question, Component confirmLabel, Consumer<Boolean> callback) {
        super(title);
        this.question = question;
        this.confirmLabel = confirmLabel;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int contentW = Math.min(CONTENT_WIDTH, Math.max(120, this.width - 32));
        this.lines = this.font.split(Ui.ui(this.question.getString()), contentW);

        int buttonY = this.height / 2 + 14;
        int gap = 8;
        int left = (this.width - (BUTTON_W * 2 + gap)) / 2;

        this.addRenderableWidget(new CardButton(left, buttonY, BUTTON_W, BUTTON_H,
                Component.literal("Cancel"), () -> this.callback.accept(false)));
        this.addRenderableWidget(new CardButton(left + BUTTON_W + gap, buttonY, BUTTON_W, BUTTON_H,
                this.confirmLabel, () -> this.callback.accept(true), CardButton.Style.STRONG));
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, Theme.SCRIM);
        Ui.textCentered(gfx, Ui.uiBold(this.title.getString()), this.width / 2,
                this.height / 2 - 46, Theme.TEXT);
        int y = this.height / 2 - 24;
        for (FormattedCharSequence line : this.lines) {
            gfx.text(this.font, line, this.width / 2 - this.font.width(line) / 2, y, Theme.TEXT_SECONDARY, false);
            y += this.font.lineHeight + 2;
        }
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
