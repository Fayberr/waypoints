package net.fayber.waypoints.compat.xaero;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderHandler;

/**
 * Registers our waypoints as elements of Xaero's World Map, so they show up on the full-screen map
 * alongside Xaero's own markers.
 *
 * <p>No mixins. Xaero exposes {@code WorldMap.mapElementRenderHandler} as a public static field and
 * {@code MapElementRenderHandler#add} as a public method, which is the supported extension point:
 * we hand it a renderer and Xaero does the culling, panning, zooming, depth ordering and hover
 * detection for us.
 *
 * <p>Two timing details drive the shape of this class:
 * <ul>
 *   <li>The handler is not built during mod init. Xaero creates it in
 *       {@code WorldMapClientOnly#loadLaterClientRender}, i.e. after the client's resource load, so
 *       registering from {@code onInitializeClient} would hit a null field. We poll on the client
 *       tick instead.</li>
 *   <li>If Xaero ever rebuilds the handler, the field identity changes and our renderer would be
 *       silently dropped, so the tick compares identity rather than using a one-shot flag.</li>
 * </ul>
 *
 * <p>This class (and everything in this package) is only referenced from behind an
 * {@code isModLoaded("xaeroworldmap")} check in {@code WaypointsClient}, so its Xaero imports are
 * never classloaded when the World Map is absent. Xaero's jars are All Rights Reserved: the
 * dependency is {@code compileOnly} and nothing from them is ever shipped.
 *
 * <p>Deliberately World-Map-only: it does not touch anything from Xaero's Minimap, which is a
 * separate mod that may not be installed.
 */
public final class XaeroWorldMapCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("waypoints/xaero");

    private static final WaypointMapRenderer RENDERER = new WaypointMapRenderer();

    /** The handler instance we last added the renderer to, for identity comparison. */
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
