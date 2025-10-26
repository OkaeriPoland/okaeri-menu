package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pagination.ItemFilter;
import eu.okaeri.menu.pagination.PaginationUtils;
import eu.okaeri.menu.pane.PaginatedPane;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Shop example with declarative filters.
 * Demonstrates ItemFilter API where filters are attached to MenuItems.
 */
public class ShopExample {

    /**
     * Creates a shop menu with toggleable filters.
     * Filters are declared on menu items and automatically applied.
     */
    public static Menu createShopMenu(Plugin plugin) {
        List<ShopItem> items = createShopItems();

        return Menu.builder(plugin)
            .title("&6&lItem Shop")
            .rows(6)
            // Filter buttons with declarative filters
            .pane(staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(1, 0, MenuItem.item()
                    .material(ctx -> ctx.getBool("filter:weapon") ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD)
                    .name(ctx -> ctx.getBool("filter:weapon") ? "&a✓ Weapons" : "&7Weapons")
                    .lore("""
                        &7Show weapons only
                        
                        &eClick to toggle!""")
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("weapon")
                        .when(ctx -> ctx.getBool("filter:weapon"))
                        .predicate(item -> item.category().equals("weapon"))
                        .build())
                    .onClick(event -> {
                        event.set("filter:weapon", !event.getBool("filter:weapon"));
                    })
                    .build())
                .item(3, 0, MenuItem.item()
                    .material(ctx -> ctx.getBool("filter:armor") ? Material.DIAMOND_CHESTPLATE : Material.LEATHER_CHESTPLATE)
                    .name(ctx -> ctx.getBool("filter:armor") ? "&a✓ Armor" : "&7Armor")
                    .lore("""
                        &7Show armor only
                        
                        &eClick to toggle!""")
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("armor")
                        .when(ctx -> ctx.getBool("filter:armor"))
                        .predicate(item -> item.category().equals("armor"))
                        .build())
                    .onClick(event -> {
                        event.set("filter:armor", !event.getBool("filter:armor"));
                    })
                    .build())
                .item(5, 0, MenuItem.item()
                    .material(ctx -> ctx.getBool("filter:expensive") ? Material.GOLD_INGOT : Material.IRON_INGOT)
                    .name(ctx -> ctx.getBool("filter:expensive") ? "&a✓ Expensive (>100)" : "&7Expensive (>100)")
                    .lore("""
                        &7Show items over 100 coins
                        
                        &eClick to toggle!""")
                    .filter(ItemFilter.<ShopItem>builder()
                        .target("shop")
                        .id("expensive")
                        .when(ctx -> ctx.getBool("filter:expensive"))
                        .predicate(item -> item.price() > 100)
                        .build())
                    .onClick(event -> {
                        event.set("filter:expensive", !event.getBool("filter:expensive"));
                    })
                    .build())
                .item(7, 0, MenuItem.item()
                    .material(Material.PAPER)
                    .name("&eFilter Info")
                    .lore("""
                            &7Active filters: &f<active_count>
                            &7Strategy: &fAND
                            
                            &7All active filters
                            &7must match!""",
                        ctx -> {
                            int activeCount = 0;
                            if (ctx.getBool("filter:weapon")) activeCount++;
                            if (ctx.getBool("filter:armor")) activeCount++;
                            if (ctx.getBool("filter:expensive")) activeCount++;

                            return Map.of("active_count", activeCount);
                        })
                    .build())
                .item(8, 0, MenuItem.item()
                    .material(Material.BARRIER)
                    .name("&cClear All")
                    .onClick(event -> {
                        event.set("filter:weapon", false);
                        event.set("filter:armor", false);
                        event.set("filter:expensive", false);
                    })
                    .build())
                .build())
            // Shop items
            .pane(PaginatedPane.<ShopItem>pane()
                .name("shop")
                .bounds(0, 1, 9, 4)
                .items(items)
                // itemsPerPage defaults to pane size (9x4 = 36 slots)
                .renderer((ctx, item, index) -> MenuItem.item()
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
                    .onClick(event -> {
                        event.sendMessage("&aPurchased " + item.name() + " for " + item.price() + " coins!");
                        event.playSound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                    })
                    .build())
                .build())
            // Navigation
            .pane(staticPane()
                .name("nav")
                .bounds(0, 5, 9, 1)
                .item(3, 0, PaginationUtils.previousPageButton("shop").build())
                .item(4, 0, PaginationUtils.pageIndicator("shop").build())
                .item(5, 0, PaginationUtils.nextPageButton("shop").build())
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
            new ShopItem("Cake", Material.CAKE, "food", 25),
            new ShopItem("Cookie", Material.COOKIE, "food", 3),

            // Blocks
            new ShopItem("Diamond Block", Material.DIAMOND_BLOCK, "block", 450),
            new ShopItem("Gold Block", Material.GOLD_BLOCK, "block", 350),
            new ShopItem("Iron Block", Material.IRON_BLOCK, "block", 200),
            new ShopItem("Emerald Block", Material.EMERALD_BLOCK, "block", 600),
            new ShopItem("Obsidian", Material.OBSIDIAN, "block", 80),
            new ShopItem("Ender Chest", Material.ENDER_CHEST, "block", 150),

            // Special Items
            new ShopItem("Elytra", Material.ELYTRA, "special", 1000),
            new ShopItem("Totem of Undying", Material.TOTEM_OF_UNDYING, "special", 750),
            new ShopItem("Nether Star", Material.NETHER_STAR, "special", 500),
            new ShopItem("Dragon Egg", Material.DRAGON_EGG, "special", 2000),
            new ShopItem("Beacon", Material.BEACON, "special", 800),
            new ShopItem("Shulker Box", Material.SHULKER_BOX, "special", 200)
        );
    }
}
