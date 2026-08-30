package net.fayber.waypoints.gui;

import net.fayber.waypoints.compat.WaypointsClothScreen;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

public class WaypointScreen extends Screen {
    private EditBox searchBox;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 5;

    public WaypointScreen() {
        super(Component.literal("Waypoints"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildUI();
    }

    private void rebuildUI() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int topY = 25;

        // Search Box
        String prevQuery = searchBox != null ? searchBox.getValue() : "";
        searchBox = new EditBox(this.font, centerX - 140, topY, 190, 20, Component.literal("Search"));
        searchBox.setValue(prevQuery);
        searchBox.setResponder(text -> rebuildUI());
        this.addRenderableWidget(searchBox);

        // "+ New" Button
        this.addRenderableWidget(Button.builder(Component.literal("+ New"), btn -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Vec3 pos = mc.player.position();
            String dim = mc.player.level().dimension().identifier().toString();
            Waypoint newWp = new Waypoint(null, "New Waypoint", pos.x, pos.y, pos.z, dim, WaypointColor.CYAN.getArgb(), false);
            mc.setScreenAndShow(new WaypointEditScreen(newWp, true));
        }).bounds(centerX + 60, topY, 80, 20).build());

        // Waypoint list filtering
        String query = searchBox.getValue().toLowerCase().trim();
        List<Waypoint> waypoints = WaypointStore.get().getAll().stream()
                .filter(wp -> query.isEmpty() || wp.getName().toLowerCase().contains(query) || wp.getDimension().toLowerCase().contains(query))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) waypoints.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, waypoints.size());

        int itemY = topY + 28;
        for (int i = startIdx; i < endIdx; i++) {
            final Waypoint wp = waypoints.get(i);
            int rowY = itemY + (i - startIdx) * 28;

            // Visibility Toggle
            String visLabel = wp.isVisible() ? "§aON" : "§cOFF";
            this.addRenderableWidget(Button.builder(Component.literal(visLabel), btn -> {
                wp.setVisible(!wp.isVisible());
                WaypointStore.get().save();
                rebuildUI();
            }).bounds(centerX - 140, rowY, 35, 20).build());

            // Waypoint Name & Info button (opens Edit)
            String nameInfo = wp.getName() + " (" + (int) wp.getX() + ", " + (int) wp.getY() + ", " + (int) wp.getZ() + ")";
            this.addRenderableWidget(Button.builder(Component.literal(nameInfo), btn -> {
                this.minecraft.setScreenAndShow(new WaypointEditScreen(wp, false));
            }).bounds(centerX - 100, rowY, 150, 20).build());

            // Teleport Button (runs client command /tp)
            this.addRenderableWidget(Button.builder(Component.literal("TP"), btn -> {
                if (this.minecraft.player != null) {
                    this.minecraft.player.connection.sendCommand(String.format("tp %.1f %.1f %.1f", wp.getX(), wp.getY(), wp.getZ()).replace(',', '.'));
                    this.onClose();
                }
            }).bounds(centerX + 55, rowY, 35, 20).build());

            // Delete Button
            this.addRenderableWidget(Button.builder(Component.literal("§c✕"), btn -> {
                WaypointStore.get().remove(wp.getId());
                rebuildUI();
            }).bounds(centerX + 95, rowY, 25, 20).build());
        }

        // Pagination buttons
        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                if (page > 0) {
                    page--;
                    rebuildUI();
                }
            }).bounds(centerX - 60, this.height - 55, 30, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                if (page < totalPages - 1) {
                    page++;
                    rebuildUI();
                }
            }).bounds(centerX + 30, this.height - 55, 30, 20).build());
        }

        // Footer buttons: Settings & Done
        this.addRenderableWidget(Button.builder(Component.literal("Settings"), btn -> {
            this.minecraft.setScreenAndShow(WaypointsClothScreen.create(this));
        }).bounds(centerX - 105, this.height - 28, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(centerX + 5, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
