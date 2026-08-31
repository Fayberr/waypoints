package net.fayber.waypoints;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fayber.waypoints.compat.xaero.XaeroWorldMapCompat;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WaypointsClient implements ClientModInitializer {
    public static final String MOD_ID = "waypoints";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyMapping openMenuKey;
    public static KeyMapping quickAddKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        // Dev-only: -Dwaypoints.preview=list|edit|new opens that screen with demo data at boot.
        if (net.fayber.waypoints.dev.PreviewHook.enabled()) {
            net.fayber.waypoints.dev.PreviewHook.register();
        }

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

        // In-World 3D Rendering hook (beam + label card). All card pieces are submitted as
        // separate submit-order buckets from this single event so the GPU draw order is
        // guaranteed (glow < card body < card text; see PinRenderer's class doc).
        //
        // MUST be COLLECT_SUBMITS, not END_MAIN. COLLECT_SUBMITS fires at the return of
        // LevelRenderer#submitFeatures, i.e. while the submit-node collector is still being
        // filled and before it is drained. END_MAIN fires at the end of the main pass, after
        // both the solid and translucent feature batches have already been executed, so
        // geometry submitted there misses this frame's draw and gets flushed outside the
        // RenderSystem#getModelViewStack push that LevelRenderer#renderLevel makes to apply
        // the camera view-rotation matrix. The net effect is geometry that keeps its
        // translation but loses all camera rotation: the beam stays a dead-vertical line
        // welded to the viewport (and even draws when the waypoint is behind you) instead of
        // being real, perspective-correct, depth-tested world geometry.
        LevelRenderEvents.COLLECT_SUBMITS.register(WaypointRenderer::render);

        // Xaero's World Map integration: adds our waypoints to the full-screen map as map
        // elements. The compat class is only touched inside this branch so its Xaero imports stay
        // unloaded when the World Map is not installed (it is a compileOnly dependency), and the
        // catch keeps a future Xaero change from taking the whole mod down with it.
        if (FabricLoader.getInstance().isModLoaded("xaeroworldmap")) {
            try {
                XaeroWorldMapCompat.register();
            } catch (Throwable t) {
                LOGGER.warn("Xaero's World Map integration could not be set up", t);
            }
        }

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
