package eu.okaeri.menu.message;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Universal message provider supporting legacy § codes, legacy & codes, and MiniMessage tags.
 * All formats can be mixed freely in the same message.
 *
 * <p><b>Supported formats:</b>
 * <ul>
 *   <li>Legacy § color codes: {@code §cRed §bBlue}
 *   <li>Legacy & color codes: {@code &cRed &bBlue}
 *   <li>MiniMessage tags: {@code <red>Red <gradient:red:blue>Gradient</gradient>}
 *   <li>MiniMessage placeholders: {@code <player>} (auto-escaped for safety)
 * </ul>
 *
 * <p><b>Performance optimization:</b>
 * MiniMessage parsing is only used when the template contains {@code <} and {@code >} characters.
 * Otherwise, only legacy color codes are processed for better performance.
 *
 * <p><b>Extensibility:</b>
 * Extend this class and override {@link #buildResolvers(HumanEntity, Map)} to add custom
 * placeholder support (e.g., PlaceholderAPI, custom placeholder libraries).
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * MessageProvider provider = new DefaultMessageProvider();
 *
 * // Legacy codes
 * Component legacy = provider.resolve(player, "§cRed &bBlue", Map.of());
 *
 * // MiniMessage
 * Component mm = provider.resolve(player, "<red>Red <gradient:red:blue>Gradient", Map.of());
 *
 * // Mixed formats
 * Component mixed = provider.resolve(player, "§cLegacy <gradient:red:blue>&aMixed", Map.of());
 *
 * // Placeholders (auto-escaped)
 * Component placeholder = provider.resolve(player, "Hello <player>!", Map.of("player", "Steve"));
 * }</pre>
 *
 * <p><b>Custom placeholders:</b>
 * <pre>{@code
 * public class CustomMessageProvider extends DefaultMessageProvider {
 *     @Override
 *     protected List<TagResolver> buildResolvers(HumanEntity viewer, Map<String, Object> vars) {
 *         List<TagResolver> resolvers = super.buildResolvers(viewer, vars);
 *         // Add PlaceholderAPI support
 *         resolvers.add(Placeholder.unparsed("world", viewer.getWorld().getName()));
 *         return resolvers;
 *     }
 * }
 * }</pre>
 */
public class DefaultMessageProvider implements MessageProvider {

    private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("§([0-9A-Fa-fK-Ok-oRrXx])");
    private static final Pattern POST_PROCESS_PATTERN = Pattern.compile(".++", Pattern.DOTALL);
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final MiniMessage miniMessage;

    /**
     * Creates a default message provider with the standard MiniMessage configuration.
     */
    public DefaultMessageProvider() {
        this(MiniMessage.builder()
            // Convert § codes to & codes for uniform handling
            .preProcessor(text -> SECTION_COLOR_PATTERN.matcher(text).replaceAll("&$1"))
            // Post-process to deserialize & codes after MiniMessage parsing
            .postProcessor(component -> component.replaceText(config -> config
                .match(POST_PROCESS_PATTERN)
                .replacement((result, input) -> AMPERSAND_SERIALIZER.deserialize(result.group()))
            ))
            .build()
        );
    }

    /**
     * Creates a message provider with a custom MiniMessage instance.
     * Useful for advanced MiniMessage configurations.
     *
     * @param miniMessage Custom MiniMessage instance
     */
    public DefaultMessageProvider(@NonNull MiniMessage miniMessage) {
        this.miniMessage = miniMessage;
    }

    @Override
    @NonNull
    public List<Component> resolve(@NonNull HumanEntity viewer, @NonNull String template, @NonNull Map<String, Object> vars) {
        if (template.isEmpty()) {
            return List.of(Component.empty());
        }

        // Split on newlines for multiline support
        if (template.contains("\n")) {
            List<String> lines = List.of(template.split("\n"));
            List<Component> components = new ArrayList<>();
            for (String line : lines) {
                components.addAll(this.resolve(viewer, line, vars));
            }
            return components;
        }

        // Optimization: Only use MiniMessage if template contains < and >
        // For pure legacy messages, use direct serialization for better performance
        if (!template.contains("<") || !template.contains(">")) {
            // Convert § to & for uniform handling, then deserialize
            String normalized = SECTION_COLOR_PATTERN.matcher(template).replaceAll("&$1");
            return List.of(AMPERSAND_SERIALIZER.deserialize(normalized));
        }

        // Build resolvers for placeholders
        List<TagResolver> resolvers = this.buildResolvers(viewer, vars);
        TagResolver resolver = resolvers.isEmpty()
            ? TagResolver.empty()
            : TagResolver.resolver(resolvers);

        // Parse with MiniMessage (handles §, &, and MiniMessage tags)
        return List.of(this.miniMessage.deserialize(template, resolver));
    }

    @Override
    @NonNull
    public List<Component> resolve(@NonNull HumanEntity viewer, @NonNull Map<Locale, String> localeMap, @NonNull Map<String, Object> vars) {
        if (localeMap.isEmpty()) {
            return List.of(Component.empty());
        }

        // Get viewer's locale using extensible method
        Locale locale = this.resolveLocale(viewer);

        // Try to get template for viewer's locale
        String template = localeMap.get(locale);

        // Fallback to English if not found
        if (template == null) {
            template = localeMap.get(Locale.ENGLISH);
        }

        // Fallback to first available locale if English not found
        if ((template == null) && !localeMap.isEmpty()) {
            template = localeMap.values().iterator().next();
        }

        // Resolve the selected template
        return this.resolve(viewer, (template != null) ? template : "", vars);
    }

    /**
     * Resolves the locale for a viewer.
     * Override this method to provide custom locale resolution (e.g., from database, PlaceholderAPI).
     *
     * <p>Default implementation uses Paper's player.locale() for Player instances,
     * falling back to Locale.ENGLISH for other HumanEntity types.
     *
     * @param viewer The viewer
     * @return The locale for the viewer
     */
    @NonNull
    protected Locale resolveLocale(@NonNull HumanEntity viewer) {
        if (viewer instanceof Player player) {
            return player.locale();
        }
        return Locale.ENGLISH;
    }

    /**
     * Builds the list of tag resolvers for placeholder replacement.
     * Override this method to add custom placeholder support.
     *
     * <p>The default implementation creates {@link Placeholder#unparsed(String, String)}
     * resolvers from the vars map, which automatically escapes any MiniMessage tags
     * in the values to prevent injection attacks.
     *
     * <p>Supports reactive values: if a value is a {@link Supplier}, it will be
     * evaluated by calling {@link Supplier#get()} to get the actual value.
     *
     * @param viewer The player viewing the message (can be null)
     * @param vars   The variables map for placeholder replacement
     * @return List of tag resolvers
     */
    @NonNull
    protected List<TagResolver> buildResolvers(@NonNull HumanEntity viewer, @NonNull Map<String, Object> vars) {
        List<TagResolver> resolvers = new ArrayList<>();

        // Add resolvers from vars map
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            Object rawValue = entry.getValue();

            // Evaluate Suppliers to get actual value
            if (rawValue instanceof Supplier<?> supplier) {
                rawValue = supplier.get();
            }

            String value = (rawValue != null) ? rawValue.toString() : "";
            resolvers.add(Placeholder.unparsed(entry.getKey(), value));
        }

        return resolvers;
    }
}
