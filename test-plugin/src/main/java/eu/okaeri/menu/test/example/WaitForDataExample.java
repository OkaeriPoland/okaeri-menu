package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Arrays;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.AsyncPaginatedPane.paneAsync;

/**
 * Example demonstrating wait-for-data menu opening.
 * Shows the difference between immediate opening (with loading states)
 * and delayed opening (waiting for data).
 * <p>
 * Usage:
 * - /menu wait - Opens after data loads (legacy behavior)
 * - /menu waitimmediate - Opens immediately with loading states (modern behavior)
 */
public class WaitForDataExample {

    /**
     * Creates a menu with async data loading.
     * Use menu.open(player) for immediate opening or
     * menu.open(player, Duration.ofSeconds(3)) for wait-for-data mode.
     */
    public static Menu createAsyncShopMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("Async Shop Example")
            .rows(3)
            .pane("items", paneAsync(String.class)
                .name("items")
                .bounds(0, 0, 9, 3)
                .loader(() -> {
                    // Simulate database/API call with 500ms delay
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Arrays.asList(
                        "Diamond Sword",
                        "Iron Armor",
                        "Golden Apple",
                        "Enchanted Book",
                        "Potion of Strength"
                    );
                })
                .renderer((itemName, index) -> item()
                    .material(getMaterialForItem(itemName))
                    .name("&e" + itemName)
                    .lore("&7Click to purchase!")
                    .build())
                .ttl(Duration.ofSeconds(60))
                .build())
            .build();
    }

    private static Material getMaterialForItem(String itemName) {
        return switch (itemName) {
            case "Diamond Sword" -> Material.DIAMOND_SWORD;
            case "Iron Armor" -> Material.IRON_CHESTPLATE;
            case "Golden Apple" -> Material.GOLDEN_APPLE;
            case "Enchanted Book" -> Material.ENCHANTED_BOOK;
            case "Potion of Strength" -> Material.POTION;
            default -> Material.STONE;
        };
    }
}
