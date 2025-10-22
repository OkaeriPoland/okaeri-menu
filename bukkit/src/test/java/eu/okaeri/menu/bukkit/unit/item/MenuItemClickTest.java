package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.item.MenuItemClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for MenuItem click handlers.
 * Tests onClick, onLeftClick, onRightClick handlers.
 */
class MenuItemClickTest {

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

    private MenuItemClickContext createClickContext(int slot, ClickType clickType) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        return new MenuItemClickContext(this.menu, this.player, event, slot, clickType);
    }

    // ========================================
    // GENERAL CLICK HANDLER
    // ========================================

    @Test
    @DisplayName("Should execute onClick handler for any click")
    void testOnClickHandler() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .onClick(ctx -> clicked.set(true))
            .build();

        // Create a left click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(clicked.get()).isTrue();
    }

    @Test
    @DisplayName("Should execute onClick handler for right click")
    void testOnClickHandlerRightClick() {
        AtomicBoolean clicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.EMERALD)
            .onClick(ctx -> clicked.set(true))
            .build();

        // Create a right click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(clicked.get()).isTrue();
    }

    @Test
    @DisplayName("Should not throw if onClick handler not set")
    void testNoClickHandler() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        assertThatCode(() -> item.handleClick(clickContext))
            .doesNotThrowAnyException();
    }

    // ========================================
    // LEFT CLICK HANDLER
    // ========================================

    @Test
    @DisplayName("Should execute onLeftClick handler for left click")
    void testOnLeftClickHandler() {
        AtomicBoolean leftClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.IRON_SWORD)
            .onLeftClick(ctx -> leftClicked.set(true))
            .build();

        // Create a left click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(leftClicked.get()).isTrue();
    }

    @Test
    @DisplayName("Should not execute onLeftClick handler for right click")
    void testOnLeftClickHandlerRightClick() {
        AtomicBoolean leftClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.GOLDEN_SWORD)
            .onLeftClick(ctx -> leftClicked.set(true))
            .build();

        // Create a right click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(leftClicked.get()).isFalse();
    }

    // ========================================
    // RIGHT CLICK HANDLER
    // ========================================

    @Test
    @DisplayName("Should execute onRightClick handler for right click")
    void testOnRightClickHandler() {
        AtomicBoolean rightClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.BOW)
            .onRightClick(ctx -> rightClicked.set(true))
            .build();

        // Create a right click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(rightClicked.get()).isTrue();
    }

    @Test
    @DisplayName("Should not execute onRightClick handler for left click")
    void testOnRightClickHandlerLeftClick() {
        AtomicBoolean rightClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.CROSSBOW)
            .onRightClick(ctx -> rightClicked.set(true))
            .build();

        // Create a left click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(rightClicked.get()).isFalse();
    }

    // ========================================
    // MULTIPLE HANDLERS
    // ========================================

    @Test
    @DisplayName("Should execute both onClick and onLeftClick for left click")
    void testMultipleHandlersLeftClick() {
        AtomicInteger clickCount = new AtomicInteger(0);
        AtomicInteger leftClickCount = new AtomicInteger(0);

        MenuItem item = MenuItem.item()
            .material(Material.REDSTONE)
            .onClick(ctx -> clickCount.incrementAndGet())
            .onLeftClick(ctx -> leftClickCount.incrementAndGet())
            .build();

        // Create a left click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(clickCount.get()).isEqualTo(1);
        assertThat(leftClickCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute both onClick and onRightClick for right click")
    void testMultipleHandlersRightClick() {
        AtomicInteger clickCount = new AtomicInteger(0);
        AtomicInteger rightClickCount = new AtomicInteger(0);

        MenuItem item = MenuItem.item()
            .material(Material.GLOWSTONE)
            .onClick(ctx -> clickCount.incrementAndGet())
            .onRightClick(ctx -> rightClickCount.incrementAndGet())
            .build();

        // Create a right click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(clickCount.get()).isEqualTo(1);
        assertThat(rightClickCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute all three handlers with correct counts")
    void testAllThreeHandlers() {
        AtomicInteger clickCount = new AtomicInteger(0);
        AtomicInteger leftClickCount = new AtomicInteger(0);
        AtomicInteger rightClickCount = new AtomicInteger(0);

        MenuItem item = MenuItem.item()
            .material(Material.BEACON)
            .onClick(ctx -> clickCount.incrementAndGet())
            .onLeftClick(ctx -> leftClickCount.incrementAndGet())
            .onRightClick(ctx -> rightClickCount.incrementAndGet())
            .build();

        // Test left click
        MenuItemClickContext leftClick = this.createClickContext(0, ClickType.LEFT);
        item.handleClick(leftClick);

        assertThat(clickCount.get()).isEqualTo(1);
        assertThat(leftClickCount.get()).isEqualTo(1);
        assertThat(rightClickCount.get()).isEqualTo(0);

        // Test right click
        MenuItemClickContext rightClick = this.createClickContext(0, ClickType.RIGHT);
        item.handleClick(rightClick);

        assertThat(clickCount.get()).isEqualTo(2);
        assertThat(leftClickCount.get()).isEqualTo(1);
        assertThat(rightClickCount.get()).isEqualTo(1);
    }

    // ========================================
    // CLICK CONTEXT ACCESS
    // ========================================

    @Test
    @DisplayName("Should provide access to click context in handler")
    void testClickContextAccess() {
        AtomicInteger capturedSlot = new AtomicInteger(-1);

        MenuItem item = MenuItem.item()
            .material(Material.CHEST)
            .onClick(ctx -> capturedSlot.set(ctx.getSlot()))
            .build();

        MenuItemClickContext clickContext = this.createClickContext(15, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(capturedSlot.get()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should provide access to menu context in handler")
    void testMenuContextAccess() {
        AtomicBoolean playerMatches = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.ENDER_PEARL)
            .onClick(ctx -> {
                playerMatches.set(ctx.getEntity().equals(this.player));
            })
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(playerMatches.get()).isTrue();
    }

    @Test
    @DisplayName("Should provide access to click type in handler")
    void testClickTypeAccess() {
        AtomicBoolean isLeftClick = new AtomicBoolean(false);
        AtomicBoolean isRightClick = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.COMPASS)
            .onClick(ctx -> {
                isLeftClick.set(ctx.isLeftClick());
                isRightClick.set(ctx.isRightClick());
            })
            .build();

        // Test left click
        MenuItemClickContext leftClick = this.createClickContext(0, ClickType.LEFT);
        item.handleClick(leftClick);

        assertThat(isLeftClick.get()).isTrue();
        assertThat(isRightClick.get()).isFalse();

        // Reset and test right click
        isLeftClick.set(false);
        isRightClick.set(false);

        MenuItemClickContext rightClick = this.createClickContext(0, ClickType.RIGHT);
        item.handleClick(rightClick);

        assertThat(isLeftClick.get()).isFalse();
        assertThat(isRightClick.get()).isTrue();
    }

    // ========================================
    // HANDLER EXECUTION ORDER
    // ========================================

    @Test
    @DisplayName("Should execute onClick before onLeftClick")
    void testHandlerExecutionOrder() {
        StringBuilder executionOrder = new StringBuilder();

        MenuItem item = MenuItem.item()
            .material(Material.PAPER)
            .onClick(ctx -> executionOrder.append("1"))
            .onLeftClick(ctx -> executionOrder.append("2"))
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(executionOrder.toString()).isEqualTo("12");
    }

    @Test
    @DisplayName("Should execute onClick before onRightClick")
    void testHandlerExecutionOrderRightClick() {
        StringBuilder executionOrder = new StringBuilder();

        MenuItem item = MenuItem.item()
            .material(Material.BOOK)
            .onClick(ctx -> executionOrder.append("1"))
            .onRightClick(ctx -> executionOrder.append("2"))
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(executionOrder.toString()).isEqualTo("12");
    }

    // ========================================
    // MIDDLE CLICK HANDLER
    // ========================================

    @Test
    @DisplayName("Should execute onMiddleClick handler for middle click")
    void testOnMiddleClickHandler() {
        AtomicBoolean middleClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .onMiddleClick(ctx -> middleClicked.set(true))
            .build();

        // Create a middle click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.MIDDLE);

        item.handleClick(clickContext);

        assertThat(middleClicked.get()).isTrue();
    }

    @Test
    @DisplayName("Should not execute onMiddleClick handler for left click")
    void testOnMiddleClickHandlerNotCalledForLeftClick() {
        AtomicBoolean middleClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.IRON_SWORD)
            .onMiddleClick(ctx -> middleClicked.set(true))
            .build();

        // Create a left click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.LEFT);

        item.handleClick(clickContext);

        assertThat(middleClicked.get()).isFalse();
    }

    @Test
    @DisplayName("Should not execute onMiddleClick handler for right click")
    void testOnMiddleClickHandlerNotCalledForRightClick() {
        AtomicBoolean middleClicked = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.GOLDEN_SWORD)
            .onMiddleClick(ctx -> middleClicked.set(true))
            .build();

        // Create a right click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.RIGHT);

        item.handleClick(clickContext);

        assertThat(middleClicked.get()).isFalse();
    }

    @Test
    @DisplayName("Should execute both onClick and onMiddleClick for middle click")
    void testMultipleHandlersMiddleClick() {
        AtomicInteger clickCount = new AtomicInteger(0);
        AtomicInteger middleClickCount = new AtomicInteger(0);

        MenuItem item = MenuItem.item()
            .material(Material.REDSTONE)
            .onClick(ctx -> clickCount.incrementAndGet())
            .onMiddleClick(ctx -> middleClickCount.incrementAndGet())
            .build();

        // Create a middle click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.MIDDLE);

        item.handleClick(clickContext);

        assertThat(clickCount.get()).isEqualTo(1);
        assertThat(middleClickCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should execute onClick and onMiddleClick but not left/right handlers")
    void testMiddleClickDoesNotTriggerLeftRight() {
        AtomicInteger clickCount = new AtomicInteger(0);
        AtomicInteger middleClickCount = new AtomicInteger(0);
        AtomicInteger leftClickCount = new AtomicInteger(0);
        AtomicInteger rightClickCount = new AtomicInteger(0);

        MenuItem item = MenuItem.item()
            .material(Material.BEACON)
            .onClick(ctx -> clickCount.incrementAndGet())
            .onMiddleClick(ctx -> middleClickCount.incrementAndGet())
            .onLeftClick(ctx -> leftClickCount.incrementAndGet())
            .onRightClick(ctx -> rightClickCount.incrementAndGet())
            .build();

        // Create a middle click
        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.MIDDLE);

        item.handleClick(clickContext);

        assertThat(clickCount.get()).isEqualTo(1);
        assertThat(middleClickCount.get()).isEqualTo(1);
        assertThat(leftClickCount.get()).isEqualTo(0);
        assertThat(rightClickCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should execute handlers in correct order: onClick -> onMiddleClick")
    void testHandlerExecutionOrderMiddle() {
        StringBuilder executionOrder = new StringBuilder();

        MenuItem item = MenuItem.item()
            .material(Material.PAPER)
            .onClick(ctx -> executionOrder.append("1"))
            .onMiddleClick(ctx -> executionOrder.append("2"))
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.MIDDLE);

        item.handleClick(clickContext);

        assertThat(executionOrder.toString()).isEqualTo("12");
    }

    @Test
    @DisplayName("Should provide access to middle click context in handler")
    void testMiddleClickContextAccess() {
        AtomicBoolean isMiddleClick = new AtomicBoolean(false);

        MenuItem item = MenuItem.item()
            .material(Material.CHEST)
            .onMiddleClick(ctx -> {
                isMiddleClick.set(ctx.isMiddleClick());
            })
            .build();

        MenuItemClickContext clickContext = this.createClickContext(0, ClickType.MIDDLE);

        item.handleClick(clickContext);

        assertThat(isMiddleClick.get()).isTrue();
    }
}
