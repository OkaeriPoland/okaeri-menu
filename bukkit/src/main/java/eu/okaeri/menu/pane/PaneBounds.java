package eu.okaeri.menu.pane;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents the bounds (position and size) of a pane within an inventory.
 * Uses column (x) and row (y) coordinates, where (0,0) is the top-left corner.
 *
 * <p>Use static factory methods like {@link #of(int, int, int, int)} to create instances
 * with validation, or the constructor for internal use without validation overhead.
 */
@Getter
@EqualsAndHashCode
public class PaneBounds {

    /**
     * Column (x) position of the pane's top-left corner
     */
    private final int x;

    /**
     * Row (y) position of the pane's top-left corner
     */
    private final int y;

    /**
     * Width of the pane in columns
     */
    private final int width;

    /**
     * Height of the pane in rows
     */
    private final int height;

    /**
     * Creates pane bounds with validation.
     *
     * @param x      Column position
     * @param y      Row position
     * @param width  Width in columns
     * @param height Height in rows
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public PaneBounds(int x, int y, int width, int height) {
        if ((x < 0) || (y < 0)) {
            throw new IllegalArgumentException("Pane position cannot be negative: (" + x + ", " + y + ")");
        }
        if ((width <= 0) || (height <= 0)) {
            throw new IllegalArgumentException("Pane size must be positive: " + width + "x" + height);
        }
        if ((x + width) > 9) {
            throw new IllegalArgumentException("Pane exceeds inventory width: x=" + x + " + width=" + width + " > 9");
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates pane bounds (convenience factory method).
     *
     * @param x      Column position (0-8 for standard chest)
     * @param y      Row position (0-5 for 6-row chest)
     * @param width  Width in columns (1-9)
     * @param height Height in rows (1-6)
     * @return The pane bounds
     * @throws IllegalArgumentException if any parameter is invalid
     */
    @NonNull
    public static PaneBounds of(int x, int y, int width, int height) {
        return new PaneBounds(x, y, width, height);
    }

    /**
     * Creates full-inventory bounds (9x6 chest).
     *
     * @return Bounds covering the entire inventory
     */
    @NonNull
    public static PaneBounds fullInventory() {
        return new PaneBounds(0, 0, 9, 6);
    }

    /**
     * Converts pane-local coordinates to global slot index.
     *
     * @param localX Column within this pane (0 to width-1)
     * @param localY Row within this pane (0 to height-1)
     * @return Global slot index in the inventory
     */
    public int toGlobalSlot(int localX, int localY) {
        if ((localX < 0) || (localX >= this.width)) {
            throw new IllegalArgumentException("Local X out of pane bounds: " + localX + " (width=" + this.width + ")");
        }
        if ((localY < 0) || (localY >= this.height)) {
            throw new IllegalArgumentException("Local Y out of pane bounds: " + localY + " (height=" + this.height + ")");
        }

        int globalX = this.x + localX;
        int globalY = this.y + localY;
        return (globalY * 9) + globalX;
    }

    /**
     * Checks if this pane overlaps with another pane.
     *
     * @param other The other pane bounds
     * @return true if the panes overlap
     */
    public boolean overlaps(@NonNull PaneBounds other) {
        return !(((this.x + this.width) <= other.x) ||
            ((other.x + other.width) <= this.x) ||
            ((this.y + this.height) <= other.y) ||
            ((other.y + other.height) <= this.y));
    }

    /**
     * Gets the total number of slots in this pane.
     *
     * @return The slot count
     */
    public int getSlotCount() {
        return this.width * this.height;
    }

    @Override
    public String toString() {
        return "PaneBounds{" +
            "x=" + this.x +
            ", y=" + this.y +
            ", width=" + this.width +
            ", height=" + this.height +
            '}';
    }
}
