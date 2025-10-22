package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Simple example demonstrating the new menu system.
 * This shows Phase 1 functionality: Pane-based architecture with reactive properties.
 */
public class SimpleMenuExample {

    public static Menu createSimpleMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("Simple Menu")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)  // Full 3-row inventory
                .item(0, 0, item()
                    .material(Material.DIAMOND)
                    .name("Static Diamond")
                    .onClick(ctx -> ctx.sendMessage("You clicked a diamond!"))
                    .build())
                .item(1, 0, item()
                    .material(Material.GOLD_INGOT)
                    .name(() -> "Dynamic: " + System.currentTimeMillis())  // Reactive!
                    .onClick(ctx -> {
                        ctx.sendMessage("Refreshing menu...");
                        ctx.refresh();
                    })
                    .build())
                .item(8, 2, item()
                    .material(Material.BARRIER)
                    .name("Close")
                    .onClick(ctx -> ctx.closeInventory())
                    .build())
                .build())
            .build();
    }

    public static Menu createPaneExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("Multi-Pane Example")
            .rows(6)
            // Top navigation pane
            .pane("nav", staticPane()
                .name("nav")
                .bounds(0, 0, 9, 1)
                .item(4, 0, item()
                    .material(Material.COMPASS)
                    .name("Navigation")
                    .build())
                .item(8, 0, item()
                    .material(Material.BARRIER)
                    .name("Close")
                    .onClick(ctx -> ctx.closeInventory())
                    .build())
                .build())
            // Content pane
            .pane("content", staticPane()
                .name("content")
                .bounds(0, 1, 9, 5)
                .item(4, 2, item()
                    .material(Material.EMERALD)
                    .name("Content Item")
                    .onClick(ctx -> {
                        ctx.sendMessage("Content clicked!");
                        ctx.refreshPane("content");  // Refresh only this pane
                    })
                    .build())
                .build())
            .build();
    }

    /**
     * Example with reactive properties.
     */
    public static Menu createReactiveExample(Plugin plugin, Player player) {
        // Some reactive state
        final int[] clickCount = {0};

        return Menu.builder(plugin)
            .title("Reactive Example")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, item()
                    .material(Material.DIAMOND)
                    .name(() -> "Clicks: " + clickCount[0])  // Reactive!
                    .amount(() -> Math.min(64, clickCount[0] + 1))  // Reactive amount!
                    .onClick(ctx -> {
                        clickCount[0]++;
                        ctx.refresh();  // Re-evaluate reactive properties
                    })
                    .build())
                .build())
            .build();
    }

    /**
     * Example with automatic update intervals.
     * Demonstrates reactive properties that change over time without manual refresh.
     */
    public static Menu createAutoUpdateExample(Plugin plugin) {
        Instant startTime = Instant.now();

        return Menu.builder(plugin)
            .title("Auto-Update Example")
            .rows(3)
            .updateInterval(Duration.ofSeconds(1))  // Update every second
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Clock display - updates automatically
                .item(1, 1, item()
                    .material(Material.CLOCK)
                    .name(() -> "Current Time")
                    .lore("""
                            &7Server: &f<millis>
                            &7Elapsed: &f<elapsed>s""",
                        ctx -> Map.of(
                            "millis", System.currentTimeMillis(),
                            "elapsed", Duration.between(startTime, Instant.now()).getSeconds()
                        ))
                    .build())
                // Online players count - updates automatically
                .item(4, 1, item()
                    .material(Material.PLAYER_HEAD)
                    .name(() -> "Online Players")
                    .amount(() -> Math.max(1, Bukkit.getOnlinePlayers().size()))  // Min 1 for visibility
                    .lore("""
                            &7Count: &f<count>
                            &7Updates every second""",
                        ctx -> Map.of("count", Bukkit.getOnlinePlayers().size()))
                    .build())
                // Dynamic material - cycles through colors
                .item(7, 1, item()
                    .material(() -> {
                        long seconds = Duration.between(startTime, Instant.now()).getSeconds();
                        Material[] materials = {
                            Material.RED_WOOL,
                            Material.ORANGE_WOOL,
                            Material.YELLOW_WOOL,
                            Material.LIME_WOOL,
                            Material.CYAN_WOOL,
                            Material.BLUE_WOOL,
                            Material.PURPLE_WOOL
                        };
                        return materials[(int) (seconds % materials.length)];
                    })
                    .name(() -> "Color Cycle")
                    .lore("""
                        &7This item changes color
                        &7every second automatically!""")
                    .build())
                // Close button
                .item(8, 2, item()
                    .material(Material.BARRIER)
                    .name("Close")
                    .onClick(ctx -> ctx.closeInventory())
                    .build())
                .build())
            .build();
    }
}
