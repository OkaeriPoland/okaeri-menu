package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.item.MenuItemChangeContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static eu.okaeri.menu.item.MenuItem.item;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for MenuItem interactive slots.
 * Tests allowPickup, allowPlacement, and onItemChange handlers.
 */
class InteractiveSlotTest {

    private static ServerMock server;
    private JavaPlugin plugin;
    private Player player;
    private Menu menu;

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

        // Create a test menu
        this.menu = Menu.builder(this.plugin)
            .title("Test Menu")
            .rows(3)
            .build();
    }

    private MenuItemChangeContext createChangeContext(int slot, ItemStack before, ItemStack after) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        return new MenuItemChangeContext(this.menu, this.player, event, slot, before, after);
    }

    // ========================================
    // INTERACTIVE SLOT PROPERTIES
    // ========================================

    @Test
    @DisplayName("Should create non-interactive item by default")
    void testDefaultNonInteractive() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .build();

        assertThat(item.isInteractive()).isFalse();
        assertThat(item.isAllowPickup()).isFalse();
        assertThat(item.isAllowPlacement()).isFalse();
    }

    @Test
    @DisplayName("Should create interactive item with allowPickup")
    void testAllowPickup() {
        MenuItem item = item()
            .allowPickup(true)
            .build();

        assertThat(item.isInteractive()).isTrue();
        assertThat(item.isAllowPickup()).isTrue();
        assertThat(item.isAllowPlacement()).isFalse();
    }

    @Test
    @DisplayName("Should create interactive item with allowPlacement")
    void testAllowPlacement() {
        MenuItem item = item()
            .allowPlacement(true)
            .build();

        assertThat(item.isInteractive()).isTrue();
        assertThat(item.isAllowPickup()).isFalse();
        assertThat(item.isAllowPlacement()).isTrue();
    }

    @Test
    @DisplayName("Should create fully interactive item with interactive()")
    void testInteractiveShorthand() {
        MenuItem item = item()
            .interactive()
            .build();

        assertThat(item.isInteractive()).isTrue();
        assertThat(item.isAllowPickup()).isTrue();
        assertThat(item.isAllowPlacement()).isTrue();
    }

    @Test
    @DisplayName("Should create interactive item with both flags")
    void testBothFlags() {
        MenuItem item = item()
            .allowPickup(true)
            .allowPlacement(true)
            .build();

        assertThat(item.isInteractive()).isTrue();
        assertThat(item.isAllowPickup()).isTrue();
        assertThat(item.isAllowPlacement()).isTrue();
    }

    // ========================================
    // SHOULD RENDER
    // ========================================

    @Test
    @DisplayName("Non-interactive items should render")
    void testNonInteractiveShouldRender() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .build();

        assertThat(item.shouldRender()).isTrue();
    }

    @Test
    @DisplayName("Interactive items should not render")
    void testInteractiveShouldNotRender() {
        MenuItem item = item()
            .allowPickup(true)
            .build();

        assertThat(item.shouldRender()).isFalse();
    }

    // ========================================
    // ITEM CHANGE HANDLER
    // ========================================

    @Test
    @DisplayName("Should execute onItemChange handler")
    void testOnItemChangeHandler() {
        AtomicBoolean changed = new AtomicBoolean(false);

        MenuItem item = item()
            .allowPickup(true)
            .onItemChange(ctx -> changed.set(true))
            .build();

        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            null,
            new ItemStack(Material.DIAMOND)
        );

        item.handleItemChange(changeContext);

        assertThat(changed.get()).isTrue();
    }

    @Test
    @DisplayName("Should not throw if onItemChange handler not set")
    void testNoItemChangeHandler() {
        MenuItem item = item()
            .allowPlacement(true)
            .build();

        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            null,
            new ItemStack(Material.EMERALD)
        );

        assertThatCode(() -> item.handleItemChange(changeContext))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should provide access to before/after items in handler")
    void testItemChangeContextAccess() {
        AtomicReference<ItemStack> capturedBefore = new AtomicReference<>();
        AtomicReference<ItemStack> capturedAfter = new AtomicReference<>();

        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> {
                capturedBefore.set(ctx.getPreviousItem());
                capturedAfter.set(ctx.getNewItem());
            })
            .build();

        ItemStack before = new ItemStack(Material.GOLD_INGOT);
        ItemStack after = new ItemStack(Material.DIAMOND);

        MenuItemChangeContext changeContext = this.createChangeContext(0, before, after);

        item.handleItemChange(changeContext);

        assertThat(capturedBefore.get()).isEqualTo(before);
        assertThat(capturedAfter.get()).isEqualTo(after);
    }

    // ========================================
    // CHANGE DETECTION
    // ========================================

    @Test
    @DisplayName("Should detect item placed")
    void testItemPlaced() {
        AtomicBoolean wasPlaced = new AtomicBoolean(false);

        MenuItem item = item()
            .allowPlacement(true)
            .onItemChange(ctx -> {
                if (ctx.wasItemPlaced()) {
                    wasPlaced.set(true);
                }
            })
            .build();

        // null -> item = placed
        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            null,
            new ItemStack(Material.STONE)
        );

        item.handleItemChange(changeContext);

        assertThat(wasPlaced.get()).isTrue();
    }

    @Test
    @DisplayName("Should detect item removed")
    void testItemRemoved() {
        AtomicBoolean wasRemoved = new AtomicBoolean(false);

        MenuItem item = item()
            .allowPickup(true)
            .onItemChange(ctx -> {
                if (ctx.wasItemRemoved()) {
                    wasRemoved.set(true);
                }
            })
            .build();

        // item -> null = removed
        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            new ItemStack(Material.IRON_INGOT),
            null
        );

        item.handleItemChange(changeContext);

        assertThat(wasRemoved.get()).isTrue();
    }

    @Test
    @DisplayName("Should detect item swapped")
    void testItemSwapped() {
        AtomicBoolean wasSwapped = new AtomicBoolean(false);

        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> {
                if (ctx.wasItemSwapped()) {
                    wasSwapped.set(true);
                }
            })
            .build();

        // item -> different item = swapped
        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            new ItemStack(Material.GOLD_INGOT),
            new ItemStack(Material.DIAMOND)
        );

        item.handleItemChange(changeContext);

        assertThat(wasSwapped.get()).isTrue();
    }

    @Test
    @DisplayName("Should detect all change types correctly")
    void testAllChangeTypes() {
        AtomicInteger placedCount = new AtomicInteger(0);
        AtomicInteger removedCount = new AtomicInteger(0);
        AtomicInteger swappedCount = new AtomicInteger(0);

        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> {
                if (ctx.wasItemPlaced()) placedCount.incrementAndGet();
                if (ctx.wasItemRemoved()) removedCount.incrementAndGet();
                if (ctx.wasItemSwapped()) swappedCount.incrementAndGet();
            })
            .build();

        // Test placed
        item.handleItemChange(this.createChangeContext(0, null, new ItemStack(Material.STONE)));
        assertThat(placedCount.get()).isEqualTo(1);
        assertThat(removedCount.get()).isEqualTo(0);
        assertThat(swappedCount.get()).isEqualTo(0);

        // Test removed
        placedCount.set(0);
        item.handleItemChange(this.createChangeContext(0, new ItemStack(Material.STONE), null));
        assertThat(placedCount.get()).isEqualTo(0);
        assertThat(removedCount.get()).isEqualTo(1);
        assertThat(swappedCount.get()).isEqualTo(0);

        // Test swapped
        removedCount.set(0);
        item.handleItemChange(this.createChangeContext(0, new ItemStack(Material.STONE), new ItemStack(Material.DIAMOND)));
        assertThat(placedCount.get()).isEqualTo(0);
        assertThat(removedCount.get()).isEqualTo(0);
        assertThat(swappedCount.get()).isEqualTo(1);
    }

    // ========================================
    // HANDLER EXECUTION
    // ========================================

    @Test
    @DisplayName("Should execute handler multiple times")
    void testMultipleExecutions() {
        AtomicInteger executionCount = new AtomicInteger(0);

        MenuItem item = item()
            .allowPickup(true)
            .onItemChange(ctx -> executionCount.incrementAndGet())
            .build();

        // Execute 3 times
        for (int i = 0; i < 3; i++) {
            item.handleItemChange(this.createChangeContext(0, null, new ItemStack(Material.DIAMOND)));
        }

        assertThat(executionCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should provide access to slot in handler")
    void testSlotAccess() {
        AtomicInteger capturedSlot = new AtomicInteger(-1);

        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> capturedSlot.set(ctx.getSlot()))
            .build();

        MenuItemChangeContext changeContext = this.createChangeContext(
            42,
            null,
            new ItemStack(Material.EMERALD)
        );

        item.handleItemChange(changeContext);

        assertThat(capturedSlot.get()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should provide access to menu in handler")
    void testMenuAccess() {
        AtomicReference<Menu> capturedMenu = new AtomicReference<>();

        MenuItem item = item()
            .allowPlacement(true)
            .onItemChange(ctx -> capturedMenu.set(ctx.getMenu()))
            .build();

        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            null,
            new ItemStack(Material.GOLD_BLOCK)
        );

        item.handleItemChange(changeContext);

        assertThat(capturedMenu.get()).isEqualTo(this.menu);
    }

    // ========================================
    // VALIDATION
    // ========================================

    @Test
    @DisplayName("Should reject interactive item with material")
    void testRejectInteractiveWithMaterial() {
        assertThatThrownBy(() ->
            item()
                .material(Material.DIAMOND)
                .allowPickup(true)
                .build()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Interactive items")
            .hasMessageContaining("should not have a material()");
    }

    @Test
    @DisplayName("Should reject interactive item with name")
    void testRejectInteractiveWithName() {
        assertThatThrownBy(() ->
            item()
                .name("Test Item")
                .allowPlacement(true)
                .build()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Interactive items")
            .hasMessageContaining("should not have a name()");
    }

    @Test
    @DisplayName("Should reject interactive item with lore")
    void testRejectInteractiveWithLore() {
        assertThatThrownBy(() ->
            item()
                .lore("Line 1\nLine 2")
                .interactive()
                .build()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Interactive items")
            .hasMessageContaining("should not have lore()");
    }

    @Test
    @DisplayName("Should allow interactive item with AIR material")
    void testAllowInteractiveWithAir() {
        MenuItem item = item()
            .material(Material.AIR)
            .allowPickup(true)
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    @Test
    @DisplayName("Should allow interactive item without display properties")
    void testAllowInteractiveWithoutDisplay() {
        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> {
            })
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    // ========================================
    // COMPLEX SCENARIOS
    // ========================================

    @Test
    @DisplayName("Should handle complex item change logic")
    void testComplexItemChangeLogic() {
        StringBuilder log = new StringBuilder();

        MenuItem item = item()
            .interactive()
            .onItemChange(ctx -> {
                if (ctx.wasItemPlaced()) {
                    Material mat = ctx.getNewItem().getType();
                    log.append("Placed:").append(mat).append(";");
                } else if (ctx.wasItemRemoved()) {
                    Material mat = ctx.getPreviousItem().getType();
                    log.append("Removed:").append(mat).append(";");
                } else if (ctx.wasItemSwapped()) {
                    Material before = ctx.getPreviousItem().getType();
                    Material after = ctx.getNewItem().getType();
                    log.append("Swapped:").append(before).append("->").append(after).append(";");
                }
            })
            .build();

        // Test sequence
        item.handleItemChange(this.createChangeContext(0, null, new ItemStack(Material.DIAMOND)));
        item.handleItemChange(this.createChangeContext(0, new ItemStack(Material.DIAMOND), new ItemStack(Material.EMERALD)));
        item.handleItemChange(this.createChangeContext(0, new ItemStack(Material.EMERALD), null));

        assertThat(log.toString()).isEqualTo("Placed:DIAMOND;Swapped:DIAMOND->EMERALD;Removed:EMERALD;");
    }

    @Test
    @DisplayName("Should allow both click and change handlers")
    void testClickAndChangeHandlers() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        AtomicBoolean changed = new AtomicBoolean(false);

        MenuItem item = item()
            .interactive()
            .onClick(event -> clicked.set(true))
            .onItemChange(event -> changed.set(true))
            .build();

        // Test change handler
        MenuItemChangeContext changeContext = this.createChangeContext(
            0,
            null,
            new ItemStack(Material.GOLD_INGOT)
        );
        item.handleItemChange(changeContext);

        assertThat(clicked.get()).isFalse();
        assertThat(changed.get()).isTrue();
    }
}
