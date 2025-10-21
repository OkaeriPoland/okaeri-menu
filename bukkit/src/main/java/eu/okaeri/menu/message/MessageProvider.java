package eu.okaeri.menu.message;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.HumanEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provider interface for resolving message templates into Adventure Components.
 * Supports i18n keys, placeholders, and MiniMessage formatting.
 *
 * <p>Message templates can contain:
 * <ul>
 *   <li>${key} - i18n key resolution (if supported by provider)
 *   <li>{var} or <var> - Placeholder replacement (format depends on provider)
 *   <li>MiniMessage tags - Color/formatting (if supported by provider)
 * </ul>
 */
public interface MessageProvider {

    /**
     * Resolves a message template into a Component.
     *
     * @param viewer   The viewer (for per-player i18n/placeholders)
     * @param template The message template
     * @param vars     Variables for placeholder replacement
     * @return The resolved Component
     */
    @NonNull
    Component resolve(@NonNull HumanEntity viewer, @NonNull String template, @NonNull Map<String, Object> vars);

    /**
     * Resolves a locale-specific message map into a Component.
     * Provider selects the appropriate locale for the viewer.
     *
     * @param viewer    The viewer (determines locale)
     * @param localeMap Map of locale to message template
     * @param vars      Variables for placeholder replacement
     * @return The resolved Component
     */
    @NonNull
    Component resolve(@NonNull HumanEntity viewer, @NonNull Map<Locale, String> localeMap, @NonNull Map<String, Object> vars);

    /**
     * Resolves a message template into a Component without variables.
     *
     * @param viewer   The viewer
     * @param template The message template
     * @return The resolved Component
     */
    @NonNull
    default Component resolve(@NonNull HumanEntity viewer, @NonNull String template) {
        return this.resolve(viewer, template, Map.of());
    }

    /**
     * Resolves a list of message templates into Components.
     * Useful for lore.
     *
     * @param viewer    The viewer
     * @param templates The message templates
     * @param vars      Variables for placeholder replacement
     * @return The resolved Components
     */
    @NonNull
    default List<Component> resolveList(@NonNull HumanEntity viewer, @NonNull List<String> templates, @NonNull Map<String, Object> vars) {
        return templates.stream()
            .map(template -> this.resolve(viewer, template, vars))
            .toList();
    }

    /**
     * Resolves a list of message templates into Components without variables.
     *
     * @param viewer    The viewer
     * @param templates The message templates
     * @return The resolved Components
     */
    @NonNull
    default List<Component> resolveList(@NonNull HumanEntity viewer, @NonNull List<String> templates) {
        return this.resolveList(viewer, templates, Map.of());
    }
}
