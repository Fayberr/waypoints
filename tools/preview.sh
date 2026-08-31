#!/usr/bin/env bash
# Headless design workbench for the Waypoints screens.
#
# Boots the dev client on a private Xvfb display, waits for the preview hook to open the requested
# waypoint screen with demo data, grabs a single frame with ffmpeg, then tears everything down.
# Lets the GUI be iterated on from a machine with no display.
#
# Usage: tools/preview.sh [screen] [output.png] [gui_scale] [wait_seconds]
#   screen: list (default) | edit | new
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCREEN="${1:-list}"
OUT="${2:-/tmp/waypoints-preview-$SCREEN.png}"
GUI_SCALE="${3:-3}"
WAIT="${4:-120}"
DISPLAY_NUM="${WP_DISPLAY:-:96}"
W=1280
H=720
JDK="${WP_JDK:-/home/fayber/.jdks/jdk-25.0.4+7}"

# This machine has no sound device, and OpenAL Soft segfaults inside the ALSA backend when it
# cannot open one, taking the whole JVM with it. The null backend keeps the client alive.
export ALSOFT_DRIVERS=null

cleanup() {
	[[ -n "${GRADLE_PID:-}" ]] && kill -9 "$GRADLE_PID" 2>/dev/null
	pkill -f "waypoints.preview=" 2>/dev/null
	[[ -n "${XVFB_PID:-}" ]] && kill -9 "$XVFB_PID" 2>/dev/null
	return 0
}
trap cleanup EXIT

# Force the GUI scale the screenshot should be judged at (the whole point is scale independence,
# so this is the knob to sweep).
mkdir -p "$HERE/run"
if [[ -f "$HERE/run/options.txt" ]]; then
	sed -i "s/^guiScale:.*/guiScale:${GUI_SCALE}/" "$HERE/run/options.txt"
	grep -q '^guiScale:' "$HERE/run/options.txt" || echo "guiScale:${GUI_SCALE}" >>"$HERE/run/options.txt"
else
	printf 'guiScale:%s\n' "$GUI_SCALE" >"$HERE/run/options.txt"
fi

Xvfb "$DISPLAY_NUM" -screen 0 "${W}x${H}x24" >/dev/null 2>&1 &
XVFB_PID=$!
sleep 2

LOG="/tmp/waypoints-preview.log"
: >"$LOG"
(
	cd "$HERE" || exit 1
	DISPLAY="$DISPLAY_NUM" ./gradlew runClient -PwpPreview="$SCREEN" \
		-Dorg.gradle.java.home="$JDK" >>"$LOG" 2>&1
) &
GRADLE_PID=$!

for ((i = 0; i < WAIT; i++)); do
	if grep -q "PREVIEW: opening demo screen" "$LOG" 2>/dev/null; then
		# Software rendering under Xvfb runs ticks slower than 20/s, so give the screen a
		# moment to settle (toggle knobs and list scroll are animated).
		sleep 6
		DISPLAY="$DISPLAY_NUM" ffmpeg -v error -f x11grab -video_size "${W}x${H}" \
			-i "$DISPLAY_NUM" -frames:v 1 -y "$OUT" </dev/null
		echo "captured: $OUT"
		exit 0
	fi
	sleep 1
done

echo "TIMEOUT: preview screen never opened, tail of $LOG:"
tail -30 "$LOG"
exit 1
