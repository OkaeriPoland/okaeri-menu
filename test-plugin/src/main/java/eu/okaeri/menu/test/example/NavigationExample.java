package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.MenuItem;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pane.StaticPane;
import org.bukkit.Material;

/**
 * Examples demonstrating the navigation system (Phase 2).
 * Shows how to create menus that can navigate between each other with back buttons.
 */
public class NavigationExample {

    /**
     * Creates a main menu with navigation to sub-menus.
     */
    public static Menu createMainMenu() {
        return Menu.builder()
            .title("Main Menu")
            .rows(3)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Navigate to Shop
                .item(1, 1, MenuItem.builder()
                    .material(Material.EMERALD)
                    .name("&a&lShop")
                    .lore("""
                        &7Browse items for sale

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createShopMenu());
                    })
                    .build())
                // Navigate to Settings
                .item(4, 1, MenuItem.builder()
                    .material(Material.COMPARATOR)
                    .name("&b&lSettings")
                    .lore("""
                        &7Configure your preferences

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createSettingsMenu());
                    })
                    .build())
                // Navigate to Profile
                .item(7, 1, MenuItem.builder()
                    .material(Material.PLAYER_HEAD)
                    .name("&d&lProfile")
                    .lore("""
                        &7View your statistics

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createProfileMenu());
                    })
                    .build())
                // Close button (no back button since this is main menu)
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates a shop menu with categories.
     */
    public static Menu createShopMenu() {
        return Menu.builder()
            .title("&a&lShop")
            .rows(4)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 4)
                // Shop categories
                .item(1, 1, MenuItem.builder()
                    .material(Material.DIAMOND_SWORD)
                    .name("&b&lWeapons")
                    .lore("""
                        &7Browse weapons

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createWeaponsMenu());
                    })
                    .build())
                .item(4, 1, MenuItem.builder()
                    .material(Material.DIAMOND_CHESTPLATE)
                    .name("&9&lArmor")
                    .lore("""
                        &7Browse armor

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createArmorMenu());
                    })
                    .build())
                .item(7, 1, MenuItem.builder()
                    .material(Material.GOLDEN_APPLE)
                    .name("&6&lConsumables")
                    .lore("""
                        &7Browse consumables

                        &eClick to open!""")
                    .onClick(ctx -> {
                        ctx.open(createConsumablesMenu());
                    })
                    .build())
                // Navigation: Back button (visible because we came from main menu)
                .item(0, 3, NavigationUtils.backButton().build())
                // Close button
                .item(8, 3, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates a weapons shop menu (3rd level).
     */
    public static Menu createWeaponsMenu() {
        return Menu.builder()
            .title("&b&lWeapons Shop")
            .rows(4)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 4)
                // Weapons
                .item(1, 1, MenuItem.builder()
                    .material(Material.DIAMOND_SWORD)
                    .name("&b&lDiamond Sword")
                    .lore("""
                        &7Damage: &c+7
                        &7Price: &6100 coins

                        &eClick to purchase!""")
                    .onClick(ctx -> {
                        ctx.sendMessage("&aPurchased Diamond Sword!");
                    })
                    .build())
                .item(2, 1, MenuItem.builder()
                    .material(Material.IRON_SWORD)
                    .name("&f&lIron Sword")
                    .lore("""
                        &7Damage: &c+6
                        &7Price: &650 coins

                        &eClick to purchase!""")
                    .onClick(ctx -> {
                        ctx.sendMessage("&aPurchased Iron Sword!");
                    })
                    .build())
                .item(3, 1, MenuItem.builder()
                    .material(Material.BOW)
                    .name("&e&lBow")
                    .lore("""
                        &7Range Weapon
                        &7Price: &675 coins

                        &eClick to purchase!""")
                    .onClick(ctx -> {
                        ctx.sendMessage("&aPurchased Bow!");
                    })
                    .build())
                // Navigation: Back button (visible, goes to Shop menu)
                .item(0, 3, NavigationUtils.backButton().build())
                // Close button
                .item(8, 3, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates an armor shop menu (3rd level).
     */
    public static Menu createArmorMenu() {
        return Menu.builder()
            .title("&9&lArmor Shop")
            .rows(4)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 4)
                // Armor pieces
                .item(1, 1, MenuItem.builder()
                    .material(Material.DIAMOND_HELMET)
                    .name("&b&lDiamond Helmet")
                    .lore("""
                        &7Defense: &a+3
                        &7Price: &6150 coins""")
                    .build())
                .item(2, 1, MenuItem.builder()
                    .material(Material.DIAMOND_CHESTPLATE)
                    .name("&b&lDiamond Chestplate")
                    .lore("""
                        &7Defense: &a+8
                        &7Price: &6300 coins""")
                    .build())
                .item(3, 1, MenuItem.builder()
                    .material(Material.DIAMOND_LEGGINGS)
                    .name("&b&lDiamond Leggings")
                    .lore("""
                        &7Defense: &a+6
                        &7Price: &6250 coins""")
                    .build())
                .item(4, 1, MenuItem.builder()
                    .material(Material.DIAMOND_BOOTS)
                    .name("&b&lDiamond Boots")
                    .lore("""
                        &7Defense: &a+3
                        &7Price: &6150 coins""")
                    .build())
                // Navigation
                .item(0, 3, NavigationUtils.backButton().build())
                .item(8, 3, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates a consumables shop menu (3rd level).
     */
    public static Menu createConsumablesMenu() {
        return Menu.builder()
            .title("&6&lConsumables Shop")
            .rows(4)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 4)
                // Consumables
                .item(1, 1, MenuItem.builder()
                    .material(Material.GOLDEN_APPLE)
                    .name("&6&lGolden Apple")
                    .lore("""
                        &7Restores &c4 hearts
                        &7Price: &625 coins""")
                    .build())
                .item(2, 1, MenuItem.builder()
                    .material(Material.COOKED_BEEF)
                    .name("&e&lSteak")
                    .lore("""
                        &7Restores &c4 hunger
                        &7Price: &65 coins""")
                    .build())
                .item(3, 1, MenuItem.builder()
                    .material(Material.POTION)
                    .name("&d&lHealing Potion")
                    .lore("""
                        &7Instant healing
                        &7Price: &650 coins""")
                    .build())
                // Navigation
                .item(0, 3, NavigationUtils.backButton().build())
                .item(8, 3, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates a settings menu.
     */
    public static Menu createSettingsMenu() {
        return Menu.builder()
            .title("&b&lSettings")
            .rows(3)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, MenuItem.builder()
                    .material(Material.COMPARATOR)
                    .name("&b&lSettings")
                    .lore("""
                        &7This is the settings menu
                        &7(Work in progress)""")
                    .build())
                // Navigation
                .item(0, 2, NavigationUtils.backButton().build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Creates a profile menu.
     */
    public static Menu createProfileMenu() {
        return Menu.builder()
            .title("&d&lYour Profile")
            .rows(3)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, MenuItem.builder()
                    .material(Material.PLAYER_HEAD)
                    .name(ctx -> "&d&l" + ctx.getEntity().getName())
                    .lore("""
                        &7Level: &a42
                        &7Coins: &6500
                        &7Playtime: &e12h 34m""")
                    .build())
                // Navigation
                .item(0, 2, NavigationUtils.backButton().build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example showing conditional back button visibility.
     * The back button only appears if there's navigation history.
     */
    public static Menu createConditionalBackButtonExample() {
        return Menu.builder()
            .title("Conditional Back Button")
            .rows(3)
            .pane("main", StaticPane.builder()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, MenuItem.builder()
                    .material(Material.COMPASS)
                    .name("&e&lNavigation Info")
                    .lore("""
                        &7Has Previous Menu: &f<has_last>
                        &7Navigation Depth: &f<depth>

                        &7The back button only appears
                        &7when you have navigation history!""",
                        ctx -> java.util.Map.of(
                            "has_last", ctx.hasLast() ? "Yes" : "No",
                            "depth", ctx.navigationDepth()
                        ))
                    .build())
                // Conditional back button - automatically hidden if no history
                .item(0, 2, NavigationUtils.backButton()
                    .lore("""
                        &7Go back to the previous menu

                        &eClick to go back!""")
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }
}
