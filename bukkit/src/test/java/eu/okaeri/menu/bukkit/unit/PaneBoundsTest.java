package eu.okaeri.menu.bukkit.unit;

import eu.okaeri.menu.pane.PaneBounds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PaneBounds.
 * Tests coordinate conversion and overlap detection without any Bukkit dependencies.
 */
class PaneBoundsTest {

    @Test
    @DisplayName("Should create valid pane bounds")
    void testCreateBounds() {
        PaneBounds bounds = new PaneBounds(0, 0, 3, 9);

        assertThat(bounds.getX()).isZero();
        assertThat(bounds.getY()).isZero();
        assertThat(bounds.getWidth()).isEqualTo(9);
        assertThat(bounds.getHeight()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",   // Top-left corner
        "0, 8, 8",   // Top-right corner
        "2, 0, 18",  // Bottom-left corner
        "2, 8, 26",  // Bottom-right corner
        "1, 4, 13"   // Center
    })
    @DisplayName("Should convert local coordinates to global slot")
    void testToGlobalSlot(int localRow, int localCol, int expected) {
        PaneBounds bounds = new PaneBounds(0, 0, 3, 9);
        assertThat(bounds.toGlobalSlot(localRow, localCol)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle offset pane coordinates")
    void testOffsetPaneConversion() {
        PaneBounds bounds = new PaneBounds(1, 2, 2, 5); // Pane at x=2, y=1

        // Local (0, 0) -> Global x=2, y=1 -> Slot 11
        assertThat(bounds.toGlobalSlot(0, 0)).isEqualTo(11);

        // Local (4, 1) -> Global x=6, y=2 -> Slot 24
        assertThat(bounds.toGlobalSlot(1, 4)).isEqualTo(24);
    }

    @Test
    @DisplayName("Should detect overlapping panes")
    void testOverlapsDetection() {
        PaneBounds pane1 = new PaneBounds(0, 0, 3, 5);
        PaneBounds pane2 = new PaneBounds(1, 3, 2, 4);

        assertThat(pane1.overlaps(pane2)).isTrue();
        assertThat(pane2.overlaps(pane1)).isTrue(); // Symmetric
    }

    @Test
    @DisplayName("Should detect non-overlapping panes")
    void testNoOverlap() {
        PaneBounds pane1 = new PaneBounds(0, 0, 2, 4);
        PaneBounds pane2 = new PaneBounds(0, 5, 2, 4);

        assertThat(pane1.overlaps(pane2)).isFalse();
        assertThat(pane2.overlaps(pane1)).isFalse();
    }

    @Test
    @DisplayName("Should detect adjacent non-overlapping panes")
    void testAdjacentPanes() {
        PaneBounds pane1 = new PaneBounds(0, 0, 3, 4);
        PaneBounds pane2 = new PaneBounds(0, 4, 3, 5); // Starts where pane1 ends

        assertThat(pane1.overlaps(pane2)).isFalse();
    }

    @Test
    @DisplayName("Should detect pane contained within another")
    void testContainedPane() {
        PaneBounds outer = new PaneBounds(0, 0, 6, 9);
        PaneBounds inner = new PaneBounds(2, 2, 2, 5);

        assertThat(outer.overlaps(inner)).isTrue();
        assertThat(inner.overlaps(outer)).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions() {
        assertThatThrownBy(() -> new PaneBounds(0, 0, 3, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> new PaneBounds(0, 0, 0, 9))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> new PaneBounds(0, 0, 3, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("Should throw exception for out-of-bounds positions")
    void testInvalidPositions() {
        assertThatThrownBy(() -> new PaneBounds(0, -1, 3, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PaneBounds(-1, 0, 3, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PaneBounds(0, 10, 3, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds inventory width");
    }
}
