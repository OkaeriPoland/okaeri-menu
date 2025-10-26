package eu.okaeri.menu.bukkit.unit.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.PaneBounds;
import eu.okaeri.menu.pane.StaticPane;
import eu.okaeri.menu.state.ViewerState;
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

import java.util.concurrent.atomic.AtomicBoolean;

import static eu.okaeri.menu.pane.StaticPane.staticPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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

        // Create a test menu
        Menu realMenu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(6)
            .build();

        // Spy on the menu to mock getViewerState
        this.menu = spy(realMenu);

        // Create MenuContext for rendering
        this.context = new MenuContext(this.menu, this.player);

        // Create ViewerState for per-player caching
        ViewerState viewerState = new ViewerState(this.context, null);
        when(this.menu.getViewerState(this.player.getUniqueId())).thenReturn(viewerState);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should create empty pane with default values")
    void testEmptyPane() {
        StaticPane pane = staticPane().name("test").build();

        assertThat(pane.getName()).isEqualTo("test");
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
            .name("test")
            .bounds(2, 1, 3, 5)
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
        PaneBounds customBounds = PaneBounds.of(1, 2, 4, 7);
        StaticPane pane = staticPane()
            .name("test")
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(1, 4, item)
            .build();

        assertThat(pane.getItem(1, 4)).isEqualTo(item);
    }

    @Test
    @DisplayName("Should add multiple items")
    void testAddMultipleItems() {
        MenuItem item1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem item2 = MenuItem.item().material(Material.GOLD_INGOT).build();
        MenuItem item3 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 0, item1)
            .item(1, 4, item2)
            .item(2, 8, item3)
            .build();

        assertThat(pane.getItem(0, 0)).isEqualTo(item1);
        assertThat(pane.getItem(1, 4)).isEqualTo(item2);
        assertThat(pane.getItem(2, 8)).isEqualTo(item3);
    }

    @Test
    @DisplayName("Should throw when adding item outside width bounds")
    void testAddItemOutsideWidthBounds() {
        StaticPane.Builder builder = staticPane()
            .name("test")
            .bounds(0, 0, 3, 5);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(0, 5, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local X out of bounds: 5");
    }

    @Test
    @DisplayName("Should throw when adding item outside height bounds")
    void testAddItemOutsideHeightBounds() {
        StaticPane.Builder builder = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(3, 0, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local Y out of bounds: 3");
    }

    @Test
    @DisplayName("Should throw when adding item at negative X")
    void testAddItemNegativeX() {
        StaticPane.Builder builder = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(0, -1, item))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Local X out of bounds: -1");
    }

    @Test
    @DisplayName("Should throw when adding item at negative Y")
    void testAddItemNegativeY() {
        StaticPane.Builder builder = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9);

        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() -> builder.item(-1, 0, item))
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 0, item1)  // Slot 0
            .item(1, 4, item2)  // Slot 13 (9 + 4)
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
            .name("test")
            .bounds(0, 0, 2, 9)  // 2 rows
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
            .name("test")
            .bounds(1, 2, 2, 3)
            .item(0, 1, item)  // Local (1, 0) -> Global (3, 1) -> Slot 12
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(1, 4, item)
            .build();

        assertThat(pane.getItem(1, 4)).isEqualTo(item);
        assertThat(pane.getItem(0, 0)).isNull();
        assertThat(pane.getItem(2, 8)).isNull();
    }

    @Test
    @DisplayName("Should get item by global slot (within bounds)")
    void testGetItemByGlobalSlot() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(1, 4, item)  // Local (4, 1) -> Global slot 13
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
            .name("test")
            .bounds(0, 0, 3, 9)
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
            .name("test")
            .bounds(1, 2, 2, 5)
            .item(0, 1, item)  // Local (1, 0) -> Global (3, 1) -> Slot 12
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
            .name("test")
            .item(0, 0, item1)
            .item(0, 1, item2)
            .build();

        // Render once to cache reactive properties
        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Invalidate
        this.context.getViewerState().invalidateProps();

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
            .name("test")
            .bounds(0, 0, 3, 9)
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
            .name("test")
            .bounds(0, 0, 6, 9);

        // Fill entire inventory
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 9; x++) {
                builder.item(y, x, MenuItem.item()
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 0, visibleItem)
            .item(0, 1, invisibleItem)
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 0, airItem)
            .item(0, 1, normalItem)
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
            .name("test")
            .bounds(0, 0, 3, 9)
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
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 0, item1)
            .item(0, 0, item2)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Second item should be rendered (map overwrites)
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.GOLD_INGOT);
    }

    @Test
    @DisplayName("Exception in MenuItem render should clear pane but not crash menu")
    void testMenuItemRenderErrorHandling() {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Render Error Test")
            .rows(3)
            .pane(staticPane("failingPane")
                .bounds(0, 0, 1, 9)
                .item(0, 0, MenuItem.item()
                    .material(() -> {
                        throw new RuntimeException("Material supplier failed!");
                    })
                    .build())
                .build())
            .pane(staticPane("workingPane")
                .bounds(1, 0, 1, 9)
                .item(0, 0, MenuItem.item()
                    .material(Material.EMERALD)
                    .name("Working Item")
                    .build())
                .build())
            .build();

        // Open menu - should not throw exception
        testMenu.open(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify failingPane was cleared (slots 0-8, all should be null)
        for (int slot = 0; slot < 9; slot++) {
            assertThat(inventory.getItem(slot))
                .as("Slot %d in failingPane should be cleared after error", slot)
                .isNull();
        }

        // Verify workingPane still rendered (slot 9 should have the emerald)
        assertThat(inventory.getItem(9))
            .as("Slot 9 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(9).getType())
            .isEqualTo(Material.EMERALD);
    }

    // ========================================
    // AUTO-POSITIONED ITEMS
    // ========================================

    @Test
    @DisplayName("Should auto-position items without coordinates")
    void testAutoPositionedItems() {
        MenuItem item1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem item2 = MenuItem.item().material(Material.GOLD_INGOT).build();
        MenuItem item3 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)  // Auto: slot 0
            .item(item2)  // Auto: slot 1
            .item(item3)  // Auto: slot 2
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.GOLD_INGOT);
        assertThat(inventory.getItem(2).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should reflow auto items when one becomes invisible")
    void testAutoItemReflow() {
        AtomicBoolean item2Visible = new AtomicBoolean(true);

        MenuItem item1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .visible(item2Visible::get)
            .build();
        MenuItem item3 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)
            .item(item2)
            .item(item3)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);

        // First render - all visible
        pane.render(inventory, this.context);
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.GOLD_INGOT);
        assertThat(inventory.getItem(2).getType()).isEqualTo(Material.EMERALD);

        // Hide item2
        item2Visible.set(false);
        this.context.getViewerState().invalidateProps();  // Clear cached reactive properties
        pane.render(inventory, this.context);

        // item3 should shift to slot 1 (reflow)
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.EMERALD);  // Reflowed!
        assertThat(inventory.getItem(2)).isNull();  // Cleared
    }

    @Test
    @DisplayName("Should skip static item slots when auto-positioning")
    void testAutoItemsSkipStaticSlots() {
        MenuItem staticItem = MenuItem.item().material(Material.BARRIER).build();
        MenuItem autoItem1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem autoItem2 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 1, staticItem)  // Occupy slot 1
            .item(autoItem1)         // Auto: slot 0
            .item(autoItem2)         // Auto: slot 2 (skip 1)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.BARRIER);
        assertThat(inventory.getItem(2).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should handle mixed static and auto items")
    void testMixedStaticAndAutoItems() {
        MenuItem static1 = MenuItem.item().material(Material.BARRIER).build();
        MenuItem static2 = MenuItem.item().material(Material.BEDROCK).build();
        MenuItem auto1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem auto2 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 2, 3)  // 3x2 = 6 slots
            .item(0, 1, static1)  // Slot 1
            .item(1, 2, static2)  // Slot 11 (row 1, col 2)
            .item(auto1)          // Slot 0
            .item(auto2)          // Slot 2 (skip 1)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.BARRIER);
        assertThat(inventory.getItem(2).getType()).isEqualTo(Material.EMERALD);
        assertThat(inventory.getItem(3)).isNull();
        assertThat(inventory.getItem(4)).isNull();
        assertThat(inventory.getItem(11).getType()).isEqualTo(Material.BEDROCK);
    }

    @Test
    @DisplayName("Should handle filler with auto items")
    void testFillerWithAutoItems() {
        MenuItem fillerItem = MenuItem.item().material(Material.GRAY_STAINED_GLASS_PANE).build();
        MenuItem autoItem1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem autoItem2 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 1, 9)  // Single row
            .filler(fillerItem)
            .item(autoItem1)     // Slot 0
            .item(autoItem2)     // Slot 1
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // First two slots have auto items
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.EMERALD);

        // Remaining slots should be filled
        for (int i = 2; i < 9; i++) {
            assertThat(inventory.getItem(i).getType()).isEqualTo(Material.GRAY_STAINED_GLASS_PANE);
        }
    }

    @Test
    @DisplayName("Should stop rendering auto items when pane is full")
    void testAutoItemsOverflow() {
        StaticPane.Builder builder = staticPane()
            .name("test")
            .bounds(0, 0, 1, 2);  // Only 2 slots

        // Add 5 auto items (more than slots available)
        for (int i = 0; i < 5; i++) {
            builder.item(MenuItem.item().material(Material.DIAMOND).build());
        }

        StaticPane pane = builder.build();
        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Only first 2 items should render
        assertThat(inventory.getItem(0)).isNotNull();
        assertThat(inventory.getItem(1)).isNotNull();
        assertThat(inventory.getItem(2)).isNull();  // No overflow
    }

    // ========================================
    // AUTO-ITEM GETTER TESTS
    // ========================================

    @Test
    @DisplayName("Should get auto-positioned items by global slot")
    void testGetAutoItemBySlot() {
        MenuItem item1 = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem item2 = MenuItem.item().material(Material.GOLD_INGOT).build();
        MenuItem item3 = MenuItem.item().material(Material.EMERALD).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)
            .item(item2)
            .item(item3)
            .build();

        // Render to populate cache
        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Verify getItemByGlobalSlot returns correct items
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item1);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item2);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isEqualTo(item3);
        assertThat(pane.getItemByGlobalSlot(3, this.context)).isNull();  // Empty slot
    }

    @Test
    @DisplayName("Should return null for invisible auto-item slot")
    void testGetInvisibleAutoItemReturnsNull() {
        MenuItem invisibleItem = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(false)
            .build();
        MenuItem visibleItem = MenuItem.item()
            .material(Material.EMERALD)
            .build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(invisibleItem)
            .item(visibleItem)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Invisible item is skipped, visible item takes slot 0
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(visibleItem);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isNull();
    }

    @Test
    @DisplayName("Should get reflowed auto-items by slot after visibility change")
    void testGetReflowedAutoItemBySlot() {
        AtomicBoolean item1Visible = new AtomicBoolean(true);

        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(() -> item1Visible.get())
            .build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .build();
        MenuItem item3 = MenuItem.item()
            .material(Material.EMERALD)
            .build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)
            .item(item2)
            .item(item3)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Initially: slot 0 = item1, slot 1 = item2, slot 2 = item3
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item1);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item2);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isEqualTo(item3);

        // Hide item1 and re-render
        item1Visible.set(false);
        this.context.getViewerState().invalidateProps();
        pane.render(inventory, this.context);

        // Now: slot 0 = item2, slot 1 = item3 (reflowed)
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item2);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item3);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isNull();
    }

    @Test
    @DisplayName("Should get correct auto-items with multiple invisible items")
    void testGetAutoItemWithMultipleInvisible() {
        AtomicBoolean item1Visible = new AtomicBoolean(false);
        AtomicBoolean item2Visible = new AtomicBoolean(false);

        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(() -> item1Visible.get())
            .build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .visible(() -> item2Visible.get())
            .build();
        MenuItem item3 = MenuItem.item()
            .material(Material.EMERALD)
            .build();
        MenuItem item4 = MenuItem.item()
            .material(Material.IRON_INGOT)
            .build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)
            .item(item2)
            .item(item3)
            .item(item4)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // item1 and item2 invisible, so slot 0 = item3, slot 1 = item4
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item3);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item4);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isNull();
    }

    @Test
    @DisplayName("Should get both static and auto items correctly")
    void testGetMixedStaticAutoItems() {
        MenuItem staticItem = MenuItem.item()
            .material(Material.BARRIER)
            .build();
        MenuItem autoItem1 = MenuItem.item()
            .material(Material.DIAMOND)
            .build();
        MenuItem autoItem2 = MenuItem.item()
            .material(Material.EMERALD)
            .build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(0, 1, staticItem)  // Static at slot 1
            .item(autoItem1)         // Auto at slot 0
            .item(autoItem2)         // Auto at slot 2 (skips slot 1)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Verify getItemByGlobalSlot returns correct items
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(autoItem1);  // Auto item
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(staticItem);  // Static item
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isEqualTo(autoItem2);  // Auto item
        assertThat(pane.getItemByGlobalSlot(3)).isNull();
    }

    @Test
    @DisplayName("Should get correct items after complex visibility toggling")
    void testGetItemsAfterComplexVisibilityToggle() {
        AtomicBoolean item2Visible = new AtomicBoolean(true);

        MenuItem item1 = MenuItem.item()
            .material(Material.DIAMOND)
            .build();
        MenuItem item2 = MenuItem.item()
            .material(Material.GOLD_INGOT)
            .visible(() -> item2Visible.get())
            .build();
        MenuItem item3 = MenuItem.item()
            .material(Material.EMERALD)
            .build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, 3, 9)
            .item(item1)
            .item(item2)
            .item(item3)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Initially all visible: slot 0 = item1, slot 1 = item2, slot 2 = item3
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item1);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item2);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isEqualTo(item3);

        // Hide item2
        item2Visible.set(false);
        this.context.getViewerState().invalidateProps();
        pane.render(inventory, this.context);

        // Now: slot 0 = item1, slot 1 = item3
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item1);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item3);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isNull();

        // Show item2 again
        item2Visible.set(true);
        this.context.getViewerState().invalidateProps();
        pane.render(inventory, this.context);

        // Back to: slot 0 = item1, slot 1 = item2, slot 2 = item3
        assertThat(pane.getItemByGlobalSlot(0, this.context)).isEqualTo(item1);
        assertThat(pane.getItemByGlobalSlot(1, this.context)).isEqualTo(item2);
        assertThat(pane.getItemByGlobalSlot(2, this.context)).isEqualTo(item3);
    }

    // ========================================
    // TEMPLATE TESTS
    // ========================================

    @Test
    @DisplayName("Should place items using template markers")
    void testTemplateBasicPlacement() {
        MenuItem filterItem = MenuItem.item().material(Material.DIAMOND).build();
        MenuItem sortItem = MenuItem.item().material(Material.EMERALD).build();
        MenuItem closeItem = MenuItem.item().material(Material.BARRIER).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, """
                F S . . . . . . C
                . . . . . . . . .
                . . . . . . . . .
                """)
            .item('F', filterItem)
            .item('S', sortItem)
            .item('C', closeItem)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Check template positions
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.DIAMOND);   // F at (0,0) = slot 0
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.EMERALD);   // S at (0,1) = slot 1
        assertThat(inventory.getItem(8).getType()).isEqualTo(Material.BARRIER);   // C at (0,8) = slot 8

        // Empty slots should be null
        assertThat(inventory.getItem(2)).isNull();
        assertThat(inventory.getItem(9)).isNull();
    }

    @Test
    @DisplayName("Should place same item at all marker positions")
    void testTemplateDuplicateMarkers() {
        MenuItem borderItem = MenuItem.item().material(Material.BLACK_STAINED_GLASS_PANE).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, """
                X X X X X
                X . . . X
                X X X X X
                """)
            .item('X', borderItem)
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Top border (row 0)
        for (int col = 0; col < 5; col++) {
            assertThat(inventory.getItem(col).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        }

        // Middle row - sides only
        assertThat(inventory.getItem(9).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);   // Left
        assertThat(inventory.getItem(13).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);  // Right
        assertThat(inventory.getItem(10)).isNull();  // Interior
        assertThat(inventory.getItem(11)).isNull();
        assertThat(inventory.getItem(12)).isNull();

        // Bottom border (row 2)
        for (int col = 0; col < 5; col++) {
            assertThat(inventory.getItem(18 + col).getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        }
    }

    @Test
    @DisplayName("Should work with auto items filling empty slots")
    void testTemplateWithAutoItems() {
        MenuItem controlItem = MenuItem.item().material(Material.ARROW).build();
        MenuItem contentItem = MenuItem.item().material(Material.DIAMOND).build();

        StaticPane pane = staticPane()
            .name("test")
            .bounds(0, 0, """
                < . . . >
                . . . . .
                """)
            .item('<', controlItem)
            .item('>', controlItem)
            .item(contentItem)  // Auto item
            .build();

        Inventory inventory = this.server.createInventory(null, 54);
        pane.render(inventory, this.context);

        // Template items
        assertThat(inventory.getItem(0).getType()).isEqualTo(Material.ARROW);  // < at (0,0)
        assertThat(inventory.getItem(4).getType()).isEqualTo(Material.ARROW);  // > at (0,4)

        // Auto item fills first available '.' slot
        assertThat(inventory.getItem(1).getType()).isEqualTo(Material.DIAMOND);  // First '.'
    }

    @Test
    @DisplayName("Should throw when using template item before defining template")
    void testTemplateRequiresTemplateFirst() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() ->
            staticPane()
                .name("test")
                .bounds(0, 0, 1, 9)  // Regular bounds, no template
                .item('X', item)  // No template defined
                .build()
        ).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Template must be defined");
    }

    @Test
    @DisplayName("Should throw when marker not found in template")
    void testTemplateInvalidMarker() {
        MenuItem item = MenuItem.item().material(Material.DIAMOND).build();

        assertThatThrownBy(() ->
            staticPane()
                .name("test")
                .bounds(0, 0, """
                    X . . . .
                    """)
                .item('Y', item)  // Y not in template
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Marker 'Y' not found");
    }
}
