package eu.okaeri.menu.async;

import eu.okaeri.menu.item.MenuItem;
import lombok.NonNull;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Map;

/**
 * Utility methods for common async suspense state items.
 * Provides default items for loading, error, and empty states used by AsyncPaginatedPane and AsyncMenuItem.
 *
 * <p>Core i18n-aware methods accept Map&lt;Locale, String&gt; for full localization support.
 * Convenience methods provide English defaults by calling the i18n versions.
 */
public class AsyncUtils {

    // ========================================
    // I18N-AWARE METHODS (Core API)
    // ========================================

    /**
     * Creates a loading state item with full i18n support.
     * Shown while async data is being loaded.
     *
     * @param material The material for the item
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a loading item
     */
    @NonNull
    public static MenuItem.Builder loadingItem(@NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name);

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates a loading item with i18n support and default material (HOPPER).
     *
     * @param name The display name in multiple languages
     * @param lore The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for a loading item
     */
    @NonNull
    public static MenuItem.Builder loadingItem(@NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return loadingItem(Material.HOPPER, name, lore);
    }

    /**
     * Creates an error state item with full i18n support.
     * Shown when async data loading fails.
     *
     * @param material The material for the item
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for an error item
     */
    @NonNull
    public static MenuItem.Builder errorItem(@NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name);

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates an error item with i18n support and default material (BARRIER).
     *
     * @param name The display name in multiple languages
     * @param lore The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for an error item
     */
    @NonNull
    public static MenuItem.Builder errorItem(@NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return errorItem(Material.BARRIER, name, lore);
    }

    /**
     * Creates an empty state item with full i18n support.
     * Shown when async data loads successfully but the list is empty.
     *
     * @param material The material for the item
     * @param name     The display name in multiple languages
     * @param lore     The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for an empty item
     */
    @NonNull
    public static MenuItem.Builder emptyItem(@NonNull Material material, @NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        MenuItem.Builder builder = MenuItem.item()
            .material(material)
            .name(name);

        if ((lore != null) && !lore.isEmpty()) {
            builder.lore(lore);
        }

        return builder;
    }

    /**
     * Creates an empty item with i18n support and default material (LIGHT_GRAY_STAINED_GLASS_PANE).
     *
     * @param name The display name in multiple languages
     * @param lore The lore in multiple languages (optional, can be null)
     * @return A MenuItem builder for an empty item
     */
    @NonNull
    public static MenuItem.Builder emptyItem(@NonNull Map<Locale, String> name, Map<Locale, String> lore) {
        return emptyItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name, lore);
    }

    // ========================================
    // CONVENIENCE METHODS (English Defaults)
    // ========================================

    /**
     * Creates a standard loading state item.
     * Shown while async data is being loaded.
     * Uses English text.
     *
     * @return A MenuItem builder for a loading item
     */
    @NonNull
    public static MenuItem.Builder loadingItem() {
        return loadingItem(
            Material.HOPPER,
            Map.of(Locale.ENGLISH, "<yellow>⏳ Loading..."),
            Map.of(Locale.ENGLISH, """
                <gray>Please wait while
                <gray>the data is loading
                
                <yellow>This may take a moment""")
        );
    }

    /**
     * Creates a customizable loading item with English text.
     * For i18n support, use {@link #loadingItem(Material, Map, Map)} instead.
     *
     * @param material The material for the item
     * @param name     The display name template (supports §, &, MiniMessage)
     * @return A MenuItem builder for a loading item
     */
    @NonNull
    public static MenuItem.Builder loadingItem(@NonNull Material material, @NonNull String name) {
        return loadingItem(material, Map.of(Locale.ENGLISH, name), null);
    }

    /**
     * Creates a standard error state item.
     * Shown when async data loading fails.
     * Uses English text.
     *
     * @return A MenuItem builder for an error item
     */
    @NonNull
    public static MenuItem.Builder errorItem() {
        return errorItem(
            Material.BARRIER,
            Map.of(Locale.ENGLISH, "<red>✕ Error"),
            Map.of(Locale.ENGLISH, """
                <gray>Failed to load data
                
                <red>Please try again later""")
        );
    }

    /**
     * Creates a customizable error item with English text.
     * For i18n support, use {@link #errorItem(Material, Map, Map)} instead.
     *
     * @param material The material for the item
     * @param name     The display name template (supports §, &, MiniMessage)
     * @return A MenuItem builder for an error item
     */
    @NonNull
    public static MenuItem.Builder errorItem(@NonNull Material material, @NonNull String name) {
        return errorItem(material, Map.of(Locale.ENGLISH, name), null);
    }

    /**
     * Creates a standard empty state item.
     * Shown when async data loads successfully but the list is empty.
     * Uses English text.
     *
     * @return A MenuItem builder for an empty item
     */
    @NonNull
    public static MenuItem.Builder emptyItem() {
        return emptyItem(
            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Map.of(Locale.ENGLISH, "<gray>No Items"),
            Map.of(Locale.ENGLISH, """
                <gray>There are no items
                <gray>to display
                
                <dark_gray>Try again later""")
        );
    }

    /**
     * Creates a customizable empty item with English text.
     * For i18n support, use {@link #emptyItem(Material, Map, Map)} instead.
     *
     * @param material The material for the item
     * @param name     The display name template (supports §, &, MiniMessage)
     * @return A MenuItem builder for an empty item
     */
    @NonNull
    public static MenuItem.Builder emptyItem(@NonNull Material material, @NonNull String name) {
        return emptyItem(material, Map.of(Locale.ENGLISH, name), null);
    }
}
