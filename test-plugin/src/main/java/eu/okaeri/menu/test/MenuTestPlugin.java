package eu.okaeri.menu.test;

import eu.okaeri.menu.test.example.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * Test plugin for manual testing of the menu system.
 * Provides commands to open all example menus.
 */
public class MenuTestPlugin extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Sends a colored message to a player.
     * Supports § and & color codes.
     */
    private static void sendColoredMessage(Player player, String message) {
        Component component = LEGACY_AMPERSAND.deserialize(message);
        player.sendMessage(component);
    }

    @Override
    public void onEnable() {
        getLogger().info("MenuTestPlugin enabled! Use /menu to see examples.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("&cOnly players can use this command!");
            return true;
        }

        if (!command.getName().equalsIgnoreCase("menu")) {
            return false;
        }

        if (args.length == 0) {
            sendColoredMessage(player, "&eAvailable examples:");
            sendColoredMessage(player, "&7  /menu simple - Simple menu");
            sendColoredMessage(player, "&7  /menu pane - Multi-pane layout");
            sendColoredMessage(player, "&7  /menu reactive - Reactive properties");
            sendColoredMessage(player, "&7  /menu autoupdate - Auto-updating menu");
            sendColoredMessage(player, "&7  /menu nav - Navigation");
            sendColoredMessage(player, "&7  /menu pagination - Pagination");
            sendColoredMessage(player, "&7  /menu browser - Player browser");
            sendColoredMessage(player, "&7  /menu shop - Shop with filters");
            sendColoredMessage(player, "&7  /menu vars - Item-level variables");
            sendColoredMessage(player, "&7  /menu strategy - Filter strategies (AND/OR)");
            sendColoredMessage(player, "&7  /menu minimessage - MiniMessage formatting");
            sendColoredMessage(player, "&7  /menu legacy - Legacy colors");
            sendColoredMessage(player, "&7  /menu i18n - i18n messages");
            sendColoredMessage(player, "&e&lInteractive Slots:");
            sendColoredMessage(player, "&7  /menu deposit - Deposit box");
            sendColoredMessage(player, "&7  /menu sell - Sell shop");
            sendColoredMessage(player, "&7  /menu processor - Item processor");
            sendColoredMessage(player, "&7  /menu equipment - Equipment menu");
            sendColoredMessage(player, "&7  /menu validation - Validation example");
            sendColoredMessage(player, "&e&lDynamic Title Examples:");
            sendColoredMessage(player, "&7  /menu countdown [seconds] - Countdown timer");
            sendColoredMessage(player, "&7  /menu progress - Progress bar title");
            sendColoredMessage(player, "&7  /menu datetime - Date/time title");
            sendColoredMessage(player, "&e&lAsync Examples:");
            sendColoredMessage(player, "&7  /menu async - Async shop with loading states");
            sendColoredMessage(player, "&7  /menu wait - Wait-for-data mode (legacy behavior)");
            sendColoredMessage(player, "&7  /menu waitimmediate - Immediate mode comparison");
            return true;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "simple" -> {
                    SimpleMenuExample.createSimpleMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened simple menu");
                }
                case "pane" -> {
                    SimpleMenuExample.createPaneExample(this).open(player);
                    sendColoredMessage(player, "&aOpened pane example");
                }
                case "reactive" -> {
                    SimpleMenuExample.createReactiveExample(this, player).open(player);
                    sendColoredMessage(player, "&aOpened reactive example");
                }
                case "autoupdate" -> {
                    SimpleMenuExample.createAutoUpdateExample(this).open(player);
                    sendColoredMessage(player, "&aOpened auto-update example");
                }
                case "nav", "navigation" -> {
                    NavigationExample.createMainMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened navigation example");
                }
                case "pagination" -> {
                    PaginationExample.createSimplePaginatedMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened pagination example");
                }
                case "browser" -> {
                    PaginationExample.createPlayerBrowser(this).open(player);
                    sendColoredMessage(player, "&aOpened player browser");
                }
                case "shop" -> {
                    ShopExample.createShopMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened shop example");
                }
                case "vars", "itemvars" -> {
                    ItemVarsExample.createItemVarsMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened item-level variables example");
                }
                case "strategy", "filterstrategy" -> {
                    FilterStrategyExample.createFilterStrategyMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened filter strategy example");
                }
                case "minimessage", "mm" -> {
                    MessageExample.createMiniMessageExample(this).open(player);
                    sendColoredMessage(player, "&aOpened MiniMessage example");
                }
                case "legacy" -> {
                    MessageExample.createLegacyColorsExample(this).open(player);
                    sendColoredMessage(player, "&aOpened legacy colors example");
                }
                case "i18n" -> {
                    MessageExample.createI18nExample(this).open(player);
                    sendColoredMessage(player, "&aOpened i18n example");
                }
                case "mixed" -> {
                    MessageExample.createMixedFormatsExample(this).open(player);
                    sendColoredMessage(player, "&aOpened mixed formats example");
                }
                case "reactive-msg" -> {
                    MessageExample.createReactiveExample(this).open(player);
                    sendColoredMessage(player, "&aOpened reactive messages example");
                }
                case "papi", "placeholderapi" -> {
                    MessageExample.createPlaceholderAPIExample(this).open(player);
                    sendColoredMessage(player, "&aOpened PlaceholderAPI example");
                }
                case "deposit" -> {
                    InteractiveSlotExample.createDepositBoxMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened deposit box menu");
                }
                case "sell" -> {
                    InteractiveSlotExample.createSellShopMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened sell shop menu");
                }
                case "processor" -> {
                    InteractiveSlotExample.createProcessorMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened item processor menu");
                }
                case "equipment" -> {
                    InteractiveSlotExample.createEquipmentMenu(this, player).open(player);
                    sendColoredMessage(player, "&aOpened equipment menu");
                }
                case "validation" -> {
                    InteractiveSlotExample.createValidationMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened validation example menu");
                }
                case "countdown" -> {
                    int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 60;
                    DynamicTitleExample.createCountdownMenu(this, seconds).open(player);
                    sendColoredMessage(player, "&aOpened countdown menu (" + seconds + " seconds)");
                }
                case "progress", "progressbar" -> {
                    DynamicTitleExample.createProgressBarMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened progress bar menu");
                }
                case "datetime" -> {
                    DynamicTitleExample.createDateTimeMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened date/time menu");
                }
                case "async" -> {
                    AsyncShopExample.createAsyncShopMenu(this, player).open(player);
                    sendColoredMessage(player, "&aOpened async shop menu");
                    sendColoredMessage(player, "&7Watch the loading states and async balance!");
                }
                case "wait" -> {
                    WaitForDataExample.createAsyncShopMenu(this).open(player, Duration.ofSeconds(3));
                    sendColoredMessage(player, "&aOpened menu in WAIT mode");
                    sendColoredMessage(player, "&7Menu opened after data loaded (legacy behavior)");
                }
                case "waitimmediate" -> {
                    WaitForDataExample.createAsyncShopMenu(this).open(player);
                    sendColoredMessage(player, "&aOpened menu in IMMEDIATE mode");
                    sendColoredMessage(player, "&7Menu shows loading states (modern behavior)");
                }
                default -> {
                    sendColoredMessage(player, "&cUnknown example: " + args[0]);
                    sendColoredMessage(player, "&7Use /menu to see available examples");
                }
            }
        } catch (Exception e) {
            sendColoredMessage(player, "&cError opening menu: " + e.getMessage());
            getLogger().severe("Error opening menu:");
            e.printStackTrace();
        }

        return true;
    }
}
