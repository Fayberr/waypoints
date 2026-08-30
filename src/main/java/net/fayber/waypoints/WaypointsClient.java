package net.fayber.waypoints;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.gui.WaypointEditScreen;
import net.fayber.waypoints.gui.WaypointScreen;
import net.fayber.waypoints.hud.WaypointHudElement;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointStore;
import net.fayber.waypoints.render.WaypointRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class WaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "waypoints";

    public static KeyMapping openMenuKey;
    public static KeyMapping quickAddKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.withDefaultNamespace("waypoints"));

        // Register keybindings
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.waypoints.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        quickAddKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.waypoints.quick_add",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                category
        ));

        // World connection lifecycle
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WaypointStore.get().reload();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WaypointStore.get().save();
        });

        // In-World 3D Rendering hook (beams + billboard pins/labels)
        LevelRenderEvents.END_MAIN.register(WaypointRenderer::render);

        // 2D HUD overlay hook (screen-edge pointer arrows for off-screen waypoints)
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints_hud"), new WaypointHudElement());

        // Tick events for key presses and death waypoint checks
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (openMenuKey.consumeClick()) {
                client.setScreen(new WaypointScreen());
            }

            while (quickAddKey.consumeClick()) {
                Vec3 pos = client.player.position();
                String dim = client.player.level().dimension().identifier().toString();
                Waypoint newWp = new Waypoint(null, "New Waypoint", pos.x, pos.y, pos.z, dim, WaypointColor.CYAN.getArgb(), false);
                client.setScreen(new WaypointEditScreen(newWp, true));
            }

            // Death waypoint auto-removal on approach (was previously exposed in the settings
            // screen but never actually enforced anywhere).
            ModConfig config = ConfigManager.get();
            if (config.deathWaypointEnabled && config.deathWaypointAutoRemove && client.level != null) {
                String dim = client.level.dimension().identifier().toString();
                Vec3 pos = client.player.position();
                List<Waypoint> deaths = WaypointStore.get().getAll().stream()
                        .filter(Waypoint::isDeathWaypoint)
                        .filter(w -> w.getDimension().equals(dim))
                        .toList();
                for (Waypoint deathWp : deaths) {
                    double dx = deathWp.getX() - pos.x;
                    double dy = deathWp.getY() - pos.y;
                    double dz = deathWp.getZ() - pos.z;
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= config.deathWaypointAutoRemoveDist) {
                        WaypointStore.get().remove(deathWp.getId());
                    }
                }
            }
        });
    }
}
