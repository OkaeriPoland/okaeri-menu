package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MenuItem rendering to ItemStack.
 * Tests material, name, lore, amount, enchantments, flags, and glint.
 */
class MenuItemRenderTest {

    private static ServerMock server;
    private JavaPlugin plugin;
    private Player player;
    private Menu menu;
    private MenuContext context;

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

        // Create MenuContext for rendering
        this.context = new MenuContext(this.menu, this.player);
    }

    // ========================================
    // BASIC RENDERING
    // ========================================

    @Test
    @DisplayName("Should render item with material")
    void testRenderMaterial() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getType()).isEqualTo(Material.DIAMOND);
    }

    @Test
    @DisplayName("Should render item with name")
    void testRenderName() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .name("&cTest Item")
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.hasItemMeta()).isTrue();

        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasDisplayName()).isTrue();
        assertThat(meta.getDisplayName()).isNotEmpty();
    }

    @Test
    @DisplayName("Should render item with lore")
    void testRenderLore() {
        MenuItem item = MenuItem.item()
            .material(Material.IRON_SWORD)
            .lore("&7Line 1\n&7Line 2\n&7Line 3")
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.hasItemMeta()).isTrue();

        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasLore()).isTrue();
        assertThat(meta.getLore()).hasSize(3);
    }

    @Test
    @DisplayName("Should render item with amount")
    void testRenderAmount() {
        MenuItem item = MenuItem.item()
            .material(Material.ARROW)
            .amount(32)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getAmount()).isEqualTo(32);
    }

    @Test
    @DisplayName("Should render item with enchantments")
    void testRenderEnchantments() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND_SWORD)
            .enchant(Enchantment.SHARPNESS, 5)
            .enchant(Enchantment.UNBREAKING, 3)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.hasItemMeta()).isTrue();

        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasEnchants()).isTrue();
        assertThat(meta.hasEnchant(Enchantment.SHARPNESS)).isTrue();
        assertThat(meta.hasEnchant(Enchantment.UNBREAKING)).isTrue();
        assertThat(meta.getEnchantLevel(Enchantment.SHARPNESS)).isEqualTo(5);
        assertThat(meta.getEnchantLevel(Enchantment.UNBREAKING)).isEqualTo(3);
    }

    @Test
    @DisplayName("Should render item with item flags")
    void testRenderItemFlags() {
        MenuItem item = MenuItem.item()
            .material(Material.GOLDEN_SWORD)
            .itemFlag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.hasItemMeta()).isTrue();

        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)).isTrue();
        assertThat(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)).isTrue();
    }

    @Test
    @DisplayName("Should render item with glint")
    void testRenderGlint() {
        MenuItem item = MenuItem.item()
            .material(Material.STICK)
            .glint(true)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.hasItemMeta()).isTrue();

        ItemMeta meta = stack.getItemMeta();
        // Glint is applied via enchantment glint override
        assertThat(meta.hasEnchantmentGlintOverride()).isTrue();
        assertThat(meta.getEnchantmentGlintOverride()).isTrue();
    }

    // ========================================
    // VISIBILITY
    // ========================================

    @Test
    @DisplayName("Should return null for invisible item")
    void testInvisibleItem() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(false)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNull();
    }

    @Test
    @DisplayName("Should render visible item")
    void testVisibleItem() {
        MenuItem item = MenuItem.item()
            .material(Material.EMERALD)
            .visible(true)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    @DisplayName("Should return null for AIR material")
    void testAirMaterial() {
        MenuItem item = MenuItem.item()
            .material(Material.AIR)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNull();
    }

    // ========================================
    // AMOUNT VALIDATION
    // ========================================

    @Test
    @DisplayName("Should clamp amount to minimum 1")
    void testAmountMinimum() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .amount(0)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should clamp amount to maximum 64")
    void testAmountMaximum() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .amount(100)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getAmount()).isEqualTo(64);
    }

    @Test
    @DisplayName("Should clamp negative amount to 1")
    void testNegativeAmount() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .amount(-5)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getAmount()).isEqualTo(1);
    }

    // ========================================
    // COMPLEX RENDERING
    // ========================================

    @Test
    @DisplayName("Should render item with all properties")
    void testFullyConfiguredItem() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND_SWORD)
            .name("&cPowerful Sword")
            .lore("&7A very powerful weapon\n&7Deals massive damage")
            .amount(1)
            .enchant(Enchantment.SHARPNESS, 5)
            .enchant(Enchantment.UNBREAKING, 3)
            .itemFlag(ItemFlag.HIDE_ENCHANTS)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getType()).isEqualTo(Material.DIAMOND_SWORD);
        assertThat(stack.getAmount()).isEqualTo(1);

        ItemMeta meta = stack.getItemMeta();
        assertThat(meta).isNotNull();
        assertThat(meta.hasDisplayName()).isTrue();
        assertThat(meta.hasLore()).isTrue();
        assertThat(meta.getLore()).hasSize(2);
        assertThat(meta.hasEnchants()).isTrue();
        assertThat(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)).isTrue();
    }

    // ========================================
    // DYNAMIC PROPERTIES
    // ========================================

    @Test
    @DisplayName("Should render item with dynamic material")
    void testDynamicMaterial() {
        MenuItem item = MenuItem.item()
            .material(() -> Material.GOLD_INGOT)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getType()).isEqualTo(Material.GOLD_INGOT);
    }

    @Test
    @DisplayName("Should render item with dynamic amount")
    void testDynamicAmount() {
        MenuItem item = MenuItem.item()
            .material(Material.ARROW)
            .amount(() -> 16)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getAmount()).isEqualTo(16);
    }

    @Test
    @DisplayName("Should render item with dynamic visibility")
    void testDynamicVisibility() {
        MenuItem visibleItem = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(() -> true)
            .build();

        MenuItem invisibleItem = MenuItem.item()
            .material(Material.EMERALD)
            .visible(() -> false)
            .build();

        assertThat(visibleItem.render(this.context)).isNotNull();
        assertThat(invisibleItem.render(this.context)).isNull();
    }

    // ========================================
    // CONTEXT-AWARE PROPERTIES
    // ========================================

    @Test
    @DisplayName("Should render item with context-aware material")
    void testContextAwareMaterial() {
        MenuItem item = MenuItem.item()
            .material(ctx -> Material.REDSTONE)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        assertThat(stack.getType()).isEqualTo(Material.REDSTONE);
    }

    @Test
    @DisplayName("Should render item with context-aware name")
    void testContextAwareName() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .name(ctx -> "&eContext Name")
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasDisplayName()).isTrue();
    }

    @Test
    @DisplayName("Should render item with context-aware visibility")
    void testContextAwareVisibility() {
        MenuItem item = MenuItem.item()
            .material(Material.DIAMOND)
            .visible(ctx -> true)
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
    }

    // ========================================
    // EMPTY/NULL HANDLING
    // ========================================

    @Test
    @DisplayName("Should handle empty name")
    void testEmptyName() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .name("")
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        // Empty name should not set display name
        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasDisplayName()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty lore")
    void testEmptyLore() {
        MenuItem item = MenuItem.item()
            .material(Material.STONE)
            .lore("")
            .build();

        ItemStack stack = item.render(this.context);

        assertThat(stack).isNotNull();
        // Empty lore string becomes a list with one empty element after split
        ItemMeta meta = stack.getItemMeta();
        assertThat(meta.hasLore()).isTrue();
        assertThat(meta.getLore()).hasSize(1);
        assertThat(meta.getLore().get(0)).isEmpty();
    }
}
