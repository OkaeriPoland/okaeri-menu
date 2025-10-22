package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.async.AsyncUtils;
import eu.okaeri.menu.item.AsyncMenuItem;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pagination.FilterStrategy;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pagination.PaginationContext;
import eu.okaeri.menu.pagination.PaginationUtils;
import eu.okaeri.menu.pane.AsyncPaginatedPane;
import eu.okaeri.menu.pane.StaticPane;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprehensive async shop example demonstrating:
 * - AsyncPaginatedPane with simulated database delay
 * - AsyncMenuItem for player balance lookup
 * - Dynamic title with loading states and counters
 * - Filter tracking and counts
 * - Loading/error/empty suspense states
 * - Real-time statistics
 */
public class AsyncShopExample {

    /**
     * Creates an async shop menu with comprehensive async features.
     * Command: /menu async
     */
    public static Menu createAsyncShopMenu(Plugin plugin, Player player) {
        // Filter activation states
        boolean[] weaponFilterActive = {false};
        boolean[] armorFilterActive = {false};
        boolean[] expensiveFilterActive = {false};

        // Statistics tracking
        int[] totalPurchases = {0};
        int[] totalSpent = {0};

        return Menu.builder(plugin)
            // Dynamic title with async data and counters
            .title(ctx -> {
                // Access async pane data to show item count
                return ctx.paneData("shop-pane", ShopItem.class)
                    .map(state -> {
                        StringBuilder title = new StringBuilder("<yellow>⚡ Async Shop");

                        // Show filtered/total count
                        title.append(" <gray>[<white>").append(state.filtered())
                            .append("<gray>/").append(state.total()).append("]");

                        // Show active filters using new API
                        int filters = ctx.pagination("shop-pane").getActiveFilterCount();
                        if (filters > 0) {
                            title.append(" <gray>[<green>").append(filters).append(" filter");
                            if (filters > 1) title.append("s");
                            title.append("</green>]");
                        }

                        // Show purchase stats
                        if (totalPurchases[0] > 0) {
                            title.append(" <gray>[<gold>").append(totalPurchases[0]).append(" bought</gold>]");
                        }

                        return title.toString();
                    })
                    .loading("<yellow>⚡ Async Shop <gray>[<yellow>Loading...</yellow>]")
                    .error("<yellow>⚡ Async Shop <gray>[<red>Error</red>]")
                    .orElse("<yellow>⚡ Async Shop");
            })
            .rows(6)
            .updateInterval(Duration.ofMillis(500))  // Update title frequently for loading states
            // Balance display (async item)
            .pane("balance", StaticPane.builder()
                .name("balance")
                .bounds(0, 0, 2, 1)
                .item(0, 0, AsyncMenuItem.itemAsync()
                    .key("balance-item")
                    .data(() -> {
                        // Simulate async balance lookup from database
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(300, 800));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // Simulated balance
                        return ThreadLocalRandom.current().nextInt(500, 5000);
                    })
                    .ttl(Duration.ofSeconds(10))  // Cache for 10 seconds
                    .loading(MenuItem.item()
                        .material(Material.GRAY_STAINED_GLASS_PANE)
                        .name("<gray>💰 Loading Balance...")
                        .lore("<gray>Fetching your balance\n<gray>from the database...")
                        .build())
                    .error(ex -> MenuItem.item()
                        .material(Material.BARRIER)
                        .name("<red>✕ Balance Error")
                        .lore("<gray>Failed to load balance\n<red>" + ex.getMessage())
                        .build())
                    .loaded(balance -> MenuItem.item()
                        .material(Material.GOLD_INGOT)
                        .name("<gradient:yellow:gold><b>💰 Your Balance")
                        .vars(Map.of("balance", balance))
                        .lore("""
                                <gray>Balance: <gold><balance> coins
                                <gray>Updates: <white>Every 10s
                                
                                <gray>Purchases: <yellow><purchases>
                                <gray>Total Spent: <gold><spent> coins
                                
                                <dark_gray>Balance loads async!""",
                            ctx -> Map.of(
                                "purchases", totalPurchases[0],
                                "spent", totalSpent[0]
                            ))
                        .build())
                    .build())
                .item(1, 0, MenuItem.item()
                    .material(Material.PAPER)
                    .name("<yellow><b>📊 Statistics")
                    .lore("""
                            <gray>Total Items: <white><total_items>
                            <gray>Filtered: <white><filtered_items>
                            <gray>Showing: <white><showing_items>
                            
                            <gray>Active Filters: <green><active_filters>
                            <gray>Strategy: <white><strategy>
                            
                            <dark_gray>Updates in real-time!""",
                        ctx -> {
                            // Use paneData API to access async pane state
                            return ctx.paneData("shop-pane", ShopItem.class)
                                .map(state -> Map.<String, Object>of(
                                    "total_items", state.total(),
                                    "filtered_items", state.filtered(),
                                    "showing_items", state.currentPage().size(),
                                    "active_filters", ctx.pagination("shop-pane").getActiveFilterCount(),
                                    "strategy", ctx.pagination("shop-pane").getFilterStrategy().name()
                                ))
                                .orElse(Map.<String, Object>of(
                                    "total_items", "?",
                                    "filtered_items", "?",
                                    "showing_items", "?",
                                    "active_filters", 0,
                                    "strategy", "AND"
                                ));
                        })
                    .build())
                .build())
            // Filter controls
            .pane("filters", StaticPane.builder()
                .name("filters")
                .bounds(2, 0, 7, 1)
                .item(0, 0, MenuItem.item()
                    .material(() -> weaponFilterActive[0] ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
                    .name(() -> weaponFilterActive[0] ? "<green>✓ Weapons" : "<gray>Weapons")
                    .lore("""
                            <gray>Show weapons only
                            <gray>Status: <status>
                            
                            <yellow>Click to toggle!""",
                        ctx -> Map.of("status", weaponFilterActive[0] ? "<green>Active" : "<gray>Inactive"))
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop-pane")
                        .id("weapon")
                        .when(() -> weaponFilterActive[0])
                        .predicate(item -> item.category().equals("weapon"))
                        .build())
                    .onClick(ctx -> {
                        weaponFilterActive[0] = !weaponFilterActive[0];
                        ctx.playSound(Sound.UI_BUTTON_CLICK, 0.5f, weaponFilterActive[0] ? 1.5f : 1.0f);
                    })
                    .build())
                .item(2, 0, MenuItem.item()
                    .material(() -> armorFilterActive[0] ? Material.DIAMOND_CHESTPLATE : Material.LEATHER_CHESTPLATE)
                    .name(() -> armorFilterActive[0] ? "<green>✓ Armor" : "<gray>Armor")
                    .lore("""
                            <gray>Show armor only
                            <gray>Status: <status>
                            
                            <yellow>Click to toggle!""",
                        ctx -> Map.of("status", armorFilterActive[0] ? "<green>Active" : "<gray>Inactive"))
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop-pane")
                        .id("armor")
                        .when(() -> armorFilterActive[0])
                        .predicate(item -> item.category().equals("armor"))
                        .build())
                    .onClick(ctx -> {
                        armorFilterActive[0] = !armorFilterActive[0];
                        ctx.playSound(Sound.UI_BUTTON_CLICK, 0.5f, armorFilterActive[0] ? 1.5f : 1.0f);
                    })
                    .build())
                .item(4, 0, MenuItem.item()
                    .material(() -> expensiveFilterActive[0] ? Material.GOLD_INGOT : Material.IRON_INGOT)
                    .name(() -> expensiveFilterActive[0] ? "<green>✓ Expensive (>100)" : "<gray>Expensive (>100)")
                    .lore("""
                            <gray>Show items over 100 coins
                            <gray>Status: <status>
                            
                            <yellow>Click to toggle!""",
                        ctx -> Map.of("status", expensiveFilterActive[0] ? "<green>Active" : "<gray>Inactive"))
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop-pane")
                        .id("expensive")
                        .when(() -> expensiveFilterActive[0])
                        .predicate(item -> item.price() > 100)
                        .build())
                    .onClick(ctx -> {
                        expensiveFilterActive[0] = !expensiveFilterActive[0];
                        ctx.playSound(Sound.UI_BUTTON_CLICK, 0.5f, expensiveFilterActive[0] ? 1.5f : 1.0f);
                    })
                    .build())
                .item(6, 0, MenuItem.item()
                    .material(Material.BARRIER)
                    .name("<red><b>Clear All Filters")
                    .lore("""
                        <gray>Remove all active filters
                        
                        <red>Click to clear!""")
                    .onClick(ctx -> {
                        weaponFilterActive[0] = false;
                        armorFilterActive[0] = false;
                        expensiveFilterActive[0] = false;
                        ctx.playSound(Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
                    })
                    .build())
                .build())
            // Async shop items (AsyncPaginatedPane)
            .pane("shop-pane", AsyncPaginatedPane.paneAsync(ShopItem.class)
                .name("shop-pane")
                .bounds(0, 1, 9, 3)
                .loader(() -> {
                    // Simulate async database query
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2500));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // Simulate occasional errors (5% chance)
                    if (ThreadLocalRandom.current().nextInt(100) < 5) {
                        throw new RuntimeException("Database connection timeout!");
                    }

                    return createShopItems();
                })
                .ttl(Duration.ofSeconds(30))  // Cache for 30 seconds
                .renderer((item, index) -> MenuItem.item()
                    .material(item.material())
                    .vars(Map.of(
                        "name", item.name(),
                        "category", item.category(),
                        "price", item.price(),
                        "stock", item.stock()
                    ))
                    .name("<yellow><b><name>")
                    .lore("""
                        <gray>Category: <white><category>
                        <gray>Price: <gold><price> coins
                        <gray>Stock: <white><stock> left
                        
                        <yellow>Click to purchase!
                        <gray>Middle-click for details""")
                    .onClick(ctx -> {
                        totalPurchases[0]++;
                        totalSpent[0] += item.price();
                        ctx.sendMessage("<green>✓ Purchased <yellow>" + item.name() + " <green>for <gold>" + item.price() + " coins<green>!");
                        ctx.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        ctx.refresh();  // Refresh to update balance and stats
                    })
                    .onMiddleClick(ctx -> {
                        ctx.sendMessage("<yellow>━━━━━━━━━━━━━━━━━━━━━━━━");
                        ctx.sendMessage("<yellow><b>Item Details:");
                        ctx.sendMessage("<gray>Name: <white>" + item.name());
                        ctx.sendMessage("<gray>Category: <white>" + item.category());
                        ctx.sendMessage("<gray>Price: <gold>" + item.price() + " coins");
                        ctx.sendMessage("<gray>Stock: <white>" + item.stock() + " available");
                        ctx.sendMessage("<gray>Rarity: <white>" + (item.price() > 200 ? "Legendary" : item.price() > 100 ? "Epic" : "Common"));
                        ctx.sendMessage("<yellow>━━━━━━━━━━━━━━━━━━━━━━━━");
                        ctx.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
                    })
                    .build())
                // Suspense states
                .loading(AsyncUtils.loadingItem(
                    Material.HOPPER,
                    "<yellow>⏳ Loading Shop Items..."
                ).build())
                .error(AsyncUtils.errorItem(
                    Material.BARRIER,
                    "<red>✕ Failed to Load Shop"
                ).build())
                .empty(AsyncUtils.emptyItem(
                    Material.CHEST,
                    "<gray>No Items Available"
                ).build())
                .build())
            // Navigation
            .pane("nav", StaticPane.builder()
                .name("nav")
                .bounds(0, 5, 9, 1)
                .item(2, 0, PaginationUtils.previousPageButton("shop-pane")
                    .name("<yellow>← Previous Page")
                    .build())
                .item(3, 0, MenuItem.item()
                    .material(Material.COMPARATOR)
                    .name("<yellow><b>Reload Shop Data")
                    .lore("""
                        <gray>Clears cache and reloads
                        <gray>shop items from database
                        
                        <yellow>Click to reload!
                        <gray>Shows loading state""")
                    .onClick(ctx -> {
                        // Clear the async cache to trigger reload
                        var viewerState = ctx.getMenu().getViewerState(ctx.getEntity().getUniqueId());
                        if (viewerState != null) {
                            viewerState.getAsyncCache().invalidate("shop-pane");
                            viewerState.getAsyncCache().invalidate("balance-item");
                        }
                        ctx.sendMessage("<yellow>⏳ Reloading shop data...");
                        ctx.playSound(Sound.BLOCK_LEVER_CLICK, 0.5f, 1.0f);
                        ctx.refresh();
                    })
                    .build())
                .item(4, 0, PaginationUtils.pageIndicator("shop-pane")
                    .name("<gray>Page <yellow><current_page><gray>/<yellow><total_pages>")
                    .build())
                .item(5, 0, MenuItem.item()
                    .material(Material.REDSTONE)
                    .name("<yellow><b>Filter Strategy")
                    .lore("""
                            <gray>Current: <white><strategy>
                            
                            <gray>AND: All filters must match
                            <gray>OR: Any filter can match
                            
                            <yellow>Click to toggle!""",
                        ctx -> Map.of("strategy", ctx.pagination("shop-pane").getFilterStrategy().name()))
                    .onClick(ctx -> {
                        PaginationContext<ShopItem> pagination = ctx.pagination("shop-pane");

                        // Toggle strategy
                        FilterStrategy newStrategy = pagination.getFilterStrategy() == FilterStrategy.AND
                            ? FilterStrategy.OR
                            : FilterStrategy.AND;
                        pagination.setFilterStrategy(newStrategy);

                        ctx.sendMessage("<yellow>Filter strategy changed to: <white>" + newStrategy.name());
                        ctx.playSound(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
                        ctx.refresh();
                    })
                    .build())
                .item(6, 0, PaginationUtils.nextPageButton("shop-pane")
                    .name("<yellow>Next Page →")
                    .build())
                .item(8, 0, NavigationUtils.closeButton().build())
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
        int stock;
    }

    private static List<ShopItem> createShopItems() {
        return Arrays.asList(
            // Weapons (expanded)
            new ShopItem("Netherite Sword", Material.NETHERITE_SWORD, "weapon", 250, 5),
            new ShopItem("Diamond Sword", Material.DIAMOND_SWORD, "weapon", 150, 12),
            new ShopItem("Iron Sword", Material.IRON_SWORD, "weapon", 75, 25),
            new ShopItem("Golden Sword", Material.GOLDEN_SWORD, "weapon", 200, 8),
            new ShopItem("Stone Sword", Material.STONE_SWORD, "weapon", 25, 50),
            new ShopItem("Wooden Sword", Material.WOODEN_SWORD, "weapon", 10, 100),
            new ShopItem("Bow", Material.BOW, "weapon", 50, 30),
            new ShopItem("Crossbow", Material.CROSSBOW, "weapon", 120, 15),
            new ShopItem("Trident", Material.TRIDENT, "weapon", 300, 3),
            new ShopItem("Netherite Axe", Material.NETHERITE_AXE, "weapon", 250, 5),
            new ShopItem("Diamond Axe", Material.DIAMOND_AXE, "weapon", 150, 12),
            new ShopItem("Iron Axe", Material.IRON_AXE, "weapon", 75, 25),

            // Armor - Diamond Set
            new ShopItem("Diamond Helmet", Material.DIAMOND_HELMET, "armor", 100, 15),
            new ShopItem("Diamond Chestplate", Material.DIAMOND_CHESTPLATE, "armor", 200, 10),
            new ShopItem("Diamond Leggings", Material.DIAMOND_LEGGINGS, "armor", 175, 12),
            new ShopItem("Diamond Boots", Material.DIAMOND_BOOTS, "armor", 90, 18),

            // Armor - Iron Set
            new ShopItem("Iron Helmet", Material.IRON_HELMET, "armor", 50, 30),
            new ShopItem("Iron Chestplate", Material.IRON_CHESTPLATE, "armor", 100, 25),
            new ShopItem("Iron Leggings", Material.IRON_LEGGINGS, "armor", 85, 28),
            new ShopItem("Iron Boots", Material.IRON_BOOTS, "armor", 45, 35),

            // Armor - Gold Set
            new ShopItem("Golden Helmet", Material.GOLDEN_HELMET, "armor", 150, 8),
            new ShopItem("Golden Chestplate", Material.GOLDEN_CHESTPLATE, "armor", 250, 5),
            new ShopItem("Golden Leggings", Material.GOLDEN_LEGGINGS, "armor", 200, 6),
            new ShopItem("Golden Boots", Material.GOLDEN_BOOTS, "armor", 125, 10),

            // Armor - Netherite Set
            new ShopItem("Netherite Helmet", Material.NETHERITE_HELMET, "armor", 180, 4),
            new ShopItem("Netherite Chestplate", Material.NETHERITE_CHESTPLATE, "armor", 350, 2),
            new ShopItem("Netherite Leggings", Material.NETHERITE_LEGGINGS, "armor", 300, 3),
            new ShopItem("Netherite Boots", Material.NETHERITE_BOOTS, "armor", 150, 5),

            // Tools - Diamond
            new ShopItem("Diamond Pickaxe", Material.DIAMOND_PICKAXE, "tool", 150, 12),
            new ShopItem("Diamond Shovel", Material.DIAMOND_SHOVEL, "tool", 100, 15),
            new ShopItem("Diamond Hoe", Material.DIAMOND_HOE, "tool", 120, 10),

            // Tools - Iron
            new ShopItem("Iron Pickaxe", Material.IRON_PICKAXE, "tool", 75, 25),
            new ShopItem("Iron Shovel", Material.IRON_SHOVEL, "tool", 50, 30),
            new ShopItem("Iron Hoe", Material.IRON_HOE, "tool", 60, 28),

            // Tools - Netherite
            new ShopItem("Netherite Pickaxe", Material.NETHERITE_PICKAXE, "tool", 250, 5),
            new ShopItem("Netherite Shovel", Material.NETHERITE_SHOVEL, "tool", 180, 7),
            new ShopItem("Netherite Hoe", Material.NETHERITE_HOE, "tool", 200, 6),

            // Food
            new ShopItem("Golden Apple", Material.GOLDEN_APPLE, "food", 100, 20),
            new ShopItem("Enchanted Golden Apple", Material.ENCHANTED_GOLDEN_APPLE, "food", 500, 2),
            new ShopItem("Steak", Material.COOKED_BEEF, "food", 15, 100),
            new ShopItem("Bread", Material.BREAD, "food", 5, 150),
            new ShopItem("Cake", Material.CAKE, "food", 25, 50),
            new ShopItem("Cookie", Material.COOKIE, "food", 3, 200),

            // Blocks
            new ShopItem("Diamond Block", Material.DIAMOND_BLOCK, "block", 450, 8),
            new ShopItem("Gold Block", Material.GOLD_BLOCK, "block", 350, 10),
            new ShopItem("Iron Block", Material.IRON_BLOCK, "block", 200, 15),
            new ShopItem("Emerald Block", Material.EMERALD_BLOCK, "block", 600, 5),
            new ShopItem("Obsidian", Material.OBSIDIAN, "block", 80, 30),
            new ShopItem("Ender Chest", Material.ENDER_CHEST, "block", 150, 12),

            // Special Items
            new ShopItem("Elytra", Material.ELYTRA, "special", 1000, 1),
            new ShopItem("Totem of Undying", Material.TOTEM_OF_UNDYING, "special", 750, 2),
            new ShopItem("Nether Star", Material.NETHER_STAR, "special", 500, 3),
            new ShopItem("Dragon Egg", Material.DRAGON_EGG, "special", 2000, 1),
            new ShopItem("Beacon", Material.BEACON, "special", 800, 3),
            new ShopItem("Shulker Box", Material.SHULKER_BOX, "special", 200, 10)
        );
    }
}
