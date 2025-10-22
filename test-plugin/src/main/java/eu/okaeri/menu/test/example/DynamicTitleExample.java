package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Examples demonstrating dynamic menu titles.
 * Shows different patterns for reactive titles that update automatically.
 */
public class DynamicTitleExample {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a menu with a countdown timer in the title.
     * Demonstrates stateful dynamic titles.
     */
    public static Menu createCountdownMenu(Plugin plugin, int startSeconds) {
        AtomicInteger countdown = new AtomicInteger(startSeconds);

        return Menu.builder(plugin)
            .title(() -> {
                int remaining = countdown.get();
                if (remaining > 0) {
                    return "<yellow>Time Remaining: <red>" + remaining + "s";
                } else {
                    return "<red><b>TIME'S UP!";
                }
            })
            .rows(3)
            .updateInterval(Duration.ofSeconds(1))
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, MenuItem.item()
                    .material(() -> {
                        int remaining = countdown.get();
                        if (remaining > 10) return Material.LIME_CONCRETE;
                        if (remaining > 5) return Material.YELLOW_CONCRETE;
                        if (remaining > 0) return Material.ORANGE_CONCRETE;
                        return Material.RED_CONCRETE;
                    })
                    .name(() -> {
                        int remaining = countdown.getAndUpdate(n -> Math.max(0, n - 1));
                        if (remaining > 10) return "<green><b>" + remaining + " seconds";
                        if (remaining > 5) return "<yellow><b>" + remaining + " seconds";
                        if (remaining > 0) return "<red><b>" + remaining + " seconds";
                        return "<dark_red><b>EXPIRED";
                    })
                    .lore("""
                        <gray>Watch the countdown in the title
                        <gray>and in this item!
                        
                        <yellow>The color changes as time runs out.""")
                    .build())
                .build())
            .build();
    }

    /**
     * Creates a menu with a progress bar in the title.
     * Useful for tasks like downloads, processing, etc.
     */
    public static Menu createProgressBarMenu(Plugin plugin) {
        AtomicInteger progress = new AtomicInteger(0);

        return Menu.builder(plugin)
            .title(() -> {
                int percent = progress.get();
                int bars = percent / 10;  // 10 bars total
                StringBuilder bar = new StringBuilder("<yellow>Progress: <white>[");

                for (int i = 0; i < 10; i++) {
                    if (i < bars) {
                        bar.append("<green>█");
                    } else {
                        bar.append("<gray>█");
                    }
                }
                bar.append("<white>] <yellow>").append(percent).append("%");
                return bar.toString();
            })
            .rows(3)
            .updateInterval(Duration.ofMillis(500))
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(3, 1, MenuItem.item()
                    .material(Material.RED_WOOL)
                    .name("<red><b>Decrease Progress")
                    .lore("""
                        <gray>Click to decrease progress
                        <gray>by 10%
                        
                        <yellow>Click me!""")
                    .onClick(ctx -> {
                        progress.updateAndGet(n -> Math.max(0, n - 10));
                        ctx.refresh();
                    })
                    .build())
                .item(4, 1, MenuItem.item()
                    .material(Material.PAPER)
                    .name(() -> "<yellow><b>" + progress.get() + "% Complete")
                    .lore("""
                        <gray>Watch the progress bar
                        <gray>in the title update!
                        
                        <gray>Use the buttons to change it.""")
                    .build())
                .item(5, 1, MenuItem.item()
                    .material(Material.LIME_WOOL)
                    .name("<green><b>Increase Progress")
                    .lore("""
                        <gray>Click to increase progress
                        <gray>by 10%
                        
                        <yellow>Click me!""")
                    .onClick(ctx -> {
                        progress.updateAndGet(n -> Math.min(100, n + 10));
                        ctx.refresh();
                    })
                    .build())
                .build())
            .build();
    }

    /**
     * Creates a menu showing date and time with MiniMessage formatting.
     */
    public static Menu createDateTimeMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title(() -> {
                LocalDateTime now = LocalDateTime.now();
                return "<gradient:yellow:gold>" + now.format(DATE_FORMAT);
            })
            .rows(3)
            .updateInterval(Duration.ofSeconds(1))
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, MenuItem.item()
                    .material(Material.CLOCK)
                    .name("<gradient:yellow:gold><b>Current Date & Time")
                    .lore("""
                            <gray>Date: <white><date>
                            <gray>Time: <white><time>
                            <gray>Day of Week: <white><day>
                            
                            <gray>Updates every second!""",
                        ctx -> {
                            LocalDateTime now = LocalDateTime.now();
                            return Map.of(
                                "date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                "day", now.getDayOfWeek().toString()
                            );
                        })
                    .build())
                .build())
            .build();
    }
}
