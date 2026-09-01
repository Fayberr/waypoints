package net.fayber.waypoints.gui;

import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.fayber.waypoints.gui.widget.CardButton;
import net.fayber.waypoints.gui.widget.ColorPicker;
import net.fayber.waypoints.gui.widget.StyledEditBox;
import net.fayber.waypoints.gui.widget.ToggleCard;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Create or edit a single waypoint.
 *
 * <p>Same language as the list: no panel, one card per control, a single neutral dark ramp and no
 * accent colour. The old preset swatches are gone; colour comes from a real HSV picker (a
 * saturation/value square plus a hue bar) with a hex field beside it, so every colour is reachable
 * instead of eight.
 *
 * <p>The layout is derived from the window height rather than hard-coded, with the picker absorbing
 * whatever vertical space is left over, so the screen still fits at large GUI scales.
 */
public class WaypointEditScreen extends Screen {
    private static final int CONTENT_WIDTH = 380;
    private static final int GAP = 8;
    private static final int PICKER_MAX = 118;
    /**
     * Below this window height (GUI pixels) the roomy metrics do not fit, so the screen switches to
     * compact ones and drops the section labels. 720p at GUI scale 3 leaves only 240.
     */
    private static final int COMPACT_BELOW = 300;
    /** Width reserved right of the picker for the preview swatch and the hex field. */
    private static final int SIDE_WIDTH = 108;

    private final Waypoint waypoint;
    private final boolean isNew;

    private StyledEditBox nameBox;
    private StyledEditBox xBox;
    private StyledEditBox yBox;
    private StyledEditBox zBox;
    private StyledEditBox hexBox;
    private ColorPicker picker;

    private int selectedColor;
    private boolean chroma;
    private boolean beamEnabled;

    /** Set while the code writes the hex field, so its responder does not fight the picker. */
    private boolean syncingHex;
    private String errorMessage;

    // Metrics, chosen in init() from the window height.
    private int fieldHeight;
    private int sectionGap;
    /** Room above a card for its section label; 0 means the labels are dropped entirely. */
    private int labelHeight;
    private int buttonHeight;

    private int contentX;
    private int contentW;
    private int colourY;
    private int swatchY;
    private int swatchH;
    private boolean showSwatch;
    private int footerY;

    public WaypointEditScreen(Waypoint waypoint, boolean isNew) {
        super(Component.literal(isNew ? "New waypoint" : "Edit waypoint"));
        this.waypoint = waypoint;
        this.isNew = isNew;
        this.selectedColor = waypoint.getColor() | 0xFF000000;
        this.chroma = waypoint.isChroma();
        this.beamEnabled = waypoint.isBeaconBeam();
    }

    @Override
    protected void init() {
        this.contentW = Math.min(CONTENT_WIDTH, Math.max(220, this.width - 32));
        this.contentX = (this.width - this.contentW) / 2;

        // A tall window gets roomy cards and section labels; a short one (720p at GUI scale 3 is
        // only 240 GUI pixels tall) gets tighter metrics and no labels, which is what keeps the
        // colour picker and the footer on screen instead of hanging off the bottom.
        boolean compact = this.height < COMPACT_BELOW;
        this.fieldHeight = compact ? 24 : 28;
        this.sectionGap = compact ? 9 : 16;
        this.labelHeight = compact ? 0 : 13;
        this.buttonHeight = compact ? 24 : 28;
        int topMargin = compact ? 32 : 40;
        int bottomMargin = compact ? 8 : 12;

        // Everything except the picker has a fixed height, so the picker takes the remainder. On
        // extremely short windows it shrinks below its preferred minimum rather than pushing the
        // footer off screen.
        int fixed = 3 * this.labelHeight + 3 * this.fieldHeight + 3 * this.sectionGap;
        int footerSpace = this.sectionGap + this.buttonHeight;
        int available = this.height - topMargin - bottomMargin - fixed - footerSpace;
        int pickerHeight = Math.clamp(available, 24, PICKER_MAX);

        int total = fixed + pickerHeight + footerSpace;
        int y = Math.max(topMargin, (this.height - total) / 2);

        // Name
        y += this.labelHeight;
        this.nameBox = new StyledEditBox(this.font, this.contentX, y, this.contentW, this.fieldHeight,
                Component.literal("Name"));
        this.nameBox.setMaxLength(48);
        this.nameBox.setHint(Ui.ui("Waypoint name"));
        this.nameBox.setValue(this.waypoint.getName());
        this.addRenderableWidget(this.nameBox);
        y += this.fieldHeight + this.sectionGap + this.labelHeight;

        // Position
        int coordW = (this.contentW - 2 * GAP) / 3;
        this.xBox = this.coordinateBox(this.contentX, y, coordW, "X", this.waypoint.getX());
        this.yBox = this.coordinateBox(this.contentX + coordW + GAP, y, coordW, "Y", this.waypoint.getY());
        this.zBox = this.coordinateBox(this.contentX + this.contentW - coordW, y, coordW, "Z", this.waypoint.getZ());
        y += this.fieldHeight + this.sectionGap;

        // Toggles: two cards side by side, each its own card
        int toggleW = (this.contentW - GAP) / 2;
        this.addRenderableWidget(new ToggleCard(this.contentX, y, toggleW, this.fieldHeight,
                Ui.ui("Beacon beam"), () -> this.beamEnabled, v -> this.beamEnabled = v));
        this.addRenderableWidget(new ToggleCard(this.contentX + this.contentW - toggleW, y, toggleW, this.fieldHeight,
                Ui.ui("Rainbow"), () -> this.chroma, this::setChroma));
        y += this.fieldHeight + this.sectionGap + this.labelHeight;

        // Colour
        this.colourY = y;
        int pickerWidth = this.contentW - SIDE_WIDTH - GAP;
        this.picker = new ColorPicker(this.contentX, y, pickerWidth, pickerHeight, this.selectedColor);
        this.setChroma(this.chroma); // initial picker emphasis from the saved rainbow state
        this.picker.onChange(() -> {
            this.selectedColor = this.picker.getColor();
            // Picking a colour is an explicit choice, so it wins over the animated rainbow.
            this.setChroma(false);
            this.syncHexField();
        });
        this.addRenderableWidget(this.picker);

        int sideX = this.contentX + this.contentW - SIDE_WIDTH;
        this.swatchY = y;
        // The side column is swatch above, hex field below. When the picker is too short for both,
        // the swatch is dropped and the hex field takes the whole column.
        this.showSwatch = pickerHeight >= 2 * this.fieldHeight + GAP;
        this.swatchH = Math.max(14, pickerHeight - this.fieldHeight - GAP);

        this.hexBox = new StyledEditBox(this.font, sideX,
                y + (this.showSwatch ? pickerHeight - this.fieldHeight : 0), SIDE_WIDTH,
                this.fieldHeight, Component.literal("Hex colour"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setHint(Ui.ui("#RRGGBB"));
        this.hexBox.setResponder(this::onHexTyped);
        this.addRenderableWidget(this.hexBox);
        this.syncHexField();

        y += pickerHeight + this.sectionGap;

        // Footer
        this.footerY = y;
        int buttonW = (this.contentW - GAP) / 2;
        this.addRenderableWidget(new CardButton(this.contentX, y, buttonW, this.buttonHeight,
                Component.literal("Cancel"), this::onClose));
        this.addRenderableWidget(new CardButton(this.contentX + this.contentW - buttonW, y, buttonW, this.buttonHeight,
                Component.literal(this.isNew ? "Create" : "Save"), this::saveAndClose, CardButton.Style.STRONG));
    }

    private StyledEditBox coordinateBox(int x, int y, int w, String label, double value) {
        StyledEditBox box = new StyledEditBox(this.font, x, y, w, this.fieldHeight, Component.literal(label));
        box.setMaxLength(12);
        box.setHint(Ui.ui(label));
        box.setValue(String.format(Locale.ROOT, "%.1f", value));
        this.addRenderableWidget(box);
        return box;
    }

    /** Writes the current colour into the hex field without re-entering the responder. */
    private void syncHexField() {
        if (this.hexBox == null) {
            return;
        }
        this.syncingHex = true;
        this.hexBox.setValue(String.format(Locale.ROOT, "#%06X", this.selectedColor & 0x00FFFFFF));
        this.syncingHex = false;
    }

    /** Accepts "#RRGGBB" or "RRGGBB" as it is typed; a half-typed value is ignored. */
    private void onHexTyped(String text) {
        if (this.syncingHex) {
            return;
        }
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6 || hex.chars().anyMatch(c -> Character.digit(c, 16) < 0)) {
            return;
        }
        this.selectedColor = 0xFF000000 | Integer.parseInt(hex, 16);
        this.setChroma(false);
        this.picker.setColor(this.selectedColor);
    }

    /** What the waypoint will actually look like, including the rainbow animation. */
    private int previewColor() {
        return new WaypointColor(this.selectedColor, this.chroma).getEffectiveArgb() | 0xFF000000;
    }

    /**
     * Single writer for the rainbow flag: while rainbow wins, the picker is de-emphasised (drawn
     * darker, still clickable), because its colour is not what is shown on the map.
     */
    private void setChroma(boolean chroma) {
        this.chroma = chroma;
        if (this.picker != null) {
            this.picker.setDimmed(chroma);
        }
    }

    private void saveAndClose() {
        Double px = parseOrNull(this.xBox.getValue());
        Double py = parseOrNull(this.yBox.getValue());
        Double pz = parseOrNull(this.zBox.getValue());
        if (px == null || py == null || pz == null) {
            this.errorMessage = "X, Y and Z must be numbers.";
            return;
        }

        String name = this.nameBox.getValue().isBlank() ? "Waypoint" : this.nameBox.getValue();
        this.waypoint.setName(name);
        this.waypoint.setX(px);
        this.waypoint.setY(py);
        this.waypoint.setZ(pz);
        this.waypoint.snapToBlockCenter();
        this.waypoint.setColor(this.selectedColor);
        this.waypoint.setChroma(this.chroma);
        this.waypoint.setBeaconBeam(this.beamEnabled);

        if (this.isNew) {
            WaypointStore.get().add(this.waypoint);
        } else {
            WaypointStore.get().save();
        }
        this.onClose();
    }

    private static Double parseOrNull(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, Theme.SCRIM);
        Ui.text(gfx, Ui.uiBold(this.title.getString()), this.contentX, 18, Theme.TEXT);

        // Section labels sit above their cards; the cards themselves are drawn by their widgets.
        this.sectionLabel(gfx, "NAME", this.nameBox.getCardY());
        this.sectionLabel(gfx, "POSITION", this.xBox.getCardY());
        this.sectionLabel(gfx, "COLOUR", this.colourY);

        // A bad coordinate is reported next to the title, where there is always room for it.
        if (this.errorMessage != null) {
            Ui.textRight(gfx, Ui.ui(this.errorMessage), this.contentX + this.contentW, 19,
                    Theme.TEXT_SECONDARY);
        }

        // Live preview swatch beside the picker; it animates while rainbow is on. The picker cannot
        // represent an animated colour, so the swatch says which one is actually winning.
        if (this.showSwatch) {
            int swatchX = this.contentX + this.contentW - SIDE_WIDTH;
            Ui.roundRectBorder(gfx, swatchX, this.swatchY, SIDE_WIDTH, this.swatchH, Theme.RADIUS,
                    this.previewColor(), Theme.CARD_BORDER, 1.0f);
            if (this.chroma) {
                Ui.textCentered(gfx, Ui.uiBold("RAINBOW"), swatchX + SIDE_WIDTH / 2,
                        this.swatchY + (this.swatchH - Ui.font().lineHeight) / 2 + 1, 0xCC000000);
            }
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Draws a section label above a card, or nothing at all when the window is too short for them. */
    private void sectionLabel(GuiGraphicsExtractor gfx, String text, int cardY) {
        if (this.labelHeight == 0) {
            return;
        }
        Ui.text(gfx, Ui.uiBold(text), this.contentX, cardY - this.labelHeight + 3, Theme.TEXT_MUTED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
