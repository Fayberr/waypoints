package net.fayber.waypoints.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class WaypointsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Routing (Fayber Config > Cloth Config > none) lives in ConfigScreenRouter so the
        // Settings button in WaypointScreen uses the same priority.
        return ConfigScreenRouter::create;
    }
}
