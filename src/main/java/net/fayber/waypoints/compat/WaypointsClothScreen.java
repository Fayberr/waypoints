package net.fayber.waypoints.compat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fayber.waypoints.config.ConfigManager;
import net.fayber.waypoints.config.ModConfig;
import net.fayber.waypoints.config.TeleportButtonVisibility;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class WaypointsClothScreen {
    private WaypointsClothScreen() {}

    public static Screen create(Screen parent) {
        ModConfig config = ConfigManager.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Modern Waypoints Settings"))
                .setSavingRunnable(ConfigManager::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(eb.startIntSlider(Component.literal("Max Render Distance"), config.renderDistance, 0, 50000)
                .setDefaultValue(10000)
                .setTooltip(Component.literal("Maximum distance in blocks to render waypoints in-world (0 = unlimited)."))
                .setSaveConsumer(val -> config.renderDistance = val)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Always on Top"), config.alwaysOnTop)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Render waypoints through blocks/walls without depth obstruction."))
                .setSaveConsumer(val -> config.alwaysOnTop = val)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Screen-Edge Arrows"), config.showOffscreenPointers)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show pointer arrows at screen edges for off-screen waypoints."))
                .setSaveConsumer(val -> config.showOffscreenPointers = val)
                .build());

        general.addEntry(eb.startEnumSelector(Component.literal("Hide Teleport Button"),
                        TeleportButtonVisibility.class, config.teleportButtonVisibility)
                .setDefaultValue(TeleportButtonVisibility.NEVER)
                .setTooltip(Component.literal("When to hide the teleport button on waypoint cards."))
                .setSaveConsumer(val -> config.teleportButtonVisibility = val)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Xaero World Map Markers"), config.xaeroWorldMapEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Draw your waypoints on Xaero's World Map. Only has an effect when that mod is installed."))
                .setSaveConsumer(val -> config.xaeroWorldMapEnabled = val)
                .build());

        general.addEntry(eb.startFloatField(Component.literal("Xaero Marker Size"), config.xaeroMarkerScale)
                .setDefaultValue(1.0f)
                .setMin(0.5f).setMax(3.0f)
                .setTooltip(Component.literal("Size of the waypoint markers on Xaero's World Map, as a multiple of the default."))
                .setSaveConsumer(val -> config.xaeroMarkerScale = val)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.literal("Visuals"));
        visuals.addEntry(eb.startFloatField(Component.literal("Beacon Beam Width"), config.beaconWidth)
                .setDefaultValue(0.20f)
                .setMin(0.05f).setMax(2.0f)
                .setTooltip(Component.literal("Thickness/radius of the vertical glowing beam."))
                .setSaveConsumer(val -> config.beaconWidth = val)
                .build());

        visuals.addEntry(eb.startFloatField(Component.literal("Beacon Beam Opacity"), config.beaconAlpha)
                .setDefaultValue(0.65f)
                .setMin(0.1f).setMax(1.0f)
                .setTooltip(Component.literal("Transparency of the beacon beam."))
                .setSaveConsumer(val -> config.beaconAlpha = val)
                .build());

        visuals.addEntry(eb.startFloatField(Component.literal("Pin Scale"), config.pinScale)
                .setDefaultValue(1.0f)
                .setMin(0.2f).setMax(3.0f)
                .setTooltip(Component.literal("Scale factor for in-world billboard pins."))
                .setSaveConsumer(val -> config.pinScale = val)
                .build());

        visuals.addEntry(eb.startFloatField(Component.literal("Max Size (Close)"), config.labelMaxScale)
                .setDefaultValue(4.0f)
                .setMin(1.0f).setMax(16.0f)
                .setTooltip(Component.literal("Largest on-screen label size when very close, as a multiple of the size the card holds far away."))
                .setSaveConsumer(val -> config.labelMaxScale = val)
                .build());

        visuals.addEntry(eb.startFloatField(Component.literal("Min Size (Far)"), config.labelMinScale)
                .setDefaultValue(1.0f)
                .setMin(0.25f).setMax(3.0f)
                .setTooltip(Component.literal("Smallest on-screen label size when very far, as a multiple of the held size (1.0 = hold that size)."))
                .setSaveConsumer(val -> config.labelMinScale = val)
                .build());

        ConfigCategory deaths = builder.getOrCreateCategory(Component.literal("Death Waypoints"));
        deaths.addEntry(eb.startBooleanToggle(Component.literal("Enable Death Waypoints"), config.deathWaypointEnabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Automatically place a waypoint where you die."))
                .setSaveConsumer(val -> config.deathWaypointEnabled = val)
                .build());

        deaths.addEntry(eb.startBooleanToggle(Component.literal("Auto-Remove on Arrival"), config.deathWaypointAutoRemove)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Automatically delete death waypoint when approaching your corpse."))
                .setSaveConsumer(val -> config.deathWaypointAutoRemove = val)
                .build());

        deaths.addEntry(eb.startDoubleField(Component.literal("Auto-Remove Distance"), config.deathWaypointAutoRemoveDist)
                .setDefaultValue(6.0)
                .setMin(1.0).setMax(32.0)
                .setTooltip(Component.literal("Distance in blocks to trigger death waypoint auto-removal."))
                .setSaveConsumer(val -> config.deathWaypointAutoRemoveDist = val)
                .build());

        deaths.addEntry(eb.startIntSlider(Component.literal("Max Death Waypoints"), config.deathWaypointMaxCount, 1, 10)
                .setDefaultValue(1)
                .setTooltip(Component.literal("Number of past death points to keep (1 = latest only)."))
                .setSaveConsumer(val -> config.deathWaypointMaxCount = val)
                .build());

        return builder.build();
    }
}
