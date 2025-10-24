package eu.okaeri.menu.message;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
     * Resolves a message template into a list of Components.
     *
     * @param viewer   The viewer (for per-player i18n/placeholders)
     * @param template The message template
     * @param vars     Variables for placeholder replacement
     * @return The resolved Component(s)
     */
    @NonNull
    List<Component> resolve(@NonNull HumanEntity viewer, @NonNull String template, @NonNull Map<String, Object> vars);

    /**
     * Resolves a message template into a list of Components without variables.
     *
     * @param viewer   The viewer
     * @param template The message template
     * @return The resolved Component(s)
     */
    @NonNull
    default List<Component> resolve(@NonNull HumanEntity viewer, @NonNull String template) {
        return this.resolve(viewer, template, Map.of());
    }

    /**
     * Resolves a locale-specific message map into a list of Components.
     * Provider selects the appropriate locale for the viewer.
     *
     * @param viewer    The viewer (determines locale)
     * @param localeMap Map of locale to message template
     * @param vars      Variables for placeholder replacement
     * @return The resolved Component(s)
     */
    @NonNull
    List<Component> resolve(@NonNull HumanEntity viewer, @NonNull Map<Locale, String> localeMap, @NonNull Map<String, Object> vars);

    /**
     * Resolves a message template into a single Component.
     * If the template resolves to multiple components, they are joined with newlines.
     * Use this for single-line contexts like menu titles or item names.
     *
     * @param viewer   The viewer (for per-player i18n/placeholders)
     * @param template The message template
     * @param vars     Variables for placeholder replacement
     * @return A single resolved Component
     */
    @NonNull
    default Component resolveSingle(@NonNull HumanEntity viewer, @NonNull String template, @NonNull Map<String, Object> vars) {
        List<Component> components = this.resolve(viewer, template, vars);
        return (components.size() == 1) ? components.get(0) : Component.join(JoinConfiguration.newlines(), components);
    }

    /**
     * Resolves a message template into a single Component without variables.
     *
     * @param viewer   The viewer
     * @param template The message template
     * @return A single resolved Component
     */
    @NonNull
    default Component resolveSingle(@NonNull HumanEntity viewer, @NonNull String template) {
        return this.resolveSingle(viewer, template, Map.of());
    }

    /**
     * Resolves a locale-specific message map into a single Component.
     * If the template resolves to multiple components, they are joined with newlines.
     *
     * @param viewer    The viewer (determines locale)
     * @param localeMap Map of locale to message template
     * @param vars      Variables for placeholder replacement
     * @return A single resolved Component
     */
    @NonNull
    default Component resolveSingle(@NonNull HumanEntity viewer, @NonNull Map<Locale, String> localeMap, @NonNull Map<String, Object> vars) {
        List<Component> components = this.resolve(viewer, localeMap, vars);
        return (components.size() == 1) ? components.get(0) : Component.join(JoinConfiguration.newlines(), components);
    }

    /**
     * Resolves a locale-specific message map into a single Component without variables.
     *
     * @param viewer    The viewer (determines locale)
     * @param localeMap Map of locale to message template
     * @return A single resolved Component
     */
    @NonNull
    default Component resolveSingle(@NonNull HumanEntity viewer, @NonNull Map<Locale, String> localeMap) {
        return this.resolveSingle(viewer, localeMap, Map.of());
    }
}
