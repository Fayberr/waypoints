package net.fayber.waypoints.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Single source of truth for which settings screen opens: Fayber Config when installed, else the
 * Cloth Config screen, else the plain waypoints screen. Used by both the ModMenu entrypoint and
 * the Settings button in WaypointScreen.
 *
 * <p>Each concrete screen class is only referenced from its own branch AFTER the isModLoaded
 * check, so none of the optional mod's classes are classloaded when it is absent (all of them
 * are compileOnly dependencies).
 */
public final class ConfigScreenRouter {
    private ConfigScreenRouter() {}

    @Nullable
    public static Screen create(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("fayberconfig")) {
            return WaypointsFayberScreen.create(parent);
        }
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return WaypointsClothScreen.create(parent);
        }
        return null;
    }
}
