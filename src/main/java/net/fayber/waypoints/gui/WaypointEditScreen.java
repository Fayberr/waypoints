package net.fayber.waypoints.gui;

import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WaypointEditScreen extends Screen {
    private final Waypoint waypoint;
    private final boolean isNew;

    public WaypointEditScreen(Waypoint waypoint, boolean isNew) {
        super(Component.literal(isNew ? "New Waypoint" : "Edit Waypoint"));
        this.waypoint = waypoint;
        this.isNew = isNew;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
            if (isNew) {
                WaypointStore.get().add(waypoint);
            } else {
                WaypointStore.get().save();
            }
            this.onClose();
        }).bounds(centerX - 105, this.height - 35, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds(centerX + 5, this.height - 35, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
