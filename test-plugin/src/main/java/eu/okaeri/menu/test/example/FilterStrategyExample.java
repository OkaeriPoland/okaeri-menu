package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pane.pagination.FilterStrategy;
import eu.okaeri.menu.pane.pagination.ItemFilter;
import eu.okaeri.menu.pane.pagination.PaginationUtils;
import eu.okaeri.menu.pane.PaginatedPane;
import eu.okaeri.menu.pane.StaticPane;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;

/**
 * Examples demonstrating filter strategies (AND vs OR).
 * Shows how to combine multiple filters with different strategies.
 */
public class FilterStrategyExample {

    /**
     * Filter strategies (AND vs OR) demonstration.
     * Shows how multiple filters combine with different strategies.
     * Uses state API and declarative filters for clean, reactive code.
     */
    public static Menu createFilterStrategyMenu(Plugin plugin) {
        List<ShopItem> items = createShopItems();

        return Menu.builder(plugin)
            .title("&6Filter Strategies Demo")
            .rows(6)
            // Default state values
            .state(defaults -> defaults
                .define("strategy", FilterStrategy.AND)
            )
            // Strategy selector (top row)
            .pane(StaticPane.staticPane()
                .name("strategy")
                .bounds(0, 0, 1, 9)
                .item(0, 1, item()
                    .material(ctx -> (ctx.get("strategy", FilterStrategy.class) == FilterStrategy.AND)
                        ? Material.LIME_WOOL
                        : Material.GRAY_WOOL)
                    .name("&aAND Strategy")
                    .lore("""
                        &7Combines filters with AND logic
                        &7All filters must match
                        
                        &eClick to activate!""")
                    .onClick(event -> {
                        event.set("strategy", FilterStrategy.AND);
                        event.pagination("shop").setFilterStrategy(FilterStrategy.AND);
                        event.refresh();
                    })
                    .build())
                .item(0, 3, item()
                    .material(ctx -> (ctx.get("strategy", FilterStrategy.class) == FilterStrategy.OR)
                        ? Material.LIME_WOOL
                        : Material.GRAY_WOOL)
                    .name("&bOR Strategy")
                    .lore("""
                        &7Combines filters with OR logic
                        &7Any filter can match
                        
                        &eClick to activate!""")
                    .onClick(event -> {
                        event.set("strategy", FilterStrategy.OR);
                        event.pagination("shop").setFilterStrategy(FilterStrategy.OR);
                        event.refresh();
                    })
                    .build())
                .item(0, 8, item()
                    .material(Material.BARRIER)
                    .name("&cClear Filters")
                    .onClick(event -> {
                        // Clear all filter states
                        event.clearState();
                        event.refresh();
                    })
                    .build())
                .build())
            // Filter controls (row 2)
            .pane(StaticPane.staticPane()
                .name("filters")
                .bounds(1, 0, 1, 9)
                .item(0, 1, item()
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("weapon")
                        .when(ctx -> ctx.getBool("filter:weapon"))
                        .predicate(item -> "weapon".equals(item.category()))
                        .build())
                    .material(ctx -> ctx.getBool("filter:weapon")
                        ? Material.LIME_STAINED_GLASS_PANE
                        : Material.DIAMOND_SWORD)
                    .name(ctx -> ctx.getBool("filter:weapon")
                        ? "&a✓ Weapons Only"
                        : "&bWeapons Only")
                    .lore("""
                        &7Filter by category
                        
                        &eTry with AND and OR!""")
                    .onClick(event -> {
                        event.set("filter:weapon", !event.getBool("filter:weapon"));
                        event.refresh();
                    })
                    .build())
                .item(0, 3, item()
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("armor")
                        .when(ctx -> ctx.getBool("filter:armor"))
                        .predicate(item -> "armor".equals(item.category()))
                        .build())
                    .material(ctx -> ctx.getBool("filter:armor")
                        ? Material.LIME_STAINED_GLASS_PANE
                        : Material.DIAMOND_CHESTPLATE)
                    .name(ctx -> ctx.getBool("filter:armor")
                        ? "&a✓ Armor Only"
                        : "&9Armor Only")
                    .lore("""
                        &7Filter by category
                        
                        &eTry with AND and OR!""")
                    .onClick(event -> {
                        event.set("filter:armor", !event.getBool("filter:armor"));
                        event.refresh();
                    })
                    .build())
                .item(0, 5, item()
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("expensive")
                        .when(ctx -> ctx.getBool("filter:expensive"))
                        .predicate(item -> item.price() > 100)
                        .build())
                    .material(ctx -> ctx.getBool("filter:expensive")
                        ? Material.LIME_STAINED_GLASS_PANE
                        : Material.GOLD_INGOT)
                    .name(ctx -> ctx.getBool("filter:expensive")
                        ? "&a✓ Expensive (>100)"
                        : "&6Expensive (>100)")
                    .lore("""
                        &7Filter by price
                        
                        &eTry with AND and OR!""")
                    .onClick(event -> {
                        event.set("filter:expensive", !event.getBool("filter:expensive"));
                        event.refresh();
                    })
                    .build())
                .item(0, 7, item()
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("affordable")
                        .when(ctx -> ctx.getBool("filter:affordable"))
                        .predicate(item -> item.price() < 100)
                        .build())
                    .material(ctx -> ctx.getBool("filter:affordable")
                        ? Material.LIME_STAINED_GLASS_PANE
                        : Material.EMERALD)
                    .name(ctx -> ctx.getBool("filter:affordable")
                        ? "&a✓ Affordable (<100)"
                        : "&aAffordable (<100)")
                    .lore("""
                        &7Filter by price
                        
                        &eTry with AND and OR!""")
                    .onClick(event -> {
                        event.set("filter:affordable", !event.getBool("filter:affordable"));
                        event.refresh();
                    })
                    .build())
                .build())
            // Shop items
            .pane(PaginatedPane.<ShopItem>pane()
                .name("shop")
                .bounds(2, 0, 3, 9)
                .items(items)
                // itemsPerPage defaults to pane size (9x3 = 27 slots)
                .renderer((ctx, item, index) -> item()
                    .material(item.material())
                    .vars(Map.of(
                        "name", item.name(),
                        "category", item.category(),
                        "price", item.price()
                    ))
                    .name("&e<name>")
                    .lore("""
                        &7Category: &f<category>
                        &7Price: &6<price> coins
                        
                        &eClick to purchase!""")
                    .build())
                .build())
            // Navigation
            .pane(StaticPane.staticPane()
                .name("nav")
                .bounds(5, 0, 1, 9)
                .item(0, 3, PaginationUtils.previousPageButton("shop").build())
                .item(0, 4, PaginationUtils.pageIndicator("shop").build())
                .item(0, 5, PaginationUtils.nextPageButton("shop").build())
                .item(0, 8, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    @Value
    @Accessors(fluent = true)
    public static class ShopItem {
        String name;
        Material material;
        String category;
        int price;
    }

    private static List<ShopItem> createShopItems() {
        return Arrays.asList(
            // Weapons (expanded)
            new ShopItem("Netherite Sword", Material.NETHERITE_SWORD, "weapon", 250),
            new ShopItem("Diamond Sword", Material.DIAMOND_SWORD, "weapon", 150),
            new ShopItem("Iron Sword", Material.IRON_SWORD, "weapon", 75),
            new ShopItem("Golden Sword", Material.GOLDEN_SWORD, "weapon", 200),
            new ShopItem("Stone Sword", Material.STONE_SWORD, "weapon", 25),
            new ShopItem("Wooden Sword", Material.WOODEN_SWORD, "weapon", 10),
            new ShopItem("Bow", Material.BOW, "weapon", 50),
            new ShopItem("Crossbow", Material.CROSSBOW, "weapon", 120),
            new ShopItem("Trident", Material.TRIDENT, "weapon", 300),
            new ShopItem("Netherite Axe", Material.NETHERITE_AXE, "weapon", 250),
            new ShopItem("Diamond Axe", Material.DIAMOND_AXE, "weapon", 150),
            new ShopItem("Iron Axe", Material.IRON_AXE, "weapon", 75),

            // Armor - Diamond Set
            new ShopItem("Diamond Helmet", Material.DIAMOND_HELMET, "armor", 100),
            new ShopItem("Diamond Chestplate", Material.DIAMOND_CHESTPLATE, "armor", 200),
            new ShopItem("Diamond Leggings", Material.DIAMOND_LEGGINGS, "armor", 175),
            new ShopItem("Diamond Boots", Material.DIAMOND_BOOTS, "armor", 90),

            // Armor - Iron Set
            new ShopItem("Iron Helmet", Material.IRON_HELMET, "armor", 50),
            new ShopItem("Iron Chestplate", Material.IRON_CHESTPLATE, "armor", 100),
            new ShopItem("Iron Leggings", Material.IRON_LEGGINGS, "armor", 85),
            new ShopItem("Iron Boots", Material.IRON_BOOTS, "armor", 45),

            // Armor - Gold Set
            new ShopItem("Golden Helmet", Material.GOLDEN_HELMET, "armor", 150),
            new ShopItem("Golden Chestplate", Material.GOLDEN_CHESTPLATE, "armor", 250),
            new ShopItem("Golden Leggings", Material.GOLDEN_LEGGINGS, "armor", 200),
            new ShopItem("Golden Boots", Material.GOLDEN_BOOTS, "armor", 125),

            // Armor - Netherite Set
            new ShopItem("Netherite Helmet", Material.NETHERITE_HELMET, "armor", 180),
            new ShopItem("Netherite Chestplate", Material.NETHERITE_CHESTPLATE, "armor", 350),
            new ShopItem("Netherite Leggings", Material.NETHERITE_LEGGINGS, "armor", 300),
            new ShopItem("Netherite Boots", Material.NETHERITE_BOOTS, "armor", 150),

            // Tools - Diamond
            new ShopItem("Diamond Pickaxe", Material.DIAMOND_PICKAXE, "tool", 150),
            new ShopItem("Diamond Shovel", Material.DIAMOND_SHOVEL, "tool", 100),
            new ShopItem("Diamond Hoe", Material.DIAMOND_HOE, "tool", 120),

            // Tools - Iron
            new ShopItem("Iron Pickaxe", Material.IRON_PICKAXE, "tool", 75),
            new ShopItem("Iron Shovel", Material.IRON_SHOVEL, "tool", 50),
            new ShopItem("Iron Hoe", Material.IRON_HOE, "tool", 60),

            // Tools - Netherite
            new ShopItem("Netherite Pickaxe", Material.NETHERITE_PICKAXE, "tool", 250),
            new ShopItem("Netherite Shovel", Material.NETHERITE_SHOVEL, "tool", 180),
            new ShopItem("Netherite Hoe", Material.NETHERITE_HOE, "tool", 200),

            // Food
            new ShopItem("Golden Apple", Material.GOLDEN_APPLE, "food", 100),
            new ShopItem("Enchanted Golden Apple", Material.ENCHANTED_GOLDEN_APPLE, "food", 500),
            new ShopItem("Steak", Material.COOKED_BEEF, "food", 15),
            new ShopItem("Bread", Material.BREAD, "food", 5),

            // Blocks
            new ShopItem("Diamond Block", Material.DIAMOND_BLOCK, "block", 450),
            new ShopItem("Gold Block", Material.GOLD_BLOCK, "block", 350),
            new ShopItem("Iron Block", Material.IRON_BLOCK, "block", 200),
            new ShopItem("Obsidian", Material.OBSIDIAN, "block", 80)
        );
    }
}
