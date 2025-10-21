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
        PaneBounds bounds = new PaneBounds(0, 0, 9, 3);

        assertThat(bounds.getX()).isZero();
        assertThat(bounds.getY()).isZero();
        assertThat(bounds.getWidth()).isEqualTo(9);
        assertThat(bounds.getHeight()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",   // Top-left corner
        "8, 0, 8",   // Top-right corner
        "0, 2, 18",  // Bottom-left corner
        "8, 2, 26",  // Bottom-right corner
        "4, 1, 13"   // Center
    })
    @DisplayName("Should convert local coordinates to global slot")
    void testToGlobalSlot(int localX, int localY, int expected) {
        PaneBounds bounds = new PaneBounds(0, 0, 9, 3);
        assertThat(bounds.toGlobalSlot(localX, localY)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle offset pane coordinates")
    void testOffsetPaneConversion() {
        PaneBounds bounds = new PaneBounds(2, 1, 5, 2); // Pane at x=2, y=1

        // Local (0, 0) -> Global x=2, y=1 -> Slot 11
        assertThat(bounds.toGlobalSlot(0, 0)).isEqualTo(11);

        // Local (4, 1) -> Global x=6, y=2 -> Slot 24
        assertThat(bounds.toGlobalSlot(4, 1)).isEqualTo(24);
    }

    @Test
    @DisplayName("Should detect overlapping panes")
    void testOverlapsDetection() {
        PaneBounds pane1 = new PaneBounds(0, 0, 5, 3);
        PaneBounds pane2 = new PaneBounds(3, 1, 4, 2);

        assertThat(pane1.overlaps(pane2)).isTrue();
        assertThat(pane2.overlaps(pane1)).isTrue(); // Symmetric
    }

    @Test
    @DisplayName("Should detect non-overlapping panes")
    void testNoOverlap() {
        PaneBounds pane1 = new PaneBounds(0, 0, 4, 2);
        PaneBounds pane2 = new PaneBounds(5, 0, 4, 2);

        assertThat(pane1.overlaps(pane2)).isFalse();
        assertThat(pane2.overlaps(pane1)).isFalse();
    }

    @Test
    @DisplayName("Should detect adjacent non-overlapping panes")
    void testAdjacentPanes() {
        PaneBounds pane1 = new PaneBounds(0, 0, 4, 3);
        PaneBounds pane2 = new PaneBounds(4, 0, 5, 3); // Starts where pane1 ends

        assertThat(pane1.overlaps(pane2)).isFalse();
    }

    @Test
    @DisplayName("Should detect pane contained within another")
    void testContainedPane() {
        PaneBounds outer = new PaneBounds(0, 0, 9, 6);
        PaneBounds inner = new PaneBounds(2, 2, 5, 2);

        assertThat(outer.overlaps(inner)).isTrue();
        assertThat(inner.overlaps(outer)).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions() {
        assertThatThrownBy(() -> new PaneBounds(0, 0, 0, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> new PaneBounds(0, 0, 9, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> new PaneBounds(0, 0, -1, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("Should throw exception for out-of-bounds positions")
    void testInvalidPositions() {
        assertThatThrownBy(() -> new PaneBounds(-1, 0, 5, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PaneBounds(0, -1, 5, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be negative");

        assertThatThrownBy(() -> new PaneBounds(10, 0, 5, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds inventory width");
    }
}
