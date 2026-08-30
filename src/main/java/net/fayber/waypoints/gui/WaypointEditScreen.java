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
    private final Waypoint waypoint;
    private final boolean isNew;

    private EditBox nameBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private int selectedColor;
    private boolean chroma;
    private boolean beamEnabled;

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
        int[] presetColors = {
                0xFF00E5FF, 0xFF00E676, 0xFFFF1744, 0xFFD500F9,
                0xFFFFD600, 0xFFFF6D00, 0xFF2979FF, 0xFFFFFFFF
        };
        String[] colorNames = {"Cyan", "Green", "Red", "Purple", "Gold", "Orange", "Blue", "White"};

        int colorStartY = startY + 90;
        for (int i = 0; i < presetColors.length; i++) {
            final int col = presetColors[i];
            int btnX = centerX - 100 + (i % 4) * 52;
            int btnY = colorStartY + (i / 4) * 24;
            this.addRenderableWidget(Button.builder(Component.literal(colorNames[i]), btn -> {
                selectedColor = col;
                chroma = false;
            }).bounds(btnX, btnY, 48, 20).build());
        }

        // Chroma Rainbow toggle
        this.addRenderableWidget(Button.builder(Component.literal("Dynamic Chroma (Rainbow)"), btn -> {
            chroma = true;
        }).bounds(centerX - 100, colorStartY + 52, 200, 20).build());

        // Save & Cancel
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> saveAndClose())
                .bounds(centerX - 105, this.height - 35, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> this.onClose())
                .bounds(centerX + 5, this.height - 35, 100, 20)
                .build());
    }

    private void saveAndClose() {
        waypoint.setName(nameBox.getValue().isBlank() ? "Waypoint" : nameBox.getValue());
        try {
            waypoint.setX(Double.parseDouble(xBox.getValue().trim()));
            waypoint.setY(Double.parseDouble(yBox.getValue().trim()));
            waypoint.setZ(Double.parseDouble(zBox.getValue().trim()));
        } catch (NumberFormatException ignored) {}

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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
