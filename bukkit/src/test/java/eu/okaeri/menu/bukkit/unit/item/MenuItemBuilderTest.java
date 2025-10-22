package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.item.AsyncMenuItem;
import eu.okaeri.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.item.MenuItem.itemAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MenuItem.Builder validation and construction.
 */
class MenuItemBuilderTest {

    // ========================================
    // BASIC BUILDER USAGE
    // ========================================

    @Test
    @DisplayName("Should create MenuItem with default values")
    void testDefaultBuilder() {
        MenuItem item = item().build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isFalse();
    }

    @Test
    @DisplayName("Should set material")
    void testSetMaterial() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set name")
    void testSetName() {
        MenuItem item = item()
            .material(Material.STONE)
            .name("Test Item")
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set lore")
    void testSetLore() {
        MenuItem item = item()
            .material(Material.IRON_SWORD)
            .lore("Line 1\nLine 2\nLine 3")
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set amount")
    void testSetAmount() {
        MenuItem item = item()
            .material(Material.ARROW)
            .amount(64)
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set enchantments")
    void testSetEnchantments() {
        MenuItem item = item()
            .material(Material.DIAMOND_SWORD)
            .enchant(Enchantment.SHARPNESS, 5)
            .enchant(Enchantment.UNBREAKING, 3)
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set item flags")
    void testSetItemFlags() {
        MenuItem item = item()
            .material(Material.GOLDEN_SWORD)
            .itemFlag(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set glint")
    void testSetGlint() {
        MenuItem item = item()
            .material(Material.STICK)
            .glint(true)
            .build();

        assertThat(item).isNotNull();
    }

    // ========================================
    // INTERACTIVE ITEM VALIDATION
    // ========================================

    @Test
    @DisplayName("Should create interactive item with allowPickup")
    void testInteractiveWithAllowPickup() {
        MenuItem item = item()
            .allowPickup(true)
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    @Test
    @DisplayName("Should create interactive item with allowPlacement")
    void testInteractiveWithAllowPlacement() {
        MenuItem item = item()
            .allowPlacement(true)
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    @Test
    @DisplayName("Should reject interactive item with material set")
    void testInteractiveWithMaterialRejects() {
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
    @DisplayName("Should allow interactive item with AIR material (default)")
    void testInteractiveWithAirMaterialAllowed() {
        MenuItem item = item()
            .material(Material.AIR)  // Explicitly set to AIR (default)
            .allowPickup(true)
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    @Test
    @DisplayName("Should reject interactive item with name set")
    void testInteractiveWithNameRejects() {
        assertThatThrownBy(() ->
            item()
                .name("Test Item")
                .allowPickup(true)
                .build()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Interactive items")
            .hasMessageContaining("should not have a name()");
    }

    @Test
    @DisplayName("Should reject interactive item with lore set")
    void testInteractiveWithLoreRejects() {
        assertThatThrownBy(() ->
            item()
                .lore("Line 1\nLine 2")
                .allowPlacement(true)
                .build()
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Interactive items")
            .hasMessageContaining("should not have lore()");
    }

    // ========================================
    // CLICK HANDLERS
    // ========================================

    @Test
    @DisplayName("Should set onClick handler")
    void testSetOnClickHandler() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .onClick(ctx -> ctx.playSound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP))
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set onLeftClick handler")
    void testSetOnLeftClickHandler() {
        MenuItem item = item()
            .material(Material.EMERALD)
            .onLeftClick(ctx -> {
            })
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set onRightClick handler")
    void testSetOnRightClickHandler() {
        MenuItem item = item()
            .material(Material.GOLD_INGOT)
            .onRightClick(ctx -> {
            })
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should set multiple click handlers")
    void testSetMultipleClickHandlers() {
        MenuItem item = item()
            .material(Material.REDSTONE)
            .onClick(ctx -> {
            })
            .onLeftClick(ctx -> {
            })
            .onRightClick(ctx -> {
            })
            .build();

        assertThat(item).isNotNull();
    }

    // ========================================
    // INTERACTIVE SLOT HANDLERS
    // ========================================

    @Test
    @DisplayName("Should set onItemChange handler for interactive slot")
    void testSetOnItemChangeHandler() {
        MenuItem item = item()
            .allowPickup(true)
            .onItemChange(ctx -> {
            })
            .build();

        assertThat(item).isNotNull();
        assertThat(item.isInteractive()).isTrue();
    }

    // ========================================
    // VISIBILITY
    // ========================================

    @Test
    @DisplayName("Should set visibility condition")
    void testSetVisibilityCondition() {
        MenuItem item = item()
            .material(Material.BARRIER)
            .visible(ctx -> true)
            .build();

        assertThat(item).isNotNull();
    }

    // ========================================
    // REACTIVE DATA SOURCES
    // ========================================

    @Test
    @DisplayName("Should add reactive data source with TTL")
    void testAddReactiveDataSourceWithTTL() {
        MenuItem item = item()
            .material(Material.CLOCK)
            .reactive("time", System::currentTimeMillis, java.time.Duration.ofSeconds(1))
            .name(ctx -> "Time: " + ctx.computed("time").orElse("?"))
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should add reactive data source with default TTL")
    void testAddReactiveDataSourceWithDefaultTTL() {
        MenuItem item = item()
            .material(Material.COMPASS)
            .reactive("direction", () -> "North")
            .name(ctx -> "Direction: " + ctx.computed("direction").orElse("?"))
            .build();

        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("Should add multiple reactive data sources")
    void testAddMultipleReactiveDataSources() {
        MenuItem item = item()
            .material(Material.PAPER)
            .reactive("source1", () -> "A")
            .reactive("source2", () -> "B")
            .reactive("source3", () -> "C")
            .name(ctx -> {
                String s1 = ctx.computed("source1").map(Object::toString).orElse("?");
                String s2 = ctx.computed("source2").map(Object::toString).orElse("?");
                String s3 = ctx.computed("source3").map(Object::toString).orElse("?");
                return s1 + s2 + s3;
            })
            .build();

        assertThat(item).isNotNull();
    }

    // ========================================
    // BUILDER METHOD CHAINING
    // ========================================

    @Test
    @DisplayName("Should support full method chaining")
    void testFullMethodChaining() {
        MenuItem item = item()
            .material(Material.DIAMOND_SWORD)
            .name("&cPowerful Sword")
            .lore("&7A very powerful weapon\n&7Deals massive damage")
            .amount(1)
            .enchant(Enchantment.SHARPNESS, 5)
            .enchant(Enchantment.UNBREAKING, 3)
            .itemFlag(ItemFlag.HIDE_ENCHANTS)
            .glint(true)
            .onClick(ctx -> ctx.sendMessage("You clicked the sword!"))
            .visible(ctx -> true)
            .build();

        assertThat(item).isNotNull();
    }

    // ========================================
    // STATIC FACTORY METHODS
    // ========================================

    @Test
    @DisplayName("Should create builder via static method")
    void testStaticBuilderMethod() {
        MenuItem.Builder builder = item();

        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Should create async builder via static method")
    void testStaticAsyncBuilderMethod() {
        AsyncMenuItem.Builder builder = itemAsync();

        assertThat(builder).isNotNull();
    }
}
