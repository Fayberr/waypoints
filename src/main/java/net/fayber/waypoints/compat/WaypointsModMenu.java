package net.fayber.waypoints.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.fayber.waypoints.gui.WaypointScreen;

public class WaypointsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return WaypointsClothScreen::create;
        }
        return parent -> new WaypointScreen();
    }
}
