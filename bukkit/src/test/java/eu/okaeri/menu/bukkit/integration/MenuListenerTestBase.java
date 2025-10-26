package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.MenuListener;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Base class for MenuListener integration tests.
 * Provides common setup, teardown, and helper methods.
 */
public abstract class MenuListenerTestBase {

    protected static ServerMock server;
    protected JavaPlugin plugin;
    protected MenuListener listener;
    protected Player player;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        this.plugin = MockBukkit.createMockPlugin();
        this.listener = MenuListener.register(this.plugin);
        this.player = server.addPlayer();
    }

    /**
     * Helper method to create a standard inventory click event.
     *
     * @param player    The player clicking
     * @param slot      The slot being clicked
     * @param clickType The type of click
     * @param action    The inventory action
     * @return The created event
     */
    protected InventoryClickEvent createClickEvent(Player player, int slot, ClickType clickType, InventoryAction action) {
        return new InventoryClickEvent(
            player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            slot,
            clickType,
            action
        );
    }

    /**
     * Helper method to create a left click event.
     *
     * @param player The player clicking
     * @param slot   The slot being clicked
     * @return The created event
     */
    protected InventoryClickEvent createLeftClick(Player player, int slot) {
        return this.createClickEvent(player, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    }

    /**
     * Helper method to create a right click event.
     *
     * @param player The player clicking
     * @param slot   The slot being clicked
     * @return The created event
     */
    protected InventoryClickEvent createRightClick(Player player, int slot) {
        return this.createClickEvent(player, slot, ClickType.RIGHT, InventoryAction.PICKUP_HALF);
    }

    /**
     * Helper method to create a shift click event.
     *
     * @param player The player clicking
     * @param slot   The slot being clicked
     * @return The created event
     */
    protected InventoryClickEvent createShiftClick(Player player, int slot) {
        return this.createClickEvent(player, slot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
    }
}
