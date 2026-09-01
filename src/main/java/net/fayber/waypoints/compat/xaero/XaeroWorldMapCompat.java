package net.fayber.waypoints.compat.xaero;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderHandler;

/**
 * Registers our waypoints as elements of Xaero's World Map, so they show up on the full-screen
 * map alongside Xaero's own markers. No mixins: {@code WorldMap.mapElementRenderHandler} plus
 * {@code MapElementRenderHandler#add} is the supported extension point, and Xaero does the
 * culling, panning, zooming, depth ordering and hover detection for us.
 *
 * <p>The handler is not built during mod init (Xaero creates it in
 * {@code WorldMapClientOnly#loadLaterClientRender}, after the resource load), so we poll on the
 * client tick instead of registering from {@code onInitializeClient}. And if Xaero ever rebuilds
 * the handler the field identity changes and our renderer would be silently dropped, so the tick
 * compares identity rather than using a one-shot flag.
 *
 * <p>Only referenced from behind an {@code isModLoaded("xaeroworldmap")} check in
 * {@code WaypointsClient}, so its Xaero imports are never classloaded when the World Map is
 * absent. Deliberately World-Map-only: Xaero's Minimap is a separate mod that may not be installed.
 */
public final class XaeroWorldMapCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("waypoints/xaero");

    private static final WaypointMapRenderer RENDERER = new WaypointMapRenderer();

    /** Handler instance we last added the renderer to, for the identity check. */
    private static MapElementRenderHandler registeredWith;
    /** Set after a failure so a broken Xaero build cannot spam the log every tick. */
    private static boolean failed;

    private XaeroWorldMapCompat() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> attach());
        LOGGER.info("Xaero's World Map detected, waypoints will be added to the map");
    }

    private static void attach() {
        if (failed) {
            return;
        }
        MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
        if (handler == null || handler == registeredWith) {
            return;
        }
        try {
            registeredWith = handler;
            handler.add(RENDERER);
            LOGGER.info("Registered waypoints with Xaero's World Map element renderer");
        } catch (Throwable t) {
            failed = true;
            LOGGER.warn("Could not register waypoints with Xaero's World Map; map markers disabled", t);
        }
    }
}
