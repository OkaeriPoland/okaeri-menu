package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for MenuListener drag and close event handling.
 * Tests drag event cancellation, inventory cleanup, and exception handling.
 */
class MenuListenerDragAndCloseTest extends MenuListenerTestBase {

    // ========================================
    // DRAG EVENTS
    // ========================================

    @Test
    @DisplayName("Should cancel drag into menu inventory")
    void testCancelDragIntoMenu() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create drag event that includes menu slots
        InventoryDragEvent dragEvent = new InventoryDragEvent(
            this.player.getOpenInventory(),
            null,
            new ItemStack(Material.DIAMOND),
            false,
            java.util.Map.of(0, new ItemStack(Material.DIAMOND))  // Dragging into slot 0 of menu
        );

        this.listener.onInventoryDrag(dragEvent);

        // Should be cancelled
        assertThat(dragEvent.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should allow drag in player inventory when menu open")
    void testAllowDragInPlayerInventory() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create drag event only in player inventory (slots 9+)
        InventoryDragEvent dragEvent = new InventoryDragEvent(
            this.player.getOpenInventory(),
            null,
            new ItemStack(Material.DIAMOND),
            false,
            java.util.Map.of(36, new ItemStack(Material.DIAMOND))  // Player inventory slot
        );

        this.listener.onInventoryDrag(dragEvent);

        // Should NOT be cancelled
        assertThat(dragEvent.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("Should handle exception in drag handler safely")
    void testExceptionInDragHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Create an event that will trigger the drag handler
        InventoryDragEvent dragEvent = new InventoryDragEvent(
            this.player.getOpenInventory(),
            null,
            new ItemStack(Material.DIAMOND),
            false,
            java.util.Map.of(0, new ItemStack(Material.DIAMOND))
        );

        // Should not throw even if internal error occurs
        assertThatCode(() -> this.listener.onInventoryDrag(dragEvent))
            .doesNotThrowAnyException();
    }

    // ========================================
    // CLOSE EVENTS & CLEANUP
    // ========================================

    @Test
    @DisplayName("Should cleanup ViewerState on inventory close")
    void testCleanupOnClose() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        // Verify ViewerState exists
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNotNull();

        // Simulate close
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        // ViewerState should be cleaned up
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    @Test
    @DisplayName("Should handle close event for non-menu inventory")
    void testCloseNonMenuInventory() {
        // Open regular chest (not a menu)
        Inventory chest = server.createInventory(null, 27, Component.text("Regular Chest"));
        this.player.openInventory(chest);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());

        // Should not throw
        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle exception in close handler safely")
    void testExceptionInCloseHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());

        // Should not throw
        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();
    }
}
