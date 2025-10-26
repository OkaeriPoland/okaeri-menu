package eu.okaeri.menu.bukkit.unit.pane;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.okaeri.menu.pane.PaginatedPane.pane;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaginatedPane.
 * Tests pagination, rendering, static items, and slot handling.
 */
class PaginatedPaneTest {

    private static ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private Player player;
    private Menu menu;
    private Inventory inventory;

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
        this.player = server.addPlayer();
        this.menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(6)
            .build();
        this.inventory = server.createInventory(this.menu, 54, "Test");

        // Open menu to create ViewerState (required for pagination)
        this.menu.open(this.player);
    }

    @Test
    @DisplayName("Should require name in builder")
    void testBuilderRequiresName() {
        assertThatThrownBy(() -> pane(String.class)
            .bounds(0, 0, 9, 5)
            .items(List.of("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("name is required");
    }

    @Test
    @DisplayName("Should require bounds in builder")
    void testBuilderRequiresBounds() {
        assertThatThrownBy(() -> pane(String.class)
            .name("test")
            .items(List.of("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bounds are required");
    }

    @Test
    @DisplayName("Should require items supplier in builder")
    void testBuilderRequiresItemsSupplier() {
        assertThatThrownBy(() -> pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 5)
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("supplier is required");
    }

    @Test
    @DisplayName("Should require item renderer in builder")
    void testBuilderRequiresRenderer() {
        assertThatThrownBy(() -> pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 5)
            .items(List.of("A", "B"))
            .build())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("renderer is required");
    }

    @Test
    @DisplayName("Should default itemsPerPage to pane size")
    void testDefaultItemsPerPage() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 5)  // 9 * 5 = 45 slots
            .items(List.of("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        assertThat(pane.getItemsPerPage()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should respect custom itemsPerPage")
    void testCustomItemsPerPage() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 5)
            .items(List.of("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .itemsPerPage(10)
            .build();

        assertThat(pane.getItemsPerPage()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should adjust itemsPerPage for static items")
    void testItemsPerPageWithStaticItems() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 5)  // 45 slots
            .items(List.of("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .staticItem(8, 4, MenuItem.item().material(Material.BARRIER).build())  // 1 static item
            .build();

        assertThat(pane.getItemsPerPage()).isEqualTo(44);  // 45 - 1 = 44
    }

    @Test
    @DisplayName("Should render items for current page")
    void testRenderCurrentPage() {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)  // 1 row = 9 slots
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .itemsPerPage(5)  // 5 items per page
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should render first 5 items (A-E) on page 0
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.inventory.getItem(i);
            assertThat(stack).isNotNull();
            assertThat(stack.getType()).isEqualTo(Material.STONE);
        }

        // Slots 5-8 should be empty
        for (int i = 5; i < 9; i++) {
            assertThat(this.inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should render second page after navigation")
    void testRenderSecondPage() {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .itemsPerPage(4)
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);

        // Open menu to create ViewerState
        this.menu.open(this.player);

        // Navigate to page 1
        PaginationContext<String> pagination = PaginationContext.get(context, pane);
        pagination.nextPage();

        pane.render(this.inventory, context);

        // Should render items E-H (indices 4-7)
        assertThat(this.inventory.getItem(0)).isNotNull();
        assertThat(this.inventory.getItem(1)).isNotNull();
        assertThat(this.inventory.getItem(2)).isNotNull();
        assertThat(this.inventory.getItem(3)).isNotNull();
    }

    @Test
    @DisplayName("Should clear pane area before rendering")
    void testClearBeforeRendering() {
        List<String> items = Arrays.asList("A", "B");

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .build())
            .itemsPerPage(5)
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);

        // Pre-fill inventory
        for (int i = 0; i < 9; i++) {
            this.inventory.setItem(i, new ItemStack(Material.DIAMOND));
        }

        pane.render(this.inventory, context);

        // Items 0-1 should be rendered (A, B)
        assertThat(this.inventory.getItem(0)).isNotNull();
        assertThat(this.inventory.getItem(1)).isNotNull();

        // Items 2-8 should be cleared to null
        for (int i = 2; i < 9; i++) {
            assertThat(this.inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should render static items")
    void testRenderStaticItems() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B", "C"))
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .build())
            .staticItem(8, 0, MenuItem.item()
                .material(Material.BARRIER)
                .name("Static Button")
                .build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Slot 8 should have static barrier
        ItemStack staticItem = this.inventory.getItem(8);
        assertThat(staticItem).isNotNull();
        assertThat(staticItem.getType()).isEqualTo(Material.BARRIER);

        // Other slots should have stone
        assertThat(this.inventory.getItem(0).getType()).isEqualTo(Material.STONE);
    }

    @Test
    @DisplayName("Should skip positions occupied by static items")
    void testStaticItemsSkipPositions() {
        List<String> items = Arrays.asList("A", "B", "C", "D", "E");

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .amount(index + 1)  // Use amount to track index
                .build())
            .staticItem(2, 0, MenuItem.item()  // Block position 2
                .material(Material.BARRIER)
                .build())
            .itemsPerPage(5)
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Position 2 should have barrier
        assertThat(this.inventory.getItem(2).getType()).isEqualTo(Material.BARRIER);

        // Positions 0, 1, 3, 4 should have stone (but items may shift)
        assertThat(this.inventory.getItem(0).getType()).isEqualTo(Material.STONE);
        assertThat(this.inventory.getItem(1).getType()).isEqualTo(Material.STONE);
        assertThat(this.inventory.getItem(3).getType()).isEqualTo(Material.STONE);
        assertThat(this.inventory.getItem(4).getType()).isEqualTo(Material.STONE);
    }

    @Test
    @DisplayName("Should render ALL paginated items around static items without losing any")
    void testStaticItemsDontLosePageItems() {
        // Item list where each item has a unique identifier
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);  // Will use amount to identify

        PaginatedPane<Integer> pane = pane(Integer.class)
            .name("test")
            .bounds(0, 0, 9, 1)  // 9 slots wide
            .items(items)
            .renderer((ctx, itemId, index) -> MenuItem.item()
                .material(Material.STONE)
                .amount(itemId)  // Use amount to identify which item (1,2,3,4,5)
                .build())
            .staticItem(2, 0, MenuItem.item()  // Static at position 2
                .material(Material.BARRIER)
                .build())
            .itemsPerPage(5)
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Verify static item at position 2
        assertThat(this.inventory.getItem(2))
            .as("Position 2 should have static barrier")
            .isNotNull()
            .extracting(ItemStack::getType)
            .isEqualTo(Material.BARRIER);

        // CRITICAL: Verify ALL 5 paginated items render (1,2,3,4,5)
        // Items should flow around the static item:
        // Expected: [1, 2, STATIC, 3, 4, 5, ...]
        assertThat(this.inventory.getItem(0))
            .as("Slot 0 should have item 1")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(1);

        assertThat(this.inventory.getItem(1))
            .as("Slot 1 should have item 2")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(2);

        // Slot 2 is static (already verified above)

        assertThat(this.inventory.getItem(3))
            .as("Slot 3 should have item 3 (not item 4!) - Bug: currently skips item 3")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(3);

        assertThat(this.inventory.getItem(4))
            .as("Slot 4 should have item 4")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(4);

        assertThat(this.inventory.getItem(5))
            .as("Slot 5 should have item 5")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(5);
    }

    @Test
    @DisplayName("Should calculate correct total pages with static items")
    void testTotalPagesWithStaticItems() {
        // Scenario: 9 items, 9x1 pane (9 slots), 2 static items
        // Available slots: 9 - 2 = 7
        // Expected: ceil(9 / 7) = 2 pages
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        PaginatedPane<Integer> pane = pane(Integer.class)
            .name("test")
            .bounds(0, 0, 9, 1)  // 9 slots
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .amount(item)
                .build())
            .staticItem(0, 0, MenuItem.item().material(Material.BARRIER).build())
            .staticItem(8, 0, MenuItem.item().material(Material.BARRIER).build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);

        // Get pagination context to check total pages
        PaginationContext<Integer> pagination = PaginationContext.get(context, pane);

        // Verify items per page calculation
        assertThat(pane.getItemsPerPage())
            .as("Items per page should be pane size - static items: 9 - 2 = 7")
            .isEqualTo(7);

        // Verify total pages calculation
        assertThat(pagination.getTotalPages())
            .as("Should calculate 2 pages: ceil(9 items / 7 per page)")
            .isEqualTo(2);
    }

    @Test
    @DisplayName("Should distribute items correctly across multiple pages with static items")
    void testMultiPagePaginationWithStaticItems() {
        // Scenario: 9 items, 9x1 pane, 2 static items at positions 0 and 8
        // Page 0 should show: STATIC, 1, 2, 3, 4, 5, 6, 7, STATIC (7 data items)
        // Page 1 should show: STATIC, 8, 9, empty..., STATIC (2 data items)
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        PaginatedPane<Integer> pane = pane(Integer.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .amount(item)
                .build())
            .staticItem(0, 0, MenuItem.item().material(Material.BARRIER).build())
            .staticItem(8, 0, MenuItem.item().material(Material.BARRIER).build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        PaginationContext<Integer> pagination = PaginationContext.get(context, pane);

        // === PAGE 0 ===
        pagination.setPage(0);
        pane.render(this.inventory, context);

        // Verify static items
        assertThat(this.inventory.getItem(0).getType())
            .as("Slot 0 should have static barrier on page 0")
            .isEqualTo(Material.BARRIER);
        assertThat(this.inventory.getItem(8).getType())
            .as("Slot 8 should have static barrier on page 0")
            .isEqualTo(Material.BARRIER);

        // Verify page 0 has items 1-7
        assertThat(this.inventory.getItem(1).getAmount()).isEqualTo(1);
        assertThat(this.inventory.getItem(2).getAmount()).isEqualTo(2);
        assertThat(this.inventory.getItem(3).getAmount()).isEqualTo(3);
        assertThat(this.inventory.getItem(4).getAmount()).isEqualTo(4);
        assertThat(this.inventory.getItem(5).getAmount()).isEqualTo(5);
        assertThat(this.inventory.getItem(6).getAmount()).isEqualTo(6);
        assertThat(this.inventory.getItem(7).getAmount()).isEqualTo(7);

        // === PAGE 1 ===
        pagination.nextPage();
        pane.render(this.inventory, context);

        // Verify static items still present
        assertThat(this.inventory.getItem(0).getType())
            .as("Slot 0 should have static barrier on page 1")
            .isEqualTo(Material.BARRIER);
        assertThat(this.inventory.getItem(8).getType())
            .as("Slot 8 should have static barrier on page 1")
            .isEqualTo(Material.BARRIER);

        // Verify page 1 has items 8-9
        assertThat(this.inventory.getItem(1))
            .as("Slot 1 on page 1 should have item 8")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(8);

        assertThat(this.inventory.getItem(2))
            .as("Slot 2 on page 1 should have item 9")
            .isNotNull()
            .extracting(ItemStack::getAmount)
            .isEqualTo(9);

        // Verify remaining slots are empty (cleared)
        assertThat(this.inventory.getItem(3))
            .as("Slot 3 on page 1 should be empty (no more items)")
            .isNull();

        // CRITICAL: Verify all 9 items are accessible across both pages
        pagination.setPage(0);
        List<Integer> page0Items = pagination.getCurrentPageItems();
        pagination.setPage(1);
        List<Integer> page1Items = pagination.getCurrentPageItems();

        assertThat(page0Items.size()).isEqualTo(7);
        assertThat(page1Items.size()).isEqualTo(2);
        assertThat(page0Items.size() + page1Items.size())
            .as("All 9 items should be accessible across both pages")
            .isEqualTo(9);
    }

    @Test
    @DisplayName("Should support dynamic items via supplier")
    void testDynamicItemsSupplier() {
        List<String> dynamicList = new ArrayList<>(Arrays.asList("A", "B"));

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(() -> dynamicList)  // Supplier
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should have 2 items initially
        assertThat(this.inventory.getItem(0)).isNotNull();
        assertThat(this.inventory.getItem(1)).isNotNull();
        assertThat(this.inventory.getItem(2)).isNull();

        // Add more items dynamically
        dynamicList.add("C");
        pane.render(this.inventory, context);

        // Should now have 3 items
        assertThat(this.inventory.getItem(0)).isNotNull();
        assertThat(this.inventory.getItem(1)).isNotNull();
        assertThat(this.inventory.getItem(2)).isNotNull();
    }

    @Test
    @DisplayName("Should call renderer with correct index")
    void testRendererReceivesCorrectIndex() {
        List<Integer> receivedIndices = new ArrayList<>();

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B", "C"))
            .renderer((ctx, item, index) -> {
                receivedIndices.add(index);
                return MenuItem.item().material(Material.STONE).build();
            })
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        assertThat(receivedIndices).containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("Should handle null renderer results")
    void testNullRendererResult() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B", "C"))
            .renderer((ctx, item, index) -> null)  // Returns null
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);

        // Should not throw exception
        assertThatCode(() -> pane.render(this.inventory, context))
            .doesNotThrowAnyException();

        // All slots should be null
        for (int i = 0; i < 9; i++) {
            assertThat(this.inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should handle null item stacks from MenuItem")
    void testNullItemStackFromMenuItem() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.AIR)  // AIR renders as null
                .build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should not crash, slots should be null
        assertThat(this.inventory.getItem(0)).isNull();
        assertThat(this.inventory.getItem(1)).isNull();
    }

    @Test
    @DisplayName("Should respect pane bounds")
    void testPaneBounds() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(2, 1, 5, 2)  // x=2, y=1, width=5, height=2
            .items(Arrays.asList("A", "B", "C", "D", "E"))
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should render starting at slot 9*1 + 2 = 11
        assertThat(this.inventory.getItem(11)).isNotNull();  // First item
        assertThat(this.inventory.getItem(12)).isNotNull();  // Second item

        // Should not render outside bounds
        assertThat(this.inventory.getItem(0)).isNull();
        assertThat(this.inventory.getItem(10)).isNull();
    }

    @Test
    @DisplayName("Should get static item by global slot")
    void testGetItemByGlobalSlot() {
        MenuItem staticItem = MenuItem.item()
            .material(Material.BARRIER)
            .build();

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .staticItem(5, 0, staticItem)  // Slot 5
            .build();

        MenuItem result = pane.getItemByGlobalSlot(5);
        assertThat(result).isSameAs(staticItem);
    }

    @Test
    @DisplayName("Should return null for non-static slots")
    void testGetItemByGlobalSlotNonStatic() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        MenuItem result = pane.getItemByGlobalSlot(0);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null for slots outside bounds")
    void testGetItemByGlobalSlotOutsideBounds() {
        MenuItem staticItem = MenuItem.item().material(Material.BARRIER).build();

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(2, 1, 5, 2)  // Limited bounds
            .items(List.of("A"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .staticItem(0, 0, staticItem)
            .build();

        // Slot 0 (top-left corner of inventory) is outside pane bounds
        MenuItem result = pane.getItemByGlobalSlot(0);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should add static items after construction")
    void testSetStaticItemAfterConstruction() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        MenuItem newStaticItem = MenuItem.item()
            .material(Material.BARRIER)
            .build();

        pane.setStaticItem(5, 0, newStaticItem);

        MenuItem result = pane.getItemByGlobalSlot(5);
        assertThat(result).isSameAs(newStaticItem);
    }

    @Test
    @DisplayName("Should invalidate when setting static item")
    void testInvalidateOnSetStaticItem() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(List.of("A"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Add static item (should invalidate)
        pane.setStaticItem(5, 0, MenuItem.item().material(Material.BARRIER).build());

        // Render should work without issues
        assertThatCode(() -> pane.render(this.inventory, context))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty items list")
    void testEmptyItemsList() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(List.of())  // Empty list
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);

        assertThatCode(() -> pane.render(this.inventory, context))
            .doesNotThrowAnyException();

        // All slots should be null
        for (int i = 0; i < 9; i++) {
            assertThat(this.inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should handle multi-row pagination")
    void testMultiRowPagination() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            items.add("Item-" + i);
        }

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 3)  // 3 rows = 27 slots
            .items(items)
            .renderer((ctx, item, index) -> MenuItem.item()
                .material(Material.STONE)
                .name(item)
                .build())
            .itemsPerPage(10)  // 10 items per page
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should render first 10 items
        for (int i = 0; i < 10; i++) {
            assertThat(this.inventory.getItem(i)).isNotNull();
        }

        // Remaining slots in pane should be null
        for (int i = 10; i < 27; i++) {
            assertThat(this.inventory.getItem(i)).isNull();
        }
    }

    @Test
    @DisplayName("Should call renderer only for current page items")
    void testRendererCallCount() {
        AtomicInteger callCount = new AtomicInteger(0);

        List<String> items = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(items)
            .renderer((ctx, item, index) -> {
                callCount.incrementAndGet();
                return MenuItem.item().material(Material.STONE).build();
            })
            .itemsPerPage(3)  // 3 items per page
            .build();

        MenuContext context = new MenuContext(this.menu, this.player);
        pane.render(this.inventory, context);

        // Should call renderer exactly 3 times (for page 0 items)
        assertThat(callCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle invalidation correctly")
    void testInvalidation() {
        PaginatedPane<String> pane = pane(String.class)
            .name("test")
            .bounds(0, 0, 9, 1)
            .items(Arrays.asList("A", "B"))
            .renderer((ctx, item, index) -> MenuItem.item().material(Material.STONE).build())
            .build();

        // Should not throw when rendering after invalidation
        MenuContext context = new MenuContext(this.menu, this.player);
        context.getViewerState().invalidateProps();
        assertThatCode(() -> pane.render(this.inventory, context))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Exception in filter value extraction should clear pane but not crash menu")
    void testFilterExtractionErrorHandling() {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Error Test Menu")
            .rows(4)
            .pane(StaticPane.staticPane()
                .name("failingFilters")
                .bounds(0, 0, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.BARRIER)
                    .filter(ItemFilter.builder()
                        .target("failingPane")
                        .id("broken")
                        .value(() -> {
                            throw new RuntimeException("Filter value extraction failed!");
                        })
                        .build())
                    .build())
                .build())
            .pane(pane(String.class)
                .name("failingPane")
                .bounds(0, 1, 9, 1)
                .items(Arrays.asList("Should", "Not", "Appear"))
                .renderer((ctx, item, index) -> MenuItem.item()
                    .material(Material.DIAMOND)
                    .build())
                .build())
            .pane(StaticPane.staticPane()
                .name("workingPane")
                .bounds(0, 2, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.EMERALD)
                    .name("Working Item")
                    .build())
                .build())
            .build();

        // Open menu - should not throw exception
        testMenu.open(this.player);

        Inventory inventory = this.player.getOpenInventory().getTopInventory();

        // Verify failingPane was cleared (slots 9-17, all should be null)
        for (int slot = 9; slot < 18; slot++) {
            assertThat(inventory.getItem(slot))
                .as("Slot %d in failingPane should be cleared after error", slot)
                .isNull();
        }

        // Verify workingPane still rendered (slot 18 should have the emerald)
        assertThat(inventory.getItem(18))
            .as("Slot 18 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(18).getType())
            .isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Exception in item renderer should clear pane but not crash menu")
    void testRendererErrorHandling() {
        Menu testMenu = Menu.builder(this.plugin)
            .title("Renderer Error Test")
            .rows(3)
            .pane(pane(String.class)
                .name("failingPane")
                .bounds(0, 0, 9, 1)
                .items(Arrays.asList("A", "B", "C"))
                .renderer((ctx, item, index) -> {
                    throw new RuntimeException("Renderer failed!");
                })
                .build())
            .pane(StaticPane.staticPane()
                .name("workingPane")
                .bounds(0, 1, 9, 1)
                .item(0, 0, MenuItem.item()
                    .material(Material.GOLD_INGOT)
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

        // Verify workingPane still rendered (slot 9 should have the gold ingot)
        assertThat(inventory.getItem(9))
            .as("Slot 9 in workingPane should have item despite other pane failing")
            .isNotNull();
        assertThat(inventory.getItem(9).getType())
            .isEqualTo(Material.GOLD_INGOT);
    }
}
