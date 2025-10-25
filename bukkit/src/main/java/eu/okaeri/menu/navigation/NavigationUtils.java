package eu.okaeri.menu.navigation;

import eu.okaeri.menu.MenuContext;
import eu.okaeri.menu.item.MenuItem;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.Map;

/**
 * Utility methods for common navigation patterns.
 * All methods use the menu's MessageProvider for text formatting.
 *
 * <p>Core i18n-aware methods accept Map&lt;Locale, String&gt; for full localization support.
 * Convenience methods provide English defaults by calling the i18n versions.
 */
public final class NavigationUtils {

    // ========================================
    // I18N-AWARE METHODS (Core API)
    // ========================================

    /**
     * Creates a back button with full i18n support.
     * The button is only visible if there is navigation history.
     *
     * @param material The material for the button
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a back button
     */
    @NonNull
    public static MenuItem.Builder backButton(@NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .visible(MenuContext::hasLast)
            .onClick(event -> {
                event.playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                event.back();
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates a back button with i18n support and default material (ARROW).
     *
     * @param name The display name in multiple languages
     * @param lore The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a back button
     */
    @NonNull
    public static MenuItem.Builder backButton(@NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return backButton(Material.ARROW, name, lore);
    }

    /**
     * Creates a close button with full i18n support.
     * Closes the inventory and clears navigation history.
     *
     * @param material The material for the button
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a close button
     */
    @NonNull
    public static MenuItem.Builder closeButton(@NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name)
            .onClick(event -> {
                event.clearHistory();
                event.closeInventory();
            });

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates a close button with i18n support and default material (BARRIER).
     *
     * @param name The display name in multiple languages
     * @param lore The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a close button
     */
    @NonNull
    public static MenuItem.Builder closeButton(@NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return closeButton(Material.BARRIER, name, lore);
    }

    /**
     * Creates a navigation depth indicator with i18n support (for debugging).
     * Shows how deep the player is in the menu navigation.
     *
     * @param material The material for the indicator
     * @param name     The display name in multiple languages
     * @param lore     The lore template in multiple languages (supports &lt;depth&gt; and &lt;has_previous&gt; placeholders)
     * @return A MenuItem builder for a depth indicator
     */
    @NonNull
    public static MenuItem.Builder depthIndicator(@NonNull Material material, @NonNull Map<Locale, String> name, @NonNull Map<Locale, String> lore) {
        return MenuItem.item()
            .material(material)
            .name(name)
            .lore(lore, ctx -> Map.of(
                "depth", ctx.navigationDepth(),
                "has_previous", ctx.hasLast() ? "Yes" : "No"
            ));
    }

    /**
     * Creates a depth indicator with i18n support and default material (COMPASS).
     *
     * @param name The display name in multiple languages
     * @param lore The lore template in multiple languages (supports &lt;depth&gt; and &lt;has_previous&gt; placeholders)
     * @return A MenuItem builder for a depth indicator
     */
    @NonNull
    public static MenuItem.Builder depthIndicator(@NonNull Map<Locale, String> name, @NonNull Map<Locale, String> lore) {
        return depthIndicator(Material.COMPASS, name, lore);
    }

    // ========================================
    // CONVENIENCE METHODS (English Defaults)
    // ========================================

    /**
     * Creates a standard back button that navigates to the previous menu.
     * Uses English text. The button is only visible if there is navigation history.
     *
     * @return A MenuItem builder for a back button
     */
    @NonNull
    public static MenuItem.Builder backButton() {
        return backButton(
            Material.ARROW,
            Map.of(Locale.ENGLISH, "<yellow>← Back"),
            Map.of(Locale.ENGLISH, """
                <gray>Return to previous menu
                
                <yellow>Click to go back!""")
        );
    }

    /**
     * Creates a customizable back button with English text.
     * For i18n support, use {@link #backButton(Material, Map, Map)} instead.
     *
     * @param material The material for the button
     * @param name     The display name template (supports §, &, MiniMessage)
     * @return A MenuItem builder for a back button
     */
    @NonNull
    public static MenuItem.Builder backButton(@NonNull Material material, @NonNull String name) {
        return backButton(material, Map.of(Locale.ENGLISH, name), null);
    }

    /**
     * Creates a close button that closes the inventory and clears navigation history.
     * Uses English text.
     *
     * @return A MenuItem builder for a close button
     */
    @NonNull
    public static MenuItem.Builder closeButton() {
        return closeButton(
            Material.BARRIER,
            Map.of(Locale.ENGLISH, "<red>✕ Close"),
            Map.of(Locale.ENGLISH, """
                <gray>Close this menu
                
                <red>Click to close!""")
        );
    }

    /**
     * Creates a customizable close button with English text.
     * For i18n support, use {@link #closeButton(Material, Map, Map)} instead.
     *
     * @param material The material for the button
     * @param name     The display name template (supports §, &, MiniMessage)
     * @return A MenuItem builder for a close button
     */
    @NonNull
    public static MenuItem.Builder closeButton(@NonNull Material material, @NonNull String name) {
        return closeButton(material, Map.of(Locale.ENGLISH, name), null);
    }

    /**
     * Creates a navigation depth indicator (for debugging).
     * Shows how deep the player is in the menu navigation.
     * Uses English text.
     *
     * @return A MenuItem builder for a depth indicator
     */
    @NonNull
    public static MenuItem.Builder depthIndicator() {
        return depthIndicator(
            Material.COMPASS,
            Map.of(Locale.ENGLISH, "<gold>Navigation Depth"),
            Map.of(Locale.ENGLISH, """
                <gray>Depth: <white><depth>
                <gray>Has Previous: <white><has_previous>""")
        );
    }
}
