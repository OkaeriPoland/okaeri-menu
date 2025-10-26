package eu.okaeri.menu.pane;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Visual ASCII template for pane item placement.
 * <p>
 * Defines item positions using single-character markers in a grid layout.
 * Spaces are formatting-only and ignored during parsing.
 * '.' marks empty slots available for auto/paginated items.
 * Any other character is a marker for fixed item positions.
 * <p>
 * Example:
 * <pre>{@code
 * PaneTemplate template = PaneTemplate.parse("""
 *     F S . . . . . < >
 *     . . . . . . . . .
 *     . . . . . . . . .
 *     """);
 *
 * // Get all positions for marker 'F'
 * List<Position> positions = template.getPositions('F');
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PaneTemplate {

    /**
     * Map of marker character to list of positions where it appears.
     */
    private final @NonNull Map<Character, List<Position>> markerPositions;

    /**
     * Total number of rows in the template.
     */
    @Getter
    private final int rows;

    /**
     * Maximum number of columns in any row.
     */
    @Getter
    private final int columns;

    /**
     * Parses a template string into a PaneTemplate.
     * <p>
     * Template format:
     * <ul>
     *   <li>Each line represents a row</li>
     *   <li>Spaces are formatting-only (ignored)</li>
     *   <li>'.' marks empty slots</li>
     *   <li>Any other character is a marker for item positions</li>
     * </ul>
     *
     * @param template The ASCII template string
     * @return Parsed PaneTemplate
     * @throws IllegalArgumentException if template is empty
     */
    @NonNull
    public static PaneTemplate parse(@NonNull String template) {
        Map<Character, List<Position>> positions = new HashMap<>();
        String[] lines = template.strip().split("\n");

        if (lines.length == 0) {
            throw new IllegalArgumentException("Template cannot be empty");
        }

        int maxColumns = 0;

        // Parse each line
        for (int row = 0; row < lines.length; row++) {
            String line = lines[row].strip();
            int col = 0;  // Actual column position (ignoring spaces)

            for (int i = 0; i < line.length(); i++) {
                char marker = line.charAt(i);

                // Skip spaces entirely (they're just for formatting)
                if (marker == ' ') {
                    continue;
                }

                // Skip empty slots (.)
                if (marker == '.') {
                    col++;
                    continue;
                }

                // Store position for this marker
                positions
                    .computeIfAbsent(marker, k -> new ArrayList<>())
                    .add(new Position(row, col));
                col++;
            }

            maxColumns = Math.max(maxColumns, col);
        }

        return new PaneTemplate(positions, lines.length, maxColumns);
    }

    /**
     * Gets all positions for a given marker.
     *
     * @param marker The marker character
     * @return List of positions where this marker appears (empty if not found)
     */
    @NonNull
    public List<Position> getPositions(char marker) {
        return this.markerPositions.getOrDefault(marker, List.of());
    }

    /**
     * Checks if a marker exists in the template.
     *
     * @param marker The marker character to check
     * @return true if the marker appears at least once
     */
    public boolean hasMarker(char marker) {
        return this.markerPositions.containsKey(marker) && !this.markerPositions.get(marker).isEmpty();
    }

    /**
     * Validates that template dimensions match the given bounds.
     *
     * @param bounds The pane bounds to validate against
     * @throws IllegalArgumentException if dimensions don't match
     */
    public void validateDimensions(@NonNull PaneBounds bounds) {
        if (this.rows != bounds.getHeight()) {
            throw new IllegalArgumentException(
                "Template height (" + this.rows + ") doesn't match pane height (" + bounds.getHeight() + ")"
            );
        }
        if (this.columns != bounds.getWidth()) {
            throw new IllegalArgumentException(
                "Template width (" + this.columns + ") doesn't match pane width (" + bounds.getWidth() + ")"
            );
        }
    }

    /**
     * A position in the template grid.
     *
     * @param row The row index (0-based)
     * @param col The column index (0-based)
     */
    public record Position(int row, int col) {
    }
}
