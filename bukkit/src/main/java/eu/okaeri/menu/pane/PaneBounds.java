package eu.okaeri.menu.pane;

import lombok.NonNull;
import lombok.Value;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the bounds (position and size) of a pane within an inventory.
 * Uses column (x) and row (y) coordinates, where (0,0) is the top-left corner.
 *
 * <p>Use static factory methods like {@link #of(int, int, int, int)} to create instances
 * with validation, or the constructor for internal use without validation overhead.
 */
@Value
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
     * Converts local coordinates to a local slot index.
     * Local slot is calculated as (localY * width) + localX.
     *
     * @param localX Column within this pane (0 to width-1)
     * @param localY Row within this pane (0 to height-1)
     * @return Local slot index within this pane
     */
    public int coordinatesToLocalSlot(int localX, int localY) {
        return (localY * this.width) + localX;
    }

    /**
     * Converts a local slot index to a global slot index.
     * Local slot is calculated as (localY * width) + localX.
     *
     * @param localSlot The local slot index within this pane
     * @return Global slot index in the inventory
     */
    public int localSlotToGlobal(int localSlot) {
        int localX = localSlot % this.width;
        int localY = localSlot / this.width;
        return this.toGlobalSlot(localX, localY);
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

    /**
     * Validates that local coordinates are within this pane's bounds.
     * Throws an exception with a descriptive message if validation fails.
     *
     * @param localX Local X coordinate to validate (0 to width-1)
     * @param localY Local Y coordinate to validate (0 to height-1)
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public void validate(int localX, int localY) {
        if ((localX < 0) || (localX >= this.width)) {
            throw new IllegalArgumentException("Local X out of bounds: " + localX + " (width=" + this.width + ")");
        }
        if ((localY < 0) || (localY >= this.height)) {
            throw new IllegalArgumentException("Local Y out of bounds: " + localY + " (height=" + this.height + ")");
        }
    }

    /**
     * Creates a fluent slot iterator for this pane's bounds.
     * Allows easy iteration over slots with exclusion support.
     *
     * @return A new SlotIterator instance
     */
    @NonNull
    public SlotIterator slots() {
        return new SlotIterator(this);
    }

    /**
     * Functional interface for consuming three parameters.
     * A generic tri-function consumer for operations that require three inputs.
     *
     * @param <A> First parameter type
     * @param <B> Second parameter type
     * @param <C> Third parameter type
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    /**
     * Fluent API for iterating over slots in a pane with exclusion support.
     * Eliminates repetitive double-loop patterns.
     */
    public static class SlotIterator {
        private final PaneBounds bounds;
        private Set<Integer> excludedLocalSlots = null;

        private SlotIterator(@NonNull PaneBounds bounds) {
            this.bounds = bounds;
        }

        /**
         * Excludes specific local slots from iteration.
         *
         * @param localSlots Set of local slot indices to exclude
         * @return This iterator for chaining
         */
        @NonNull
        public SlotIterator exclude(@NonNull Set<Integer> localSlots) {
            if (this.excludedLocalSlots == null) {
                this.excludedLocalSlots = new HashSet<>();
            }
            this.excludedLocalSlots.addAll(localSlots);
            return this;
        }

        /**
         * Excludes slots that are keys in a map (e.g., static items).
         *
         * @param map Map whose keys represent local slots to exclude
         * @return This iterator for chaining
         */
        @NonNull
        public SlotIterator excludeKeys(@NonNull Map<Integer, ?> map) {
            if (this.excludedLocalSlots == null) {
                this.excludedLocalSlots = new HashSet<>();
            }
            this.excludedLocalSlots.addAll(map.keySet());
            return this;
        }

        /**
         * Iterates over all non-excluded slots, calling the action with (localX, localY, globalSlot).
         *
         * @param action The action to perform for each slot
         */
        public void forEach(@NonNull TriConsumer<Integer, Integer, Integer> action) {
            for (int localY = 0; localY < this.bounds.height; localY++) {
                for (int localX = 0; localX < this.bounds.width; localX++) {
                    int localSlot = (localY * this.bounds.width) + localX;

                    // Skip excluded slots
                    if ((this.excludedLocalSlots != null) && this.excludedLocalSlots.contains(localSlot)) {
                        continue;
                    }

                    int globalSlot = this.bounds.toGlobalSlot(localX, localY);
                    action.accept(localX, localY, globalSlot);
                }
            }
        }

        /**
         * Iterates over a map keyed by local slot indices, calling the action with (localSlot, globalSlot, value).
         * Useful for rendering items stored in a map keyed by local slot indices.
         *
         * @param <V>    The value type in the map
         * @param map    The map whose keys are local slot indices
         * @param action The action to perform for each entry, receiving (localSlot, globalSlot, value)
         */
        public <V> void forEachMap(@NonNull Map<Integer, V> map, @NonNull TriConsumer<Integer, Integer, V> action) {
            for (Map.Entry<Integer, V> entry : map.entrySet()) {
                int localSlot = entry.getKey();
                int globalSlot = this.bounds.localSlotToGlobal(localSlot);
                action.accept(localSlot, globalSlot, entry.getValue());
            }
        }

        /**
         * Fills all non-excluded slots with the specified item.
         *
         * @param inventory The inventory to fill
         * @param item      The item to place in each slot
         */
        public void fill(@NonNull Inventory inventory, ItemStack item) {
            this.forEach((localX, localY, globalSlot) -> {
                inventory.setItem(globalSlot, item);
            });
        }

        /**
         * Clears all non-excluded slots (sets them to null).
         *
         * @param inventory The inventory to clear
         */
        public void clear(@NonNull Inventory inventory) {
            this.forEach((localX, localY, globalSlot) -> {
                inventory.setItem(globalSlot, null);
            });
        }

        /**
         * Creates a sub-iterator for a specific range within this pane.
         * Useful for iterating over a subset of slots (e.g., first N items).
         *
         * @param startIndex Starting local slot index (inclusive)
         * @param endIndex   Ending local slot index (exclusive)
         * @return A new iterator for the specified range
         */
        @NonNull
        public SlotIterator range(int startIndex, int endIndex) {
            SlotIterator rangeIterator = new SlotIterator(this.bounds);

            // Copy exclusions
            if (this.excludedLocalSlots != null) {
                rangeIterator.excludedLocalSlots = new HashSet<>(this.excludedLocalSlots);
            } else {
                rangeIterator.excludedLocalSlots = new HashSet<>();
            }

            // Add exclusions for slots outside the range
            for (int i = 0; i < this.bounds.getSlotCount(); i++) {
                if ((i < startIndex) || (i >= endIndex)) {
                    rangeIterator.excludedLocalSlots.add(i);
                }
            }

            return rangeIterator;
        }
    }
}
