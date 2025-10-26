package eu.okaeri.menu.bukkit.unit.item;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.state.ViewerState;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static eu.okaeri.menu.item.MenuItem.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for item-level variables feature.
 * Tests that variables can be defined once at item level and shared across name, lore, etc.
 */
class ItemLevelVarsTest {

    private ServerMock server;
    private org.bukkit.plugin.java.JavaPlugin plugin;
    private PlayerMock player;
    private Menu menu;
    private MenuContext context;
    private PlainTextComponentSerializer plainSerializer;

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

        this.plainSerializer = PlainTextComponentSerializer.plainText();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should use item-level vars in name()")
    void testItemVarsInName() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .vars(Map.of("price", 100))
            .name("Price: <price> coins")
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("100");
    }

    @Test
    @DisplayName("Should use item-level vars in lore()")
    void testItemVarsInLore() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .vars(Map.of("stock", 50))
            .lore("Stock: <stock> items")
            .build();

        var lore = item.getLore().get(this.context);
        assertThat(lore).hasSize(1);
        String loreLine = this.plainSerializer.serialize(lore.get(0));
        assertThat(loreLine).contains("50");
    }

    @Test
    @DisplayName("Should share item-level vars between name and lore")
    void testVarsSharedBetweenNameAndLore() {
        MenuItem item = item()
            .material(Material.GOLD_INGOT)
            .vars(Map.of(
                "price", 250,
                "currency", "gold"
            ))
            .name("Price: <price> <currency>")
            .lore("Buy for <price> <currency>")
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        var lore = item.getLore().get(this.context);

        assertThat(name).contains("250").contains("gold");
        String loreLine = this.plainSerializer.serialize(lore.get(0));
        assertThat(loreLine).contains("250").contains("gold");
    }

    @Test
    @DisplayName("Should support reactive vars (Suppliers)")
    void testReactiveVars() {
        int[] counter = {0};

        MenuItem item = item()
            .material(Material.DIAMOND)
            .vars(Map.of("count", (Supplier<Integer>) () -> counter[0]))
            .name("Count: <count>")
            .build();

        // First render
        String name1 = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name1).contains("0");

        // Change value
        counter[0] = 42;
        this.context.invalidate();

        // Second render
        String name2 = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name2).contains("42");
    }

    @Test
    @DisplayName("Should allow method-level vars to override item-level vars")
    void testMethodVarsOverride() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .vars(Map.of("price", 100))  // Item-level: 100
            .name("Name price: <price>")
            .lore("Lore price: <price>", Map.of("price", 200))  // Method-level override: 200
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        var lore = item.getLore().get(this.context);

        assertThat(name).contains("100");  // Uses item-level
        String loreLine = this.plainSerializer.serialize(lore.get(0));
        assertThat(loreLine).contains("200");  // Uses method-level override
    }

    @Test
    @DisplayName("Should merge item-level and method-level vars")
    void testVarsMerging() {
        MenuItem item = item()
            .material(Material.EMERALD)
            .vars(Map.of(
                "price", 100,
                "currency", "coins"
            ))
            .lore("Price: <price> <currency>\nStock: <stock>", Map.of("stock", 50))
            .build();

        var lore = item.getLore().get(this.context);

        // Should have both item-level vars (price, currency) and method-level var (stock)
        String loreText = lore.stream().map(this.plainSerializer::serialize).collect(Collectors.joining(" "));
        assertThat(loreText).contains("100").contains("coins").contains("50");
    }

    @Test
    @DisplayName("Should work with empty item-level vars")
    void testEmptyItemVars() {
        MenuItem item = item()
            .material(Material.STONE)
            .name("Price: <price>", Map.of("price", 50))  // Only method-level vars
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("50");
    }

    @Test
    @DisplayName("Should work with empty method-level vars")
    void testEmptyMethodVars() {
        MenuItem item = item()
            .material(Material.IRON_INGOT)
            .vars(Map.of("price", 75))
            .name("Price: <price>")  // Only item-level vars
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("75");
    }

    @Test
    @DisplayName("Should handle multiple placeholders from item-level vars")
    void testMultiplePlaceholders() {
        MenuItem item = item()
            .material(Material.DIAMOND_SWORD)
            .vars(Map.of(
                "name", "Excalibur",
                "damage", 10,
                "durability", 100
            ))
            .name("<name>")
            .lore("""
                Damage: <damage>
                Durability: <durability>
                """)
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        var lore = item.getLore().get(this.context);

        assertThat(name).contains("Excalibur");
        String loreText = lore.stream().map(this.plainSerializer::serialize).collect(Collectors.joining(" "));
        assertThat(loreText).contains("10").contains("100");
    }

    @Test
    @DisplayName("Should preserve MiniMessage formatting with item-level vars")
    void testMiniMessageWithVars() {
        MenuItem item = item()
            .material(Material.GOLD_BLOCK)
            .vars(Map.of("amount", 1000))
            .name("<gold>Balance: <amount> coins</gold>")
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("1000");
        // Note: MiniMessage formatting is processed, so we just verify the placeholder works
    }

    @Test
    @DisplayName("Should work with null vars map")
    void testNullMethodVars() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .vars(Map.of("price", 100))
            .name("Price: <price>", (Map<String, Object>) null)  // Explicitly null
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("100");  // Should still use item-level vars
    }

    @Test
    @DisplayName("Should handle reactive vars in lore")
    void testReactiveVarsInLore() {
        int[] stock = {10};

        MenuItem item = item()
            .material(Material.APPLE)
            .vars(Map.of("stock", (Supplier<Integer>) () -> stock[0]))
            .lore("Stock: <stock> apples")
            .build();

        var lore1 = item.getLore().get(this.context);
        String loreLine1 = this.plainSerializer.serialize(lore1.get(0));
        assertThat(loreLine1).contains("10");

        stock[0] = 5;
        this.context.invalidate();

        var lore2 = item.getLore().get(this.context);
        String loreLine2 = this.plainSerializer.serialize(lore2.get(0));
        assertThat(loreLine2).contains("5");
    }

    // ========================================
    // Locale Map Tests (Map<Locale, String>)
    // ========================================

    @Test
    @DisplayName("Should support name with locale map")
    void testNameWithLocaleMap() {
        MenuItem item = item()
            .material(Material.DIAMOND_SWORD)
            .name(Map.of(
                Locale.ENGLISH, "Diamond Sword",
                new Locale("pl"), "Diamentowy miecz",
                new Locale("de"), "Diamantschwert"
            ))
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        // Default locale should be English (or fallback)
        assertThat(name).isNotEmpty();
    }

    @Test
    @DisplayName("Should support lore with locale map")
    void testLoreWithLocaleMap() {
        MenuItem item = item()
            .material(Material.DIAMOND)
            .lore(Map.of(
                Locale.ENGLISH, """
                    A precious gem
                    Worth a lot!""",
                new Locale("pl"), """
                    Cenny klejnot
                    Wart dużo!"""
            ))
            .build();

        var lore = item.getLore().get(this.context);
        assertThat(lore).hasSize(2);
    }

    @Test
    @DisplayName("Should support locale map with placeholders")
    void testLocaleMapWithPlaceholders() {
        MenuItem item = item()
            .material(Material.GOLD_INGOT)
            .vars(Map.of("price", 100))
            .name(Map.of(
                Locale.ENGLISH, "Price: <price> coins",
                new Locale("pl"), "Cena: <price> monet",
                new Locale("de"), "Preis: <price> Münzen"
            ))
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name).contains("100");
    }

    @Test
    @DisplayName("Should support locale map with method-level vars override")
    void testLocaleMapWithVarsOverride() {
        MenuItem item = item()
            .material(Material.EMERALD)
            .vars(Map.of("price", 50))  // Item-level: 50
            .name(Map.of(
                Locale.ENGLISH, "Item price: <price>",
                new Locale("pl"), "Cena przedmiotu: <price>"
            ))
            .lore(Map.of(
                Locale.ENGLISH, "Sale price: <price> coins",
                new Locale("pl"), "Cena wyprzedażowa: <price> monet"
            ), Map.of("price", 75))  // Method-level override: 75
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        var lore = item.getLore().get(this.context);

        assertThat(name).contains("50");  // Uses item-level
        String loreLine = this.plainSerializer.serialize(lore.get(0));
        assertThat(loreLine).contains("75");  // Uses method-level override
    }

    @Test
    @DisplayName("Should support locale map with dynamic vars")
    void testLocaleMapWithDynamicVars() {
        int[] stock = {100};

        MenuItem item = item()
            .material(Material.APPLE)
            .name(Map.of(
                Locale.ENGLISH, "Stock: <stock> apples",
                new Locale("pl"), "Zapas: <stock> jabłek"
            ), ctx -> Map.of("stock", stock[0]))
            .build();

        // First render
        String name1 = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name1).contains("100");

        // Change value
        stock[0] = 50;
        this.context.invalidate();

        // Second render
        String name2 = this.plainSerializer.serialize(item.getName().get(this.context));
        assertThat(name2).contains("50");
    }

    @Test
    @DisplayName("Should support locale map in lore with multiple lines")
    void testLocaleMapLoreMultiline() {
        MenuItem item = item()
            .material(Material.DIAMOND_SWORD)
            .vars(Map.of(
                "damage", 10,
                "durability", 100
            ))
            .lore(Map.of(
                Locale.ENGLISH, """
                    Damage: <damage>
                    Durability: <durability>
                    
                    Click to equip!""",
                new Locale("pl"), """
                    Obrażenia: <damage>
                    Wytrzymałość: <durability>
                    
                    Kliknij aby założyć!"""
            ))
            .build();

        var lore = item.getLore().get(this.context);
        assertThat(lore).hasSizeGreaterThan(2);

        String loreText = lore.stream()
            .map(this.plainSerializer::serialize)
            .collect(Collectors.joining(" "));
        assertThat(loreText).contains("10").contains("100");
    }

    @Test
    @DisplayName("Should handle empty locale map gracefully")
    void testEmptyLocaleMap() {
        MenuItem item = item()
            .material(Material.STONE)
            .name(Map.of())  // Empty locale map
            .build();

        String name = this.plainSerializer.serialize(item.getName().get(this.context));
        // Should return empty or fallback, not crash
        assertThat(name).isNotNull();
    }

    @Test
    @DisplayName("Should merge item-level vars with locale map vars")
    void testLocaleMapWithMergedVars() {
        MenuItem item = item()
            .material(Material.IRON_SWORD)
            .vars(Map.of(
                "damage", 5,
                "durability", 250
            ))
            .lore(Map.of(
                Locale.ENGLISH, """
                    Damage: <damage>
                    Durability: <durability>
                    Stock: <stock>""",
                new Locale("pl"), """
                    Obrażenia: <damage>
                    Wytrzymałość: <durability>
                    Zapas: <stock>"""
            ), Map.of("stock", 20))  // Add method-level var
            .build();

        var lore = item.getLore().get(this.context);
        String loreText = lore.stream().map(this.plainSerializer::serialize).collect(Collectors.joining(" "));

        // Should have all vars: item-level (damage, durability) + method-level (stock)
        assertThat(loreText).contains("5").contains("250").contains("20");
    }
}
