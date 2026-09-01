package net.fayber.waypoints.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Which settings screen opens: fayberconfig if installed, else Cloth Config, else nothing.
 * Used by both the ModMenu entrypoint and the Settings button in WaypointScreen.
 *
 * <p>Each screen class is only referenced after its isModLoaded check, so none of the
 * optional mod's classes (all compileOnly deps) get classloaded when it is absent.
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
