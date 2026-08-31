package net.fayber.waypoints.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fayber.waypoints.gui.WaypointEditScreen;
import net.fayber.waypoints.gui.WaypointScreen;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Design workbench, ported from Fayber Config. Seeds a handful of demo waypoints and opens one of
 * the waypoint screens shortly after the dev client reaches the title screen, so the look can be
 * iterated on without a monitor: run the client under Xvfb, grab a frame, inspect, repeat (see
 * {@code tools/preview.sh}).
 *
 * <p>Inert unless the JVM is started with {@code -Dwaypoints.preview=list|edit|new}, so shipped
 * builds pay one boolean check at init. The demo waypoints are never written to disk: no world is
 * loaded at the title screen, and {@link WaypointStore#save()} no-ops without an active world.
 */
public final class PreviewHook {
    private static final Logger LOGGER = LoggerFactory.getLogger("waypoints");

    private PreviewHook() {
    }

    public static boolean enabled() {
        return System.getProperty("waypoints.preview") != null;
    }

    public static void register() {
        String mode = System.getProperty("waypoints.preview", "list");
        LOGGER.info("Waypoints preview hook armed ({})", mode);
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int ticks = 0;
            private boolean opened = false;

            @Override
            public void onEndTick(Minecraft client) {
                if (this.opened || !(client.screen instanceof TitleScreen) || ++this.ticks < 20) {
                    return;
                }
                this.opened = true;
                seed();
                LOGGER.info("PREVIEW: opening demo screen");
                client.setScreen(screenFor(mode));
            }
        });
    }

    private static Screen screenFor(String mode) {
        return switch (mode) {
            case "edit" -> new WaypointEditScreen(WaypointStore.get().getAll().getFirst(), false);
            case "new" -> new WaypointEditScreen(demo("New Waypoint", 128.5, 71.0, -344.5, 0xFF00E5FF, false), true);
            default -> new WaypointScreen();
        };
    }

    /** A spread of names, colours and dimensions, including one long name to test truncation. */
    private static void seed() {
        if (!WaypointStore.get().getAll().isEmpty()) {
            return;
        }
        WaypointStore.get().add(demo("Base", 128.5, 71.0, -344.5, 0xFF00E5FF, false));
        WaypointStore.get().add(demo("Ancient City Entrance Shaft", -1204.5, -32.0, 880.5, 0xFFD500F9, false));
        WaypointStore.get().add(demo("Nether Hub", 16.5, 122.0, 8.5, 0xFFFF6D00, false));
        WaypointStore.get().add(demo("Villager Trading Hall", 640.5, 64.0, 12.5, 0xFF00E676, true));
        WaypointStore.get().add(demo("Mine", -88.5, 12.0, 402.5, 0xFFFFD600, false));
        WaypointStore.get().add(demo("Ocean Monument", 2048.5, 45.0, -1536.5, 0xFF2979FF, false));
    }

    private static Waypoint demo(String name, double x, double y, double z, int color, boolean chroma) {
        return new Waypoint(null, name, x, y, z, "minecraft:overworld", color, chroma);
    }
}
