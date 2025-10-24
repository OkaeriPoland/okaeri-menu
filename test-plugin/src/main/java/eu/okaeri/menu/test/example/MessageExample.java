package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.message.DefaultMessageProvider;
import eu.okaeri.menu.message.MessageProvider;
import eu.okaeri.menu.navigation.NavigationUtils;
import eu.okaeri.menu.pane.PaginatedPane;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Examples demonstrating the message system (Phase 4).
 * Shows how to use DefaultMessageProvider with universal format support (§, &, MiniMessage).
 */
public class MessageExample {

    /**
     * Example using MiniMessage formatting.
     * DefaultMessageProvider supports MiniMessage tags like <red>, <gradient>, <bold>, etc.
     */
    public static Menu createMiniMessageExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<gradient:red:blue>MiniMessage Example</gradient>")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Simple colors
                .item(0, 1, item()
                    .material(Material.RED_WOOL)
                    .name("<red>Red Text")
                    .lore("""
                        <gray>This uses MiniMessage
                        <gray>for text formatting!""")
                    .build())
                // Gradient
                .item(2, 1, item()
                    .material(Material.NETHER_STAR)
                    .name("<gradient:red:blue>Rainbow Gradient</gradient>")
                    .lore("""
                        <gray>MiniMessage supports
                        <gradient:gold:yellow>beautiful gradients!</gradient>""")
                    .build())
                // Formatting
                .item(4, 1, item()
                    .material(Material.DIAMOND)
                    .name("<bold><blue>Bold & Blue")
                    .lore("""
                        <italic>Italic
                        <underlined>Underlined
                        <strikethrough>Strikethrough
                        <obfuscated>Obfuscated""")
                    .build())
                // Placeholders with reactive values
                .item(6, 1, item()
                    .material(Material.CLOCK)
                    .name(
                        "<gold>Time: <time>",
                        ctx -> Map.of("time", System.currentTimeMillis())
                    )
                    .lore("""
                            <gray>Current: <white><time>
                            <gray>This updates on refresh!""",
                        ctx -> Map.of("time", System.currentTimeMillis())
                    )
                    .onClick(ctx -> ctx.refresh())
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example using legacy color codes (§ and &).
     * DefaultMessageProvider automatically handles both formats.
     */
    public static Menu createLegacyColorsExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("§6Legacy Colors")  // § codes work
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(2, 1, item()
                    .material(Material.REDSTONE)
                    .name("§c§lRed Title")  // § codes
                    .lore("""
                        &7This uses legacy
                        §7color codes!
                        
                        &aGreen text
                        §9Blue text
                        &6Gold text""")
                    .build())
                .item(4, 1, item()
                    .material(Material.PAPER)
                    .name("&6Price: <price> coins", Map.of("price", "100"))  // Mixed & and placeholder
                    .lore("""
                            &7With placeholders:
                            §7Player: §f<player>
                            §7Balance: &6<balance>
                            
                            &eClick for details!""",
                        Map.of("player", "Steve", "balance", "500")
                    )
                    .build())
                .item(6, 1, item()
                    .material(Material.ENCHANTED_BOOK)
                    .name("§b&lFormatting Codes")  // Mix § and &
                    .lore("""
                        &l&aBold Green
                        &o&cItalic Red
                        &n§9Underline Blue
                        &m&7Strikethrough Gray
                        &k&dObfuscated Purple""")
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example mixing all formats: §, &, and MiniMessage.
     * DefaultMessageProvider handles all three simultaneously!
     */
    public static Menu createMixedFormatsExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("§6Mixed &b<gradient:red:blue>Formats</gradient>")  // All three!
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(1, 1, item()
                    .material(Material.EMERALD)
                    .name("§aLegacy §b<gradient:gold:yellow>MiniMessage</gradient>")
                    .lore("""
                        &7Mix § codes, & codes,
                        <gradient:red:blue>and MiniMessage tags!</gradient>""")
                    .build())
                .item(4, 1, item()
                    .material(Material.DIAMOND)
                    .name("<bold>&6Gold <gradient:blue:aqua>Gradient</gradient>")
                    .lore("""
                        §7All formats work together:
                        &a- Legacy § section codes
                        &b- Legacy & ampersand codes
                        <gradient:red:blue>- Modern MiniMessage tags</gradient>
                        
                        &eNo need to choose!""")
                    .build())
                .item(7, 1, item()
                    .material(Material.NETHER_STAR)
                    .name("§b<rainbow>Rainbow §6Gold &cRed")
                    .lore("""
                        &7The universal provider
                        <gradient:gold:yellow>handles everything!</gradient>""")
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example with reactive properties and dynamic messages.
     * Shows context-aware placeholders that update on refresh.
     */
    public static Menu createReactiveExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<gradient:blue:aqua>Reactive Messages</gradient>")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Reactive name with context-aware placeholders
                .item(3, 1, item()
                    .material(Material.DIAMOND)
                    .name(
                        "<gradient:blue:aqua>Clicks: <count></gradient>",
                        ctx -> Map.of("count", ctx.getInt("clickCount"))
                    )
                    .lore("""
                            <status>

                            &7Click to increment!""",
                        ctx -> {
                            int clickCount = ctx.getInt("clickCount");
                            String status = clickCount == 0 ? "<gray>Never clicked" :
                                clickCount < 5 ? "<yellow>Beginner" :
                                    clickCount < 10 ? "<gold>Intermediate" :
                                        "<red>Expert!";
                            return Map.of("status", status);
                        }
                    )
                    .onClick(ctx -> {
                        ctx.set("clickCount", ctx.getInt("clickCount") + 1);
                        ctx.refresh();
                    })
                    .build())
                // Dynamic color based on value
                .item(5, 1, item()
                    .material(ctx -> ctx.getInt("clickCount") > 5 ? Material.EMERALD : Material.COAL)
                    .name(
                        "<color>Status Indicator",
                        ctx -> {
                            String color = ctx.getInt("clickCount") > 5 ? "<green>" : "<gray>";
                            return Map.of("color", color);
                        }
                    )
                    .lore("""
                            <gray>Clicks: <white><count>
                            <gray>Material changes at 5+ clicks!""",
                        ctx -> Map.of("count", ctx.getInt("clickCount"))
                    )
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example using Map<Locale, String> for built-in i18n support.
     * MenuItem now supports locale maps for name and lore directly!
     */
    public static Menu createLocaleMapExample(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<gold>Multi-Language Shop")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                // Diamond Sword with locale-specific name and lore
                .item(1, 1, item()
                    .material(Material.DIAMOND_SWORD)
                    .vars(Map.of("damage", 10, "price", 150))
                    .name(Map.of(
                        Locale.ENGLISH, "Diamond Sword",
                        new Locale("pl"), "Diamentowy miecz",
                        new Locale("de"), "Diamantschwert",
                        new Locale("fr"), "Épée en diamant"
                    ))
                    .lore(Map.of(
                        Locale.ENGLISH, """
                            <gray>Damage: <red><damage>
                            <gray>Price: <gold><price> coins
                            
                            <yellow>Click to purchase!""",
                        new Locale("pl"), """
                            <gray>Obrażenia: <red><damage>
                            <gray>Cena: <gold><price> monet
                            
                            <yellow>Kliknij aby kupić!""",
                        new Locale("de"), """
                            <gray>Schaden: <red><damage>
                            <gray>Preis: <gold><price> Münzen
                            
                            <yellow>Klicken zum Kaufen!""",
                        new Locale("fr"), """
                            <gray>Dégâts: <red><damage>
                            <gray>Prix: <gold><price> pièces
                            
                            <yellow>Cliquez pour acheter!"""
                    ))
                    .onClick(ctx -> ctx.sendMessage("<green>Purchased Diamond Sword!"))
                    .build())
                // Health Potion with locale map and dynamic variables
                .item(3, 1, item()
                    .material(Material.POTION)
                    .vars(Map.of("healing", 4))
                    .name(Map.of(
                        Locale.ENGLISH, "Health Potion",
                        new Locale("pl"), "Mikstura zdrowia",
                        new Locale("de"), "Heiltrank",
                        new Locale("fr"), "Potion de santé"
                    ))
                    .lore(Map.of(
                        Locale.ENGLISH, """
                            <gray>Restores <red><healing> hearts
                            <gray>Price: <gold>50 coins
                            
                            <green>Ready to use!""",
                        new Locale("pl"), """
                            <gray>Przywraca <red><healing> serc
                            <gray>Cena: <gold>50 monet
                            
                            <green>Gotowe do użycia!""",
                        new Locale("de"), """
                            <gray>Stellt <red><healing> Herzen wieder her
                            <gray>Preis: <gold>50 Münzen
                            
                            <green>Einsatzbereit!""",
                        new Locale("fr"), """
                            <gray>Restaure <red><healing> cœurs
                            <gray>Prix: <gold>50 pièces
                            
                            <green>Prêt à utiliser!"""
                    ))
                    .build())
                // Golden Apple with method-level variable override
                .item(5, 1, item()
                    .material(Material.GOLDEN_APPLE)
                    .vars(Map.of("price", 100))  // Item-level price
                    .name(Map.of(
                        Locale.ENGLISH, "Golden Apple",
                        new Locale("pl"), "Złote jabłko",
                        new Locale("de"), "Goldener Apfel",
                        new Locale("fr"), "Pomme dorée"
                    ))
                    .lore(Map.of(
                        Locale.ENGLISH, """
                            <yellow>Rare item!
                            <gray>Price: <gold><price> coins
                            
                            <red>Sale: <sale> coins!""",
                        new Locale("pl"), """
                            <yellow>Rzadki przedmiot!
                            <gray>Cena: <gold><price> monet
                            
                            <red>Promocja: <sale> monet!""",
                        new Locale("de"), """
                            <yellow>Seltener Gegenstand!
                            <gray>Preis: <gold><price> Münzen
                            
                            <red>Angebot: <sale> Münzen!""",
                        new Locale("fr"), """
                            <yellow>Objet rare!
                            <gray>Prix: <gold><price> pièces
                            
                            <red>Solde: <sale> pièces!"""
                    ), Map.of("sale", 75))  // Method-level override for sale price
                    .build())
                // Info button
                .item(8, 1, item()
                    .material(Material.BOOK)
                    .name(Map.of(
                        Locale.ENGLISH, "<yellow>Language Info",
                        new Locale("pl"), "<yellow>Informacje o języku",
                        new Locale("de"), "<yellow>Sprachinformationen",
                        new Locale("fr"), "<yellow>Informations linguistiques"
                    ))
                    .lore(Map.of(
                        Locale.ENGLISH, """
                            <gray>This menu uses Map<Locale, String>
                            <gray>for automatic language selection!
                            
                            <aqua>Supported languages:
                            <white>- English
                            <white>- Polish (Polski)
                            <white>- German (Deutsch)
                            <white>- French (Français)
                            
                            <yellow>Your locale determines the text!""",
                        new Locale("pl"), """
                            <gray>To menu używa Map<Locale, String>
                            <gray>do automatycznego wyboru języka!
                            
                            <aqua>Obsługiwane języki:
                            <white>- Angielski (English)
                            <white>- Polski
                            <white>- Niemiecki (Deutsch)
                            <white>- Francuski (Français)
                            
                            <yellow>Twój język decyduje o tekście!""",
                        new Locale("de"), """
                            <gray>Dieses Menü verwendet Map<Locale, String>
                            <gray>für automatische Sprachauswahl!
                            
                            <aqua>Unterstützte Sprachen:
                            <white>- Englisch (English)
                            <white>- Polnisch (Polski)
                            <white>- Deutsch
                            <white>- Französisch (Français)
                            
                            <yellow>Ihre Sprache bestimmt den Text!""",
                        new Locale("fr"), """
                            <gray>Ce menu utilise Map<Locale, String>
                            <gray>pour sélection automatique de langue!
                            
                            <aqua>Langues supportées:
                            <white>- Anglais (English)
                            <white>- Polonais (Polski)
                            <white>- Allemand (Deutsch)
                            <white>- Français
                            
                            <yellow>Votre langue détermine le texte!"""
                    ))
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Example showing how to extend DefaultMessageProvider for custom i18n support.
     * This demonstrates the extensibility pattern for locale-based messages.
     */
    public static Menu createI18nExample(Plugin plugin) {
        // Create a custom i18n provider
        MessageProvider i18nProvider = new I18nMessageProvider();

        return Menu.builder(plugin)
            .title("<gold>i18n Example")
            .rows(3)
            .messageProvider(i18nProvider)  // Set custom provider at menu level
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(3, 1, item()
                    .material(Material.DIAMOND_SWORD)
                    .name("menu.shop.sword.name")  // Resolves via i18n
                    .lore("""
                            menu.shop.sword.lore.damage
                            menu.shop.sword.lore.price
                            
                            <yellow>Click to purchase!""",
                        Map.of("damage", "7", "price", "150")
                    )
                    .onClick(ctx -> ctx.sendMessage("&aPurchased Diamond Sword for 150 coins!"))
                    .build())
                .item(5, 1, item()
                    .material(Material.BOOK)
                    .name("menu.info.title")
                    .lore("""
                            menu.info.description
                            
                            <gray>Player: <white><player>
                            <gray>Locale: <white><locale>""",
                        ctx -> Map.of(
                            "player", ctx.getEntity().getName(),
                            "locale", "en_US"  // In real use, get from player settings
                        )
                    )
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Custom i18n MessageProvider that extends DefaultMessageProvider.
     * This shows how to add locale-based message resolution.
     */
    private static class I18nMessageProvider extends DefaultMessageProvider {
        private final Map<Locale, Map<String, String>> messages = new HashMap<>();

        public I18nMessageProvider() {
            super();
            loadMessages();
        }

        private void loadMessages() {
            // English messages
            Map<String, String> en = new HashMap<>();
            en.put("menu.shop.sword.name", "<blue>Diamond Sword");
            en.put("menu.shop.sword.lore.damage", "<gray>Damage: <red><damage>");
            en.put("menu.shop.sword.lore.price", "<gray>Price: <gold><price> coins");
            en.put("menu.info.title", "<gold>Information");
            en.put("menu.info.description", "<gray>This demonstrates i18n support");
            messages.put(Locale.ENGLISH, en);

            // Polish messages (example)
            Map<String, String> pl = new HashMap<>();
            pl.put("menu.shop.sword.name", "<blue>Diamentowy miecz");
            pl.put("menu.shop.sword.lore.damage", "<gray>Obrażenia: <red><damage>");
            pl.put("menu.shop.sword.lore.price", "<gray>Cena: <gold><price> monet");
            pl.put("menu.info.title", "<gold>Informacje");
            pl.put("menu.info.description", "<gray>To pokazuje obsługę i18n");
            messages.put(new Locale("pl"), pl);
        }

        @Override
        public @NonNull Component resolve(@NonNull HumanEntity viewer, @NonNull String template, @NonNull Map<String, Object> vars) {
            // Get player's locale (simplified - in real use, get from player settings)
            Locale locale = Locale.ENGLISH;

            // If template is a key, resolve it
            Map<String, String> localeMessages = messages.get(locale);
            if (localeMessages != null && localeMessages.containsKey(template)) {
                template = localeMessages.get(template);
            }

            // Delegate to parent's universal format support
            return super.resolve(viewer, template, vars);
        }
    }

    /**
     * Example showing PlaceholderAPI integration pattern.
     * This demonstrates how to add custom placeholder resolvers.
     */
    public static Menu createPlaceholderAPIExample(Plugin plugin) {
        // Create a provider with custom placeholders
        MessageProvider papiProvider = new PlaceholderAPIMessageProvider();

        return Menu.builder(plugin)
            .title("<gradient:gold:yellow>PlaceholderAPI Example</gradient>")
            .rows(3)
            .messageProvider(papiProvider)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .item(4, 1, item()
                    .material(Material.PLAYER_HEAD)
                    .name("<gradient:blue:aqua><player_name>")  // Custom placeholder
                    .lore("""
                        <gray>World: <white><world_name>
                        <gray>Health: <red><player_health>
                        <gray>Level: <green><player_level>
                        
                        <yellow>Placeholders auto-update!""")
                    .build())
                .item(8, 2, NavigationUtils.closeButton().build())
                .build())
            .build();
    }

    /**
     * Custom MessageProvider that adds PlaceholderAPI-style placeholders.
     * This shows how to override buildResolvers() for custom placeholder support.
     */
    private static class PlaceholderAPIMessageProvider extends DefaultMessageProvider {
        @Override
        protected @NonNull List<TagResolver> buildResolvers(@NonNull HumanEntity viewer, @NonNull Map<String, Object> vars) {
            List<TagResolver> resolvers = super.buildResolvers(viewer, vars);

            // Add custom PlaceholderAPI-style resolvers
            if (viewer != null) {
                resolvers.add(Placeholder.unparsed("player_name", viewer.getName()));
                resolvers.add(Placeholder.unparsed("world_name", viewer.getWorld().getName()));
                resolvers.add(Placeholder.unparsed("player_health", String.valueOf((int) viewer.getHealth())));

                // Player-specific placeholders (require cast to Player)
                if (viewer instanceof org.bukkit.entity.Player player) {
                    resolvers.add(Placeholder.unparsed("player_level", String.valueOf(player.getLevel())));
                }

                // In real implementation, integrate with actual PlaceholderAPI here
            }

            return resolvers;
        }
    }

    /**
     * Example demonstrating i18n-aware utility buttons.
     * Shows how to use NavigationUtils and PaginationUtils with locale maps.
     */
    public static Menu createI18nButtonsExample(Plugin plugin) {
        // Sample data for pagination
        List<String> items = java.util.List.of(
            "Item 1", "Item 2", "Item 3", "Item 4", "Item 5",
            "Item 6", "Item 7", "Item 8", "Item 9", "Item 10"
        );

        return Menu.builder(plugin)
            .title("<gold>i18n Buttons Demo")
            .rows(5)
            .pane("items", PaginatedPane.<String>pane()
                .name("items")
                .bounds(0, 1, 9, 3)
                .items(items)
                .itemsPerPage(9)
                .renderer((item, index) -> item()
                    .material(Material.PAPER)
                    .name("<yellow>" + item)
                    .build())
                .build())
            .pane("controls", staticPane()
                .name("controls")
                .bounds(0, 0, 9, 1)
                // Custom i18n pagination buttons
                .item(0, 0, eu.okaeri.menu.pagination.PaginationUtils.previousPageButton(
                    "items",
                    Map.of(
                        Locale.ENGLISH, "<red>← Previous",
                        new Locale("pl"), "<red>← Poprzednia",
                        new Locale("de"), "<red>← Zurück",
                        new Locale("fr"), "<red>← Précédent"
                    ),
                    Map.of(
                        Locale.ENGLISH, """
                            <gray>Page: <white><current>/<total>
                            
                            <yellow>Go to previous page""",
                        new Locale("pl"), """
                            <gray>Strona: <white><current>/<total>
                            
                            <yellow>Przejdź do poprzedniej""",
                        new Locale("de"), """
                            <gray>Seite: <white><current>/<total>
                            
                            <yellow>Zur vorherigen Seite""",
                        new Locale("fr"), """
                            <gray>Page: <white><current>/<total>
                            
                            <yellow>Aller à la page précédente"""
                    )
                ).build())
                .item(4, 0, eu.okaeri.menu.pagination.PaginationUtils.pageIndicator(
                    "items",
                    Map.of(
                        Locale.ENGLISH, "<yellow>Page <current>/<total>",
                        new Locale("pl"), "<yellow>Strona <current>/<total>",
                        new Locale("de"), "<yellow>Seite <current>/<total>",
                        new Locale("fr"), "<yellow>Page <current>/<total>"
                    ),
                    Map.of(
                        Locale.ENGLISH, """
                            <gray>Showing: <white><showing>
                            <gray>Total: <white><total_items>""",
                        new Locale("pl"), """
                            <gray>Pokazuję: <white><showing>
                            <gray>Łącznie: <white><total_items>""",
                        new Locale("de"), """
                            <gray>Angezeigt: <white><showing>
                            <gray>Gesamt: <white><total_items>""",
                        new Locale("fr"), """
                            <gray>Affichage: <white><showing>
                            <gray>Total: <white><total_items>"""
                    )
                ).build())
                .item(8, 0, eu.okaeri.menu.pagination.PaginationUtils.nextPageButton(
                    "items",
                    Map.of(
                        Locale.ENGLISH, "<green>Next →",
                        new Locale("pl"), "<green>Następna →",
                        new Locale("de"), "<green>Weiter →",
                        new Locale("fr"), "<green>Suivant →"
                    ),
                    Map.of(
                        Locale.ENGLISH, """
                            <gray>Page: <white><current>/<total>
                            
                            <yellow>Go to next page""",
                        new Locale("pl"), """
                            <gray>Strona: <white><current>/<total>
                            
                            <yellow>Przejdź do następnej""",
                        new Locale("de"), """
                            <gray>Seite: <white><current>/<total>
                            
                            <yellow>Zur nächsten Seite""",
                        new Locale("fr"), """
                            <gray>Page: <white><current>/<total>
                            
                            <yellow>Aller à la page suivante"""
                    )
                ).build())
                .build())
            .pane("navigation", staticPane()
                .name("navigation")
                .bounds(0, 4, 9, 1)
                // Custom i18n back button
                .item(0, 0, eu.okaeri.menu.navigation.NavigationUtils.backButton(
                    Map.of(
                        Locale.ENGLISH, "<yellow>← Back",
                        new Locale("pl"), "<yellow>← Wstecz",
                        new Locale("de"), "<yellow>← Zurück",
                        new Locale("fr"), "<yellow>← Retour"
                    ),
                    Map.of(
                        Locale.ENGLISH, """
                            <gray>Return to previous menu
                            
                            <yellow>Click to go back!""",
                        new Locale("pl"), """
                            <gray>Wróć do poprzedniego menu
                            
                            <yellow>Kliknij aby wrócić!""",
                        new Locale("de"), """
                            <gray>Zurück zum vorherigen Menü
                            
                            <yellow>Klicken zum Zurückkehren!""",
                        new Locale("fr"), """
                            <gray>Retour au menu précédent
                            
                            <yellow>Cliquez pour revenir!"""
                    )
                ).build())
                // Custom i18n close button
                .item(8, 0, eu.okaeri.menu.navigation.NavigationUtils.closeButton(
                    Map.of(
                        Locale.ENGLISH, "<red>✕ Close",
                        new Locale("pl"), "<red>✕ Zamknij",
                        new Locale("de"), "<red>✕ Schließen",
                        new Locale("fr"), "<red>✕ Fermer"
                    ),
                    Map.of(
                        Locale.ENGLISH, """
                            <gray>Close this menu
                            
                            <red>Click to close!""",
                        new Locale("pl"), """
                            <gray>Zamknij to menu
                            
                            <red>Kliknij aby zamknąć!""",
                        new Locale("de"), """
                            <gray>Dieses Menü schließen
                            
                            <red>Klicken zum Schließen!""",
                        new Locale("fr"), """
                            <gray>Fermer ce menu
                            
                            <red>Cliquez pour fermer!"""
                    )
                ).build())
                .build())
            .build();
    }
}
