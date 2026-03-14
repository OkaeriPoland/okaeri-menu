package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.state.ViewerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for Menu.onClose() callback.
 * Tests handler invocation, timing, fail-safety, and multi-viewer scenarios.
 */
class MenuCloseCallbackTest extends MenuListenerTestBase {

    // ========================================
    // BASIC INVOCATION
    // ========================================

    @Test
    @DisplayName("Should fire onClose handler when inventory is closed")
    void testOnCloseFiresOnClose() {
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> closeCalled.set(true))
            .build();

        menu.open(this.player);

        // Simulate close
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        assertThat(closeCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Should provide correct player in onClose context")
    void testOnCloseCorrectPlayer() {
        AtomicReference<Player> capturedPlayer = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> capturedPlayer.set(ctx.getPlayer()))
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        assertThat(capturedPlayer.get()).isSameAs(this.player);
    }

    @Test
    @DisplayName("Should provide correct menu in onClose context")
    void testOnCloseCorrectMenu() {
        AtomicReference<Menu> capturedMenu = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> capturedMenu.set(ctx.getMenu()))
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        assertThat(capturedMenu.get()).isSameAs(menu);
    }

    // ========================================
    // TIMING - BEFORE CLEANUP
    // ========================================

    @Test
    @DisplayName("Should fire onClose before ViewerState cleanup")
    void testOnCloseBeforeViewerStateCleanup() {
        AtomicReference<ViewerState> capturedState = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> capturedState.set(ctx.getViewerState()))
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        // ViewerState should have been accessible in handler
        assertThat(capturedState.get()).isNotNull();
        // But now it should be cleaned up
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    @Test
    @DisplayName("Should allow reading inventory contents in onClose")
    void testOnCloseCanReadInventory() {
        AtomicReference<List<ItemStack>> capturedItems = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .pane(staticPane("content")
                .bounds(0, 0, 1, 9)
                .item(0, 0, item().interactive().build())
                .item(0, 1, item().interactive().build())
                .build())
            .onClose(ctx -> capturedItems.set(ctx.getSlotContents("content")))
            .build();

        menu.open(this.player);

        // Place items in interactive slots
        Inventory inv = this.player.getOpenInventory().getTopInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND, 5));
        inv.setItem(1, new ItemStack(Material.EMERALD, 3));

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        assertThat(capturedItems.get()).hasSize(2);
        assertThat(capturedItems.get().get(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(capturedItems.get().get(1).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should allow accessing per-player state in onClose")
    void testOnCloseCanAccessState() {
        AtomicReference<String> capturedValue = new AtomicReference<>();

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .state(defaults -> defaults.define("myKey", "myValue"))
            .onClose(ctx -> capturedValue.set(ctx.getString("myKey")))
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        assertThat(capturedValue.get()).isEqualTo("myValue");
    }

    // ========================================
    // FAIL-SAFETY
    // ========================================

    @Test
    @DisplayName("Should not crash when onClose handler throws exception")
    void testOnCloseExceptionHandled() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> {
                throw new RuntimeException("Handler error!");
            })
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());

        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();

        // ViewerState should still be cleaned up despite exception
        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    @Test
    @DisplayName("Should not crash when no onClose handler set")
    void testNoOnCloseHandler() {
        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .build();

        menu.open(this.player);

        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());

        assertThatCode(() -> this.listener.onInventoryClose(closeEvent))
            .doesNotThrowAnyException();

        assertThat(menu.getViewerState(this.player.getUniqueId())).isNull();
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should not fire onClose twice on double close")
    void testOnCloseNotFiredTwice() {
        AtomicInteger closeCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> closeCount.incrementAndGet())
            .build();

        menu.open(this.player);

        // First close
        InventoryCloseEvent closeEvent = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(closeEvent);

        // Second close (ViewerState already removed)
        menu.close(this.player);

        assertThat(closeCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fire onClose independently for each viewer")
    void testOnCloseMultipleViewers() {
        AtomicInteger closeCount = new AtomicInteger(0);

        Menu menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(1)
            .onClose(ctx -> closeCount.incrementAndGet())
            .build();

        Player player2 = server.addPlayer("Player2");

        menu.open(this.player);
        menu.open(player2);

        // Close player1
        InventoryCloseEvent close1 = new InventoryCloseEvent(this.player.getOpenInventory());
        this.listener.onInventoryClose(close1);

        assertThat(closeCount.get()).isEqualTo(1);

        // Player2 still open
        assertThat(menu.getViewerState(player2.getUniqueId())).isNotNull();

        // Close player2
        InventoryCloseEvent close2 = new InventoryCloseEvent(player2.getOpenInventory());
        this.listener.onInventoryClose(close2);

        assertThat(closeCount.get()).isEqualTo(2);
    }
}
