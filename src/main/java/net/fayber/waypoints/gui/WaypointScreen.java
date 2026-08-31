package net.fayber.waypoints.gui;

import net.fayber.waypoints.compat.ConfigScreenRouter;
import net.fayber.waypoints.gui.style.Icons;
import net.fayber.waypoints.gui.style.Theme;
import net.fayber.waypoints.gui.style.Ui;
import net.fayber.waypoints.gui.widget.CardButton;
import net.fayber.waypoints.gui.widget.IconCardButton;
import net.fayber.waypoints.gui.widget.StyledEditBox;
import net.fayber.waypoints.gui.widget.WaypointCard;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The waypoint list.
 *
 * <p>There is deliberately no panel: a search field, a row of cards per waypoint and a footer float
 * straight on the dimmed world, and every control is its own rounded card. The palette is a single
 * neutral dark ramp with no accent, so the only colour on screen is the waypoints' own pins.
 *
 * <p>Filtering rebuilds only the list's rows ({@link WaypointList#setRows}), never the screen's
 * widgets, so the search field keeps keyboard focus while typing.
 */
public class WaypointScreen extends Screen {
    private static final int CONTENT_WIDTH = 440;
    /** Space either side of the rows for the scrollbar. */
    private static final int SCROLL_GUTTER = 14;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 28;
    private static final int ACTION_SIZE = WaypointList.CARD_HEIGHT;

    private StyledEditBox searchBox;
    private WaypointList list;
    private int contentX;
    private int contentW;
    private int listTop;
    private int listBottom;
    private boolean empty;

    public WaypointScreen() {
        super(Component.literal("Waypoints"));
    }

    @Override
    protected void init() {
        this.contentW = Math.min(CONTENT_WIDTH, Math.max(200, this.width - 32));
        this.contentX = (this.width - this.contentW) / 2;

        int toolbarY = 40;
        int newWidth = 104;
        int searchWidth = this.contentW - newWidth - 6;

        String previousQuery = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new StyledEditBox(this.font, this.contentX, toolbarY, searchWidth, TOOLBAR_HEIGHT,
                Component.literal("Search waypoints"), Icons.SEARCH);
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Ui.ui("Search"));
        this.searchBox.setValue(previousQuery);
        this.searchBox.setResponder(text -> this.refreshRows());
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(new CardButton(this.contentX + this.contentW - newWidth, toolbarY,
                newWidth, TOOLBAR_HEIGHT, Component.literal("New"), this::createHere,
                CardButton.Style.STRONG, Icons.PLUS));

        this.listTop = toolbarY + TOOLBAR_HEIGHT + 12;
        this.listBottom = this.height - FOOTER_HEIGHT - 28;
        int listWidth = this.contentW + 2 * SCROLL_GUTTER;
        int listHeight = Math.max(WaypointList.ROW_HEIGHT, this.listBottom - this.listTop);

        this.list = new WaypointList(this.minecraft, listWidth, listHeight, this.listTop,
                this.contentW, List.of());
        this.list.updateSizeAndPosition(listWidth, listHeight, this.contentX - SCROLL_GUTTER, this.listTop);
        this.addRenderableWidget(this.list);
        this.refreshRows();

        int footerY = this.height - FOOTER_HEIGHT - 14;
        int footerWidth = 112;
        this.addRenderableWidget(new CardButton(this.contentX, footerY, footerWidth, FOOTER_HEIGHT,
                Component.literal("Settings"), this::openSettings, CardButton.Style.GHOST, Icons.GEAR));
        this.addRenderableWidget(new CardButton(this.contentX + this.contentW - footerWidth, footerY,
                footerWidth, FOOTER_HEIGHT, Component.literal("Done"), this::onClose));
    }

    /** Rebuilds the filtered rows without touching the screen's own widgets. */
    private void refreshRows() {
        String query = this.searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        List<Waypoint> waypoints = WaypointStore.get().getAll().stream()
                .filter(wp -> query.isEmpty()
                        || wp.getName().toLowerCase(Locale.ROOT).contains(query)
                        || wp.getDimension().toLowerCase(Locale.ROOT).contains(query))
                .toList();

        List<WaypointList.Row> rows = new ArrayList<>(waypoints.size());
        for (Waypoint wp : waypoints) {
            rows.add(this.buildRow(wp));
        }
        this.empty = rows.isEmpty();
        this.list.setRows(rows);
    }

    private WaypointList.Row buildRow(Waypoint wp) {
        // The glyph is supplied lazily rather than chosen once, so toggling visibility swaps the
        // eye for the crossed-out eye without rebuilding the row.
        IconCardButton visibility = new IconCardButton(0, 0, ACTION_SIZE,
                () -> wp.isVisible() ? Icons.EYE : Icons.EYE_OFF,
                () -> {
                    wp.setVisible(!wp.isVisible());
                    WaypointStore.get().save();
                },
                Component.literal("Show or hide this waypoint"));

        WaypointCard card = new WaypointCard(0, 0, 10, WaypointList.CARD_HEIGHT, wp,
                () -> this.minecraft.setScreen(new WaypointEditScreen(wp, false)));

        IconCardButton teleport = new IconCardButton(0, 0, ACTION_SIZE, () -> Icons.TELEPORT,
                () -> this.teleportTo(wp), Component.literal("Teleport to this waypoint"));

        IconCardButton delete = new IconCardButton(0, 0, ACTION_SIZE, () -> Icons.TRASH,
                () -> this.confirmDelete(wp), Component.literal("Delete this waypoint"));

        List<AbstractWidget> widgets = List.of(visibility, card, teleport, delete);
        return new WaypointList.Row(widgets, new int[] {ACTION_SIZE, -1, ACTION_SIZE, ACTION_SIZE});
    }

    private void createHere() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Vec3 pos = mc.player.position();
        String dim = mc.player.level().dimension().identifier().toString();
        Waypoint created = new Waypoint(null, "New Waypoint", pos.x, pos.y, pos.z, dim,
                WaypointColor.CYAN.getArgb(), false);
        mc.setScreen(new WaypointEditScreen(created, true));
    }

    /** Runs the vanilla teleport command; needs server-side permission, exactly as before. */
    private void teleportTo(Waypoint wp) {
        if (this.minecraft.player == null) {
            return;
        }
        this.minecraft.player.connection.sendCommand(
                String.format(Locale.ROOT, "tp %.1f %.1f %.1f", wp.getX(), wp.getY(), wp.getZ()));
        this.onClose();
    }

    private void confirmDelete(Waypoint wp) {
        this.minecraft.setScreen(new ConfirmCardScreen(
                Component.literal("Delete waypoint"),
                Component.literal("\"" + wp.getName() + "\" will be removed. This cannot be undone."),
                Component.literal("Delete"),
                confirmed -> {
                    if (confirmed) {
                        WaypointStore.get().remove(wp.getId());
                    }
                    this.minecraft.setScreen(this);
                    this.refreshRows();
                }));
    }

    private void openSettings() {
        Screen settings = ConfigScreenRouter.create(this);
        if (settings != null) {
            this.minecraft.setScreen(settings);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, Theme.SCRIM);
        Ui.text(gfx, Ui.uiBold(this.title.getString()), this.contentX, 18, Theme.TEXT);

        int count = WaypointStore.get().getAll().size();
        Ui.textRight(gfx, Ui.ui(count == 1 ? "1 waypoint" : count + " waypoints"),
                this.contentX + this.contentW, 19, Theme.TEXT_MUTED);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        if (this.empty) {
            boolean searching = !this.searchBox.getValue().isBlank();
            Ui.textCentered(gfx, Ui.ui(searching ? "No waypoints match that search."
                            : "No waypoints yet. Press New to place one here."),
                    this.width / 2, (this.listTop + this.listBottom) / 2 - 4, Theme.TEXT_MUTED);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
