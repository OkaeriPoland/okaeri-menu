package eu.okaeri.menu.bukkit.unit.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.PaneBounds;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for StaticPane.
 * Tests builder validation, item positioning, rendering, and coordinate conversion.
 */
class StaticPaneTest {

    private ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu;
    private MenuContext context;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.player = this.server.addPlayer();
        this.menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(6)
            .build();
        this.context = new MenuContext(this.menu, this.player);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should create empty pane with default values")
    void testEmptyPane() {
        StaticPane pane = staticPane().build();

        assertThat(pane.getName()).isEqualTo("unnamed");
        assertThat(pane.getBounds()).isEqualTo(PaneBounds.fullInventory());
        assertThat(pane.getStaticItems()).isEmpty();
    }

    @Test
    @DisplayName("Should set name via builder")
    void testBuilderName() {
        StaticPane pane = staticPane()
            .name("test-pane")
            .build();

        assertThat(pane.getName()).isEqualTo("test-pane");
    }

    @Test
    @DisplayName("Should set bounds via builder (int params)")
    void testBuilderBoundsInt() {
        StaticPane pane = staticPane()
            .bounds(1, 2, 5, 3)
            .build();

        PaneBounds bounds = pane.getBounds();
        assertThat(bounds.getX()).isEqualTo(1);
        assertThat(bounds.getY()).isEqualTo(2);
        assertThat(bounds.getWidth()).isEqualTo(5);
        assertThat(bounds.getHeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should set bounds via builder (PaneBounds object)")
    void testBuilderBoundsObject() {
        PaneBounds customBounds = PaneBounds.of(2, 1, 7, 4);
        StaticPane pane = staticPane()
            .bounds(customBounds)
            .build();

        assertThat(pane.getBounds()).isEqualTo(customBounds);
    }

    @Test
    @DisplayName("Should add item at local coordinates")
    void testAddItem() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(4, 1, item)
            .build();

        assertThat(pane.getItem(4, 1)).isEqualTo(item);
    }

    @Test
    @DisplayName("Should add multiple items")
    void testAddMultipleItems() {
        MenuItem item1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem item2 = MenuItem.item().material(Material.GOLD_INGOT).build();
        MenuItem item3 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, item1)
            .item(4, 1, item2)
            .item(8, 2, item3)
            .build();

        assertThat(pane.getItem(0, 0)).isEqualTo(item1);
        assertThat(pane.getItem(4, 1)).isEqualTo(item2);
        assertThat(pane.getItem(8, 2)).isEqualTo(item3);
    }

    @Test
    @DisplayName("Should throw when adding item outside width bounds")
    void testAddItemOutsideWidthBounds() {
        StaticPane.Builder builder = staticPane()
            .bounds(0, 0, 5, 3);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(5, 0, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local X out of bounds: 5");
    }

    @Test
    @DisplayName("Should throw when adding item outside height bounds")
    void testAddItemOutsideHeightBounds() {
        StaticPane.Builder builder = staticPane()
            .bounds(0, 0, 9, 3);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(0, 3, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local Y out of bounds: 3");
    }

    @Test
    @DisplayName("Should throw when adding item at negative X")
    void testAddItemNegativeX() {
        StaticPane.Builder builder = staticPane()
            .bounds(0, 0, 9, 3);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(-1, 0, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local X out of bounds: -1");
    }

    @Test
    @DisplayName("Should throw when adding item at negative Y")
    void testAddItemNegativeY() {
        StaticPane.Builder builder = staticPane()
            .bounds(0, 0, 9, 3);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(0, -1, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local Y out of bounds: -1");
    }

    @Test
    @DisplayName("Should render items to inventory")
    void testRenderItems() {
        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .name("Diamond")
            .build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .name("Gold")
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, item1)  // Slot 0
            .item(4, 1, item2)  // Slot 13 (9 + 4)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        ItemStack slot0 = inventory.getItem(0);
        ItemStack slot13 = inventory.getItem(13);

        assertThat(slot0).isNotNull();
        assertThat(slot0.getType()).isEqualTo(Material.DIAMOND);

        assertThat(slot13).isNotNull();
        assertThat(slot13.getType()).isEqualTo(Material.GOLD_INGOT);
    }

    @Test
    @DisplayName("Should clear pane area before rendering")
    void testClearAreaBeforeRender() {
        // Create inventory with items in it
        Inventory inventory = this.server.createInventory(null, 54);
        inventory.setItem(0, new ItemStack(Material.DIRT));
        inventory.setItem(1, new ItemStack(Material.DIRT));
        inventory.setItem(9, new ItemStack(Material.DIRT));

        // Create pane that only fills one slot
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 2)  // 2 rows
            .item(0, 0, item)
            .build();

        pane.render(inventory, this.context);

        // Slot 0 should have our item
        assertThat(inventory.getItem(0)).isNotNull();
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);

        // Other slots in pane area should be cleared
        assertThat(inventory.getItem(1)).isNull();
        assertThat(inventory.getItem(9)).isNull();
    }

    @Test
    @DisplayName("Should handle pane bounds offset correctly")
    void testPaneBoundsOffset() {
        MenuItem item = MenuItem.item()
            .material(Material.EMERALD)
            .build();

        // Pane starts at (2, 1) with size (3, 2)
        StaticPane pane = staticPane()
            .bounds(2, 1, 3, 2)
            .item(1, 0, item)  // Local (1, 0) -> Global (3, 1) -> Slot 12
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Local (1, 0) in pane at (2, 1) should map to global slot 12
        // Global Y=1 means row 1, so slot 9-17. Global X=3 means column 3, so slot 12
        ItemStack slot12 = inventory.getItem(12);
        assertThat(slot12).isNotNull();
        assertThat(slot12.getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should get item by local coordinates")
    void testGetItemByLocalCoordinates() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(4, 1, item)
            .build();

        assertThat(pane.getItem(4, 1)).isEqualTo(item);
        assertThat(pane.getItem(0, 0)).isNull();
        assertThat(pane.getItem(8, 2)).isNull();
    }

    @Test
    @DisplayName("Should get item by global slot (within bounds)")
    void testGetItemByGlobalSlot() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(4, 1, item)  // Local (4, 1) -> Global slot 13
            .build();

        assertThat(pane.getItemByGlobalSlot(13)).isEqualTo(item);
        assertThat(pane.getItemByGlobalSlot(0)).isNull();
    }

    @Test
    @DisplayName("Should return null for global slot outside pane bounds")
    void testGetItemByGlobalSlotOutsideBounds() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        // Pane covers only rows 0-2
        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, item)
            .build();

        // Slot 27 is in row 3, outside pane
        assertThat(pane.getItemByGlobalSlot(27)).isNull();
    }

    @Test
    @DisplayName("Should handle global slot with pane offset")
    void testGetItemByGlobalSlotWithOffset() {
        MenuItem item = MenuItem.item().material(Material.EMERALD).build();

        // Pane at (2, 1) with size (5, 2)
        StaticPane pane = staticPane()
            .bounds(2, 1, 5, 2)
            .item(1, 0, item)  // Local (1, 0) -> Global (3, 1) -> Slot 12
            .build();

        assertThat(pane.getItemByGlobalSlot(12)).isEqualTo(item);
        assertThat(pane.getItemByGlobalSlot(0)).isNull();  // Outside pane
    }

    @Test
    @DisplayName("Should invalidate all items")
    void testInvalidate() {
        // Create items with reactive properties
        MenuItem item1 = MenuItem.item()
            .material(() -> Material.DIAMOND)
            .build();
        MenuItem item2 = MenuItem.item()
            .material(() -> Material.GOLD_INGOT)
            .build();

        StaticPane pane = staticPane()
            .item(0, 0, item1)
            .item(1, 0, item2)
            .build();

        // Render once to cache reactive properties
        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Invalidate
        pane.invalidate();

        // After invalidation, properties should be re-evaluated on next render
        // (We can't directly test caching, but we verify no errors occur)
        pane.render(inventory, this.context);
        assertThat(inventory.getItem(0)).isNotNull();
        assertThat(inventory.getItem(1)).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty pane rendering")
    void testEmptyPaneRendering() {
        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // All slots in pane should be empty
        for (int i = 0; i < 27; i++) {
            assertThat(inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should handle full inventory pane")
    void testFullInventoryPane() {
        StaticPane.Builder builder = staticPane()
            .bounds(0, 0, 9, 6);

        // Fill entire inventory
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 9; x++) {
                builder.item(x, y, MenuItem.item()
                    .material(Material.STONE)
                    .build());
            }
        }

        StaticPane pane = builder.build();
        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // All 54 slots should be filled
        for (int i = 0; i < 54; i++) {
            assertThat(inventory.getItem(i)).isNotNull();
            assertThat(inventory.getItem(i).getType()).isEqualTo(Material.STONE);
        }
    }

    @Test
    @DisplayName("Should handle invisible items (not rendered)")
    void testInvisibleItems() {
        MenuItem visibleItem = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(true)
            .build();

        MenuItem invisibleItem = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .visible(false)
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, visibleItem)
            .item(1, 0, invisibleItem)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        assertThat(inventory.getItem(0)).isNotNull();
        assertThat(inventory.getItem(1)).isNull();  // Invisible item not rendered
    }

    @Test
    @DisplayName("Should handle AIR material items (not rendered)")
    void testAirMaterialItems() {
        MenuItem airItem = MenuItem.item()
            .material(Material.AIR)
            .build();

        MenuItem normalItem = MenuItem.item()
            .material(Material.DIAMOND)
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, airItem)
            .item(1, 0, normalItem)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        assertThat(inventory.getItem(0)).isNull();  // AIR not rendered
        assertThat(inventory.getItem(1)).isNotNull();
    }

    @Test
    @DisplayName("Should replace existing items on re-render")
    void testReRender() {
        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .build();

        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, item1)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);

        // First render
        pane.render(inventory, this.context);
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);

        // Manually change inventory
        inventory.setItem(0, new ItemStack(Material.DIRT));
        inventory.setItem(1, new ItemStack(Material.DIRT));

        // Re-render should restore pane state
        pane.render(inventory, this.context);
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1)).isNull();  // Cleared
    }

    @Test
    @DisplayName("Should handle overlapping item replacements")
    void testOverlappingItems() {
        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .build();

        // Add two items at same position (second should overwrite)
        StaticPane pane = staticPane()
            .bounds(0, 0, 9, 3)
            .item(0, 0, item1)
            .item(0, 0, item2)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Second item should be rendered (map overwrites)
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.GOLD_INGOT);
    }
}
