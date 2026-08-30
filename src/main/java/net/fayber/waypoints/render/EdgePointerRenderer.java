package net.fayber.waypoints.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws a small clamped arrow/indicator at the screen edge pointing towards a
 * waypoint that is currently outside the camera's field of view.
 */
public final class EdgePointerRenderer {

    public enum Side { LEFT, RIGHT, UP, DOWN }

    public static final class ClampResult {
        public final int x;
        public final int y;
        public final Side side;

        public ClampResult(int x, int y, Side side) {
            this.x = x;
            this.y = y;
            this.side = side;
        }
    }

    private EdgePointerRenderer() {
    }

    /**
     * Intersects the ray from the screen center along (dirX, dirY) with the inset
     * screen rectangle border, returning the clamp point and which edge it hit.
     */
    public static ClampResult clampToScreenEdge(int centerX, int centerY, double dirX, double dirY, int margin, int screenWidth, int screenHeight) {
        if (dirX == 0 && dirY == 0) {
            dirY = -1;
        }

        double txMax = dirX > 0 ? (double) (screenWidth - margin - centerX) / dirX
                : dirX < 0 ? (double) (margin - centerX) / dirX
                : Double.MAX_VALUE;
        double tyMax = dirY > 0 ? (double) (screenHeight - margin - centerY) / dirY
                : dirY < 0 ? (double) (margin - centerY) / dirY
                : Double.MAX_VALUE;

        double t = Math.min(txMax, tyMax);
        int x = (int) Math.round(centerX + dirX * t);
        int y = (int) Math.round(centerY + dirY * t);

        Side side;
        if (txMax <= tyMax) {
            side = dirX >= 0 ? Side.RIGHT : Side.LEFT;
        } else {
            side = dirY >= 0 ? Side.DOWN : Side.UP;
        }

        return new ClampResult(x, y, side);
    }

    public static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, Side side, int argb) {
        int s = 5;
        switch (side) {
            case UP -> drawTriangleUp(graphics, x, y, s, argb);
            case DOWN -> drawTriangleDown(graphics, x, y, s, argb);
            case LEFT -> drawTriangleLeft(graphics, x, y, s, argb);
            case RIGHT -> drawTriangleRight(graphics, x, y, s, argb);
        }
    }

    private static void drawTriangleUp(GuiGraphicsExtractor graphics, int cx, int cy, int size, int argb) {
        for (int row = 0; row <= size; row++) {
            int half = row;
            graphics.fill(cx - half, cy - size + row, cx + half + 1, cy - size + row + 1, argb);
        }
    }

    private static void drawTriangleDown(GuiGraphicsExtractor graphics, int cx, int cy, int size, int argb) {
        for (int row = 0; row <= size; row++) {
            int half = size - row;
            graphics.fill(cx - half, cy + row, cx + half + 1, cy + row + 1, argb);
        }
    }

    private static void drawTriangleLeft(GuiGraphicsExtractor graphics, int cx, int cy, int size, int argb) {
        for (int col = 0; col <= size; col++) {
            int half = col;
            graphics.fill(cx - size + col, cy - half, cx - size + col + 1, cy + half + 1, argb);
        }
    }

    private static void drawTriangleRight(GuiGraphicsExtractor graphics, int cx, int cy, int size, int argb) {
        for (int col = 0; col <= size; col++) {
            int half = size - col;
            graphics.fill(cx + col, cy - half, cx + col + 1, cy + half + 1, argb);
        }
    }
}
