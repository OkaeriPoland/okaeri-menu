package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for click handler execution in MenuListener.
 * Tests onClick, onLeftClick, onRightClick handler invocation and context passing.
 */
class ClickHandlerExecutionTest extends MenuListenerTestBase {

    @Test
    @DisplayName("Should execute onClick handler")
    void testOnClickExecution() {
        AtomicInteger clickCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.STONE)
                    .onClick(event -> clickCount.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Click twice
        for (int i = 0; i < 2; i++) {
            InventoryClickEvent event = new InventoryClickEvent(
                this.player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
            );
            this.listener.onInventoryClick(event);
        }

        assertThat(clickCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should execute left and right click handlers correctly")
    void testLeftRightClickHandlers() {
        AtomicInteger leftClicks = new AtomicInteger(0);
        AtomicInteger rightClicks = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 1)
                .item(0, 0, item()
                    .material(Material.EMERALD)
                    .onLeftClick(event -> leftClicks.incrementAndGet())
                    .onRightClick(event -> rightClicks.incrementAndGet())
                    .build())
                .build())
            .build();

        menu.open(this.player);

        // Left click
        InventoryClickEvent leftEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );
        this.listener.onInventoryClick(leftEvent);

        // Right click
        InventoryClickEvent rightEvent = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            0,
            ClickType.RIGHT,
            InventoryAction.PICKUP_HALF
        );
        this.listener.onInventoryClick(rightEvent);

        assertThat(leftClicks.get()).isEqualTo(1);
        assertThat(rightClicks.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should provide context with correct slot to handler")
    void testContextSlotAccuracy() {
        AtomicInteger capturedSlot = new AtomicInteger(-1);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(2)
            .pane(staticPane("test")
                .bounds(0, 0, 9, 2)
                .item(5, 1, item()  // Slot 14
                    .material(Material.GOLD_INGOT)
                    .onClick(event -> capturedSlot.set(event.getSlot()))
                    .build())
                .build())
            .build();

        menu.open(this.player);

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            14,  // Column 5, Row 1
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL
        );

        this.listener.onInventoryClick(event);

        assertThat(capturedSlot.get()).isEqualTo(14);
    }
}
