package net.fayber.waypoints.gui;

import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WaypointEditScreen extends Screen {
    private static final int[] PRESET_COLORS = {
            0xFF00E5FF, 0xFF00E676, 0xFFFF1744, 0xFFD500F9,
            0xFFFFD600, 0xFFFF6D00, 0xFF2979FF, 0xFFFFFFFF
    };
    private static final String[] COLOR_NAMES = {"Cyan", "Green", "Red", "Purple", "Gold", "Orange", "Blue", "White"};

    private final Waypoint waypoint;
    private final boolean isNew;

    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private int selectedColor;
    private boolean chroma;
    private boolean beamEnabled;

    private Button[] colorButtons;
    private Button chromaButton;
    private int colorStartY;
    private String errorMessage;

    public WaypointEditScreen(Waypoint waypoint, boolean isNew) {
        super(Component.literal(isNew ? "Create Waypoint" : "Edit Waypoint"));
        this.waypoint = waypoint;
        this.isNew = isNew;
        this.selectedColor = waypoint.getColor();
        this.chroma = waypoint.isChroma();
        this.beamEnabled = waypoint.isBeaconBeam();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = 40;

        // Name input
        nameBox = new EditBox(this.font, centerX - 100, startY, 200, 20, Component.literal("Name"));
        nameBox.setValue(waypoint.getName());
        this.addRenderableWidget(nameBox);

        // Coordinates: X, Y, Z
        xBox = new EditBox(this.font, centerX - 100, startY + 30, 60, 20, Component.literal("X"));
        xBox.setValue(String.format("%.1f", waypoint.getX()).replace(',', '.'));
        this.addRenderableWidget(xBox);

        yBox = new EditBox(this.font, centerX - 30, startY + 30, 60, 20, Component.literal("Y"));
        yBox.setValue(String.format("%.1f", waypoint.getY()).replace(',', '.'));
        this.addRenderableWidget(yBox);

        zBox = new EditBox(this.font, centerX + 40, startY + 30, 60, 20, Component.literal("Z"));
        zBox.setValue(String.format("%.1f", waypoint.getZ()).replace(',', '.'));
        this.addRenderableWidget(zBox);

        // Beam toggle button
        this.addRenderableWidget(Button.builder(Component.literal("Beacon Beam: " + (beamEnabled ? "ON" : "OFF")), btn -> {
            beamEnabled = !beamEnabled;
            btn.setMessage(Component.literal("Beacon Beam: " + (beamEnabled ? "ON" : "OFF")));
        }).bounds(centerX - 100, startY + 60, 200, 20).build());

        // Color Presets
        colorStartY = startY + 90;
        colorButtons = new Button[PRESET_COLORS.length];
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int idx = i;
            int btnX = centerX - 100 + (i % 4) * 52;
            int btnY = colorStartY + (i / 4) * 24;
            Button b = Button.builder(Component.literal(COLOR_NAMES[i]), btn -> {
                selectedColor = PRESET_COLORS[idx];
                chroma = false;
                refreshColorSelectionUI();
            }).bounds(btnX, btnY, 48, 20).build();
            colorButtons[i] = b;
            this.addRenderableWidget(b);
        }

        // Chroma Rainbow toggle
        chromaButton = Button.builder(Component.literal("Dynamic Chroma (Rainbow)"), btn -> {
            chroma = true;
            refreshColorSelectionUI();
        }).bounds(centerX - 100, colorStartY + 52, 200, 20).build();
        this.addRenderableWidget(chromaButton);

        refreshColorSelectionUI();

        // Save & Cancel
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveAndClose())
                .bounds(centerX - 105, this.height - 35, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> this.onClose())
                .bounds(centerX + 5, this.height - 35, 100, 20)
                .build());
    }

    /** Updates button labels so the currently selected color / chroma state is actually visible. */
    private void refreshColorSelectionUI() {
        for (int i = 0; i < colorButtons.length; i++) {
            boolean selected = !chroma && selectedColor == PRESET_COLORS[i];
            colorButtons[i].setMessage(Component.literal((selected ? "» " : "") + COLOR_NAMES[i]));
        }
        chromaButton.setMessage(Component.literal("Dynamic Chroma (Rainbow): " + (chroma ? "§aON" : "§7OFF")));
    }

    private void saveAndClose() {
        Double px = parseOrNull(xBox.getValue());
        Double py = parseOrNull(yBox.getValue());
        Double pz = parseOrNull(zBox.getValue());
        if (px == null || py == null || pz == null) {
            errorMessage = "X/Y/Z must be valid numbers.";
            return;
        }

        waypoint.setName(nameBox.getValue().isBlank() ? "Waypoint" : nameBox.getValue());
        waypoint.setX(px);
        waypoint.setY(py);
        waypoint.setZ(pz);

        waypoint.setColor(selectedColor);
        waypoint.setChroma(chroma);
        waypoint.setBeaconBeam(beamEnabled);

        if (isNew) {
            WaypointStore.get().add(waypoint);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Live preview swatch of the currently selected/effective color (animates while chroma is on).
        int centerX = this.width / 2;
        int previewArgb = new WaypointColor(selectedColor, chroma).getEffectiveArgb();
        int swatchY = colorStartY - 9;
        graphics.fill(centerX - 100, swatchY, centerX - 60, swatchY + 8, 0xFF000000 | (previewArgb & 0xFFFFFF));
        graphics.fill(centerX - 100, swatchY, centerX - 60, swatchY + 1, 0xFFFFFFFF);
        graphics.fill(centerX - 100, swatchY + 7, centerX - 60, swatchY + 8, 0xFFFFFFFF);

        if (errorMessage != null) {
            graphics.centeredText(this.font, Component.literal("§c" + errorMessage), centerX, this.height - 55, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
