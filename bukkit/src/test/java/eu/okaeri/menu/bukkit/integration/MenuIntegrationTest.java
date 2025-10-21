package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Menu using MockBukkit.
 * Tests menu creation, opening, and rendering with mocked Bukkit server.
 */
class MenuIntegrationTest {

    private ServerMock server;
    private JavaPlugin plugin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should create menu with valid configuration")
    void testCreateMenu() {
        Menu menu = Menu.builder()
            .title("Test Menu")
            .rows(3)
            .plugin(this.plugin)
            .build();

        assertNotNull(menu);
        assertEquals("Test Menu", menu.getTitle().get(null));
        assertEquals(3, menu.getRows());
    }

    @Test
    @DisplayName("Should open menu for player")
    void testOpenMenu() {
        PlayerMock player = this.server.addPlayer();

        Menu menu = Menu.builder()
            .title("Test Menu")
            .rows(3)
            .plugin(this.plugin)
            .build();

        menu.open(player);

        Inventory openInventory = player.getOpenInventory().getTopInventory();
        assertNotNull(openInventory);
        assertEquals(27, openInventory.getSize()); // 3 rows = 27 slots
    }

    @Test
    @DisplayName("Should render items in menu")
    void testRenderItems() {
        PlayerMock player = this.server.addPlayer();

        Menu menu = Menu.builder()
            .title("Test Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(0, 0, MenuItem.builder()
                    .material(Material.DIAMOND)
                    .name("Diamond")
                    .build())
                .item(4, 1, MenuItem.builder()
                    .material(Material.EMERALD)
                    .name("Emerald")
                    .build())
                .build())
            .plugin(this.plugin)
            .build();

        menu.open(player);

        Inventory inventory = player.getOpenInventory().getTopInventory();

        // Check diamond at slot 0
        assertNotNull(inventory.getItem(0));
        assertEquals(Material.DIAMOND, inventory.getItem(0).getType());

        // Check emerald at slot 13 (x=4, y=1 -> 1*9 + 4 = 13)
        assertNotNull(inventory.getItem(13));
        assertEquals(Material.EMERALD, inventory.getItem(13).getType());
    }

    @Test
    @DisplayName("Should validate pane overlap at build time")
    void testPaneOverlapValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            Menu.builder()
                .title("Invalid Menu")
                .rows(3)
                .pane("pane1", StaticPane.builder()
                    .name("pane1")
                    .bounds(0, 0, 5, 2)
                    .build())
                .pane("pane2", StaticPane.builder()
                    .name("pane2")
                    .bounds(3, 1, 4, 2) // Overlaps with pane1
                    .build())
                .build()
        );
    }

    @Test
    @DisplayName("Should validate pane fits within menu rows")
    void testPaneBoundsValidation() {
        assertThrows(IllegalArgumentException.class, () ->
            Menu.builder()
                .title("Invalid Menu")
                .rows(3)
                .pane("pane1", StaticPane.builder()
                    .name("pane1")
                    .bounds(0, 2, 9, 2) // Ends at row 4, but menu has only 3 rows
                    .build())
                .build()
        );
    }

    @Test
    @DisplayName("Should close menu for player")
    void testCloseMenu() {
        PlayerMock player = this.server.addPlayer();

        Menu menu = Menu.builder()
            .title("Test Menu")
            .rows(3)
            .plugin(this.plugin)
            .build();

        menu.open(player);
        assertNotNull(player.getOpenInventory().getTopInventory());

        // Close the menu (cleanup state + close inventory)
        menu.close(player);
        player.closeInventory();

        // After close, player should have default crafting view or null inventory
        var topInv = player.getOpenInventory().getTopInventory();
        assertTrue((topInv == null) || (topInv.getContents().length == 0)
            || "CRAFTING".equals(player.getOpenInventory().getType().name()));
    }

    @Test
    @DisplayName("Should support per-viewer inventories")
    void testPerViewerInventories() {
        PlayerMock player1 = this.server.addPlayer("Player1");
        PlayerMock player2 = this.server.addPlayer("Player2");

        Menu menu = Menu.builder()
            .title("Test Menu")
            .rows(3)
            .plugin(this.plugin)
            .build();

        menu.open(player1);
        menu.open(player2);

        // Both players should have the menu open
        assertNotNull(player1.getOpenInventory().getTopInventory());
        assertNotNull(player2.getOpenInventory().getTopInventory());

        // Inventories should be separate instances
        assertNotSame(
            player1.getOpenInventory().getTopInventory(),
            player2.getOpenInventory().getTopInventory()
        );
    }

    @Test
    @DisplayName("Should handle reactive properties")
    void testReactiveProperties() {
        PlayerMock player = this.server.addPlayer();
        int[] counter = {0};

        Menu menu = Menu.builder()
            .title(() -> "Count: " + counter[0])
            .rows(3)
            .plugin(this.plugin)
            .build();

        menu.open(player);

        // Update counter and refresh
        counter[0] = 5;
        menu.refresh(player);

        // Title should be updated (though MockBukkit may not fully simulate this)
        // This test mainly verifies no exceptions are thrown
        assertDoesNotThrow(() -> menu.refresh(player));
    }
}
