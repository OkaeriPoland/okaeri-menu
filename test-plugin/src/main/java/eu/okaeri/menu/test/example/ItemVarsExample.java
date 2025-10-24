package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;

/**
 * Examples demonstrating item-level variables (.vars()).
 * Variables defined once at item level are shared across name(), lore(), etc.
 */
public class ItemVarsExample {

    /**
     * Item-level variables shared across name and lore.
     * Variables defined once with .vars() are automatically available in name(), lore(), etc.
     */
    public static Menu createItemVarsMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("&6Item-Level Variables")
            .rows(3)
            .pane("main", StaticPane.staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Example 1: Simple vars shared between name and lore
                .item(1, 1, item()
                    .material(Material.DIAMOND)
                    .vars(Map.of(
                        "price", 100,
                        "currency", "coins"
                    ))
                    .name("&bDiamond - <price> <currency>")
                    .lore("""
                        &7Price: &f<price> <currency>
                        &7Click to purchase!""")
                    .onClick(ctx -> ctx.sendMessage("&aPurchased for " + 100 + " coins!"))
                    .build())
                // Example 2: Reactive vars with context-aware vars
                .item(3, 1, item()
                    .material(Material.CLOCK)
                    .vars(ctx -> Map.of("count", ctx.getInt("counter")))  // Context-aware vars!
                    .name("&eCounter: <count>")
                    .lore("""
                        &7Current count: &f<count>

                        &eClick to increment!""")
                    .onClick(ctx -> {
                        ctx.set("counter", ctx.getInt("counter") + 1);
                        ctx.refresh();
                    })
                    .build())
                // Example 3: Method-level vars override item-level vars
                .item(5, 1, item()
                    .material(Material.EMERALD)
                    .vars(Map.of(
                        "item", "Emerald",
                        "price", 50
                    ))
                    .name("&a<item>")
                    .lore("""
                            &7Default price: &f<price> coins
                            &7Sale price: &f<price> coins
                            
                            &eSale is active!""",
                        Map.of("price", 25))  // Override item-level price for lore
                    .build())
                // Example 4: Multiple placeholders from item-level vars
                .item(7, 1, item()
                    .material(Material.ENCHANTED_BOOK)
                    .vars(Map.of(
                        "name", "Sharpness V",
                        "type", "Enchantment",
                        "rarity", "Epic",
                        "price", 250
                    ))
                    .name("&d<name>")
                    .lore("""
                        &7Type: &f<type>
                        &7Rarity: &d<rarity>
                        &7Price: &6<price> coins
                        
                        &eClick to purchase!""")
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }
}
