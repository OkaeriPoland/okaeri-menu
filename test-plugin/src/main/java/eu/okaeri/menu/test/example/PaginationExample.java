package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pagination.PaginationFilter;
import eu.okaeri.menu.pagination.PaginationUtils;
import eu.okaeri.menu.pane.PaginatedPane;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Examples demonstrating the pagination system (Phase 3).
 * Shows how to create menus with paginated content, filtering, and navigation.
 */
public class PaginationExample {

    /**
     * Simple example: Paginate a list of strings.
     */
    public static Menu createSimplePaginatedMenu(Plugin plugin) {
        // Create some sample data
        List<String> items = Arrays.asList(
            "Apple", "Apricot", "Avocado", "Banana", "Blackberry",
            "Blueberry", "Cherry", "Coconut", "Cranberry", "Date",
            "Dragonfruit", "Elderberry", "Fig", "Grapefruit", "Grape",
            "Guava", "Honeydew", "Jackfruit", "Kiwi", "Kumquat",
            "Lemon", "Lime", "Lychee", "Mango", "Mandarin",
            "Mulberry", "Nectarine", "Orange", "Papaya", "Passionfruit",
            "Peach", "Pear", "Persimmon", "Pineapple", "Plum",
            "Pomegranate", "Quince", "Raspberry", "Starfruit", "Strawberry",
            "Tangerine", "Watermelon", "Acai", "Boysenberry", "Cantaloupe",
            "Clementine", "Currant", "Gooseberry", "Huckleberry", "Jabuticaba",
            "Longan", "Loquat", "Rambutan", "Soursop", "Tamarind"
        );

        return Menu.builder(plugin)
            .title("&6Fruit List")
            .rows(6)
            .pane(PaginatedPane.<String>pane()
                .name("content")
                .bounds(0, 0, 9, 5)  // 5 rows for content
                .items(items)
                // itemsPerPage defaults to pane size (9x5 = 45 slots)
                .renderer((ctx, fruit, index) -> item()
                    .material(Material.APPLE)  // Could map different materials
                    .name("&e" + fruit)
                    .lore("""
                        &7Index: &f%d
                        
                        &eClick for more info!""".formatted(index))
                    .onClick(event -> {
                        event.sendMessage("&aYou selected: " + fruit);
                    })
                    .build())
                .build())
            // Navigation row (bottom row)
            .pane(staticPane()
                .name("nav")
                .bounds(0, 5, 9, 1)
                .item(3, 0, PaginationUtils.previousPageButton("content").build())
                .item(4, 0, PaginationUtils.pageIndicator("content").build())
                .item(5, 0, PaginationUtils.nextPageButton("content").build())
                .item(8, 0, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example with static items in paginated pane.
     * Shows how to reserve slots for buttons within the paginated area.
     */
    public static Menu createPaginatedWithStaticItems(Plugin plugin) {
        List<String> players = Arrays.asList(
            "Player1", "Player2", "Player3", "Player4", "Player5",
            "Player6", "Player7", "Player8", "Player9", "Player10",
            "Player11", "Player12", "Player13", "Player14", "Player15"
        );

        return Menu.builder(plugin)
            .title("&bPlayer List")
            .rows(4)
            .pane(PaginatedPane.<String>pane()
                .name("players")
                .bounds(0, 0, 9, 4)
                .items(players)
                .renderer((ctx, playerName, index) -> item()
                    .material(Material.PLAYER_HEAD)
                    .name("&a" + playerName)
                    .lore("""
                        &7Status: &aOnline
                        
                        &eClick to view profile!""")
                    .build())
                // Static buttons in the paginated area
                .staticItem(7, 3, PaginationUtils.previousPageButton("players").build())
                .staticItem(8, 3, PaginationUtils.nextPageButton("players").build())
                .build())
            .build();
    }

    /**
     * Advanced example: Player profile browser with search/filter.
     */
    public static Menu createPlayerBrowser(Plugin plugin) {
        List<PlayerProfile> profiles = createPlayerProfiles();

        return Menu.builder(plugin)
            .title("&d&lPlayer Browser")
            .rows(6)
            // Filter controls
            .pane(staticPane()
                .name("filters")
                .bounds(0, 0, 9, 1)
                .item(1, 0, PaginationUtils.<PlayerProfile>filterButton(
                    "profiles",
                    "online",
                    "&aOnline Only",
                    profile -> profile.isOnline()
                ).build())
                .item(2, 0, PaginationUtils.<PlayerProfile>filterButton(
                    "profiles",
                    "vip",
                    "&6VIP Players",
                    profile -> profile.isVip()
                ).build())
                .item(3, 0, PaginationUtils.<PlayerProfile>filterButton(
                    "profiles",
                    "highlevel",
                    "&bLevel 50+",
                    PaginationFilter.min(PlayerProfile::getLevel, 50)
                ).build())
                .item(7, 0, PaginationUtils.clearFiltersButton("profiles").build())
                .item(8, 0, PaginationUtils.emptyIndicator("profiles").build())
                .build())
            // Player list
            .pane(PaginatedPane.<PlayerProfile>pane()
                .name("profiles")
                .bounds(0, 1, 9, 4)
                .items(profiles)
                // itemsPerPage defaults to pane size (9x4 = 36 slots)
                .renderer((ctx, profile, index) -> item()
                    .material(profile.isOnline() ? Material.LIME_WOOL : Material.GRAY_WOOL)
                    .name((profile.isOnline() ? "&a" : "&7") + profile.getName())
                    .lore("""
                        &7Level: &f%d
                        &7Status: %s
                        %s
                        
                        &eClick for details!""".formatted(
                        profile.getLevel(),
                        profile.isOnline() ? "&aOnline" : "&7Offline",
                        profile.isVip() ? "&6VIP Member" : "&7Regular Member"
                    ))
                    .glint(profile.isVip())
                    .build())
                .build())
            // Navigation
            .pane(staticPane()
                .name("nav")
                .bounds(0, 5, 9, 1)
                .item(3, 0, PaginationUtils.previousPageButton("profiles").build())
                .item(4, 0, PaginationUtils.pageIndicator("profiles").build())
                .item(5, 0, PaginationUtils.nextPageButton("profiles").build())
                .item(8, 0, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    // ========================================
    // HELPER CLASSES
    // ========================================

    @Getter
    @AllArgsConstructor
    public static class PlayerProfile {
        private final String name;
        private final int level;
        private final boolean online;
        private final boolean vip;
    }

    private static List<PlayerProfile> createPlayerProfiles() {
        List<PlayerProfile> profiles = new ArrayList<>();

        profiles.add(new PlayerProfile("Alice", 45, true, false));
        profiles.add(new PlayerProfile("Bob", 67, true, true));
        profiles.add(new PlayerProfile("Charlie", 23, false, false));
        profiles.add(new PlayerProfile("Diana", 89, true, true));
        profiles.add(new PlayerProfile("Eve", 12, false, false));
        profiles.add(new PlayerProfile("Frank", 56, true, false));
        profiles.add(new PlayerProfile("Grace", 78, false, true));
        profiles.add(new PlayerProfile("Henry", 34, true, false));
        profiles.add(new PlayerProfile("Iris", 90, true, true));
        profiles.add(new PlayerProfile("Jack", 15, false, false));
        profiles.add(new PlayerProfile("Kelly", 42, true, false));
        profiles.add(new PlayerProfile("Liam", 58, true, true));
        profiles.add(new PlayerProfile("Maya", 31, false, false));
        profiles.add(new PlayerProfile("Nathan", 74, true, false));
        profiles.add(new PlayerProfile("Olivia", 19, false, false));
        profiles.add(new PlayerProfile("Peter", 63, true, true));
        profiles.add(new PlayerProfile("Quinn", 27, true, false));
        profiles.add(new PlayerProfile("Rachel", 85, false, true));
        profiles.add(new PlayerProfile("Sam", 38, true, false));
        profiles.add(new PlayerProfile("Tara", 52, true, true));
        profiles.add(new PlayerProfile("Uma", 11, false, false));
        profiles.add(new PlayerProfile("Victor", 69, true, false));
        profiles.add(new PlayerProfile("Wendy", 44, false, false));
        profiles.add(new PlayerProfile("Xavier", 91, true, true));
        profiles.add(new PlayerProfile("Yuki", 25, true, false));
        profiles.add(new PlayerProfile("Zara", 77, false, true));
        profiles.add(new PlayerProfile("Adam", 33, true, false));
        profiles.add(new PlayerProfile("Bella", 48, true, false));
        profiles.add(new PlayerProfile("Carlos", 62, false, true));
        profiles.add(new PlayerProfile("Daphne", 29, true, false));
        profiles.add(new PlayerProfile("Ethan", 54, true, true));
        profiles.add(new PlayerProfile("Fiona", 17, false, false));
        profiles.add(new PlayerProfile("George", 71, true, false));
        profiles.add(new PlayerProfile("Hannah", 39, true, true));
        profiles.add(new PlayerProfile("Isaac", 81, false, false));
        profiles.add(new PlayerProfile("Julia", 26, true, false));
        profiles.add(new PlayerProfile("Kyle", 55, true, true));
        profiles.add(new PlayerProfile("Luna", 13, false, false));
        profiles.add(new PlayerProfile("Mason", 68, true, false));
        profiles.add(new PlayerProfile("Nina", 46, false, true));
        profiles.add(new PlayerProfile("Oscar", 92, true, true));
        profiles.add(new PlayerProfile("Paige", 21, true, false));
        profiles.add(new PlayerProfile("Quincy", 59, false, false));
        profiles.add(new PlayerProfile("Riley", 35, true, false));
        profiles.add(new PlayerProfile("Sophia", 73, true, true));
        profiles.add(new PlayerProfile("Tyler", 28, false, false));
        profiles.add(new PlayerProfile("Ursula", 64, true, false));
        profiles.add(new PlayerProfile("Violet", 41, true, true));
        profiles.add(new PlayerProfile("Wade", 16, false, false));
        profiles.add(new PlayerProfile("Xena", 87, true, true));

        return profiles;
    }
}
