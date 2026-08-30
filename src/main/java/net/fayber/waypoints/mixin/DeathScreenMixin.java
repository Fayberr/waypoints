package net.fayber.waypoints.mixin;

import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.model.Waypoint;
import net.fayber.waypoints.model.WaypointColor;
import net.fayber.waypoints.model.WaypointIcon;
import net.fayber.waypoints.model.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    // Injected into the constructor (not init()) because Screen#init() also re-runs on every
    // window resize while the screen is open (e.g. alt-tab, F11) - a HEAD inject on init() would
    // silently create a fresh duplicate death waypoint on every resize. The constructor runs
    // exactly once per DeathScreen instance, i.e. exactly once per death.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onDeathScreenCreated(Component causeOfDeath, boolean hardcore, LocalPlayer player, CallbackInfo ci) {
        ModConfig config = ConfigManager.get();
        if (!config.deathWaypointEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.level == null) {
            return;
        }

        if (config.deathWaypointMaxCount <= 1) {
            WaypointStore.get().clearDeathWaypoints();
        }

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String dim = mc.level.dimension().identifier().toString();

        Waypoint deathWp = new Waypoint(
                null,
                "Death (" + timeStr + ")",
                player.getX(),
                player.getY(),
                player.getZ(),
                dim,
                WaypointColor.RED.getArgb(),
                false
        );
        deathWp.setIcon(WaypointIcon.SKULL);
        deathWp.setDeathWaypoint(true);

        WaypointStore.get().add(deathWp);

        if (config.deathWaypointMaxCount > 1) {
            WaypointStore.get().trimDeathWaypoints(config.deathWaypointMaxCount);
        }
    }
}
