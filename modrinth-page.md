# Modern Waypoints - Modrinth page copy

Source of truth for the text on https://modrinth.com/mod/waypoints-mod.
Update here first, then run:

    modrinth copy waypoints-mod modrinth-page.md

The `## description` and `## body` sections are what gets uploaded.
The `#` title line and the parenthetical note below are local only.
Voice: short, concrete, no config-key dumps, no "and more". No em-dashes.

## description (short summary line)

Client-side waypoints with floating pins, vertical beams, distance labels, death waypoints and an in-game manager.

## body (full page)

# Modern Waypoints

Client-side waypoints, without a map suite. A waypoint is a floating pin with a vertical beam and a live distance label. Works on vanilla servers, modded servers, Realms and singleplayer, because everything except the rendering is client-side.

## Waypoints

- Floating pins that keep a sensible size at any distance, and translucent vertical beams with configurable opacity and fading.
- Distance labels that scale with camera distance and stay readable up close.
- Labels render through walls, and nearer labels always cover farther ones.
- Per-waypoint color with preset swatches, hex input and chroma mode.
- Screen-edge pointers for waypoints outside the camera view.

## Where they live

- Waypoints are isolated per world, server address and Realm, and filtered by dimension, so nothing leaks between servers or worlds.
- Dying places a death waypoint automatically, with configurable cleanup.

## Managing them

- An in-game manager screen with search, quick visibility toggles, editing and deletion.
- Two keybinds: open the manager and quick-add a waypoint at your position.
- Config screen through Fayber Config or Cloth Config, whichever is installed.
- Waypoints appear on Xaero's World Map when that mod is installed.

## Notes

- Client-side only: install on the client, join any server.
- Optional integrations: ModMenu, Xaero's World Map, and Fayber Config or Cloth Config for settings.
