package eu.okaeri.menu;

import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.message.MessageProvider;
import eu.okaeri.menu.state.StateDefaults;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Factory for creating Menu instances with shared configuration.
 * Provides a centralized way to configure MessageProvider, AsyncExecutor, and other defaults.
 *
 * <p>Example usage:
 * <pre>{@code
 * MenuFactory provider = MenuFactory.create(plugin)
 *     .messageProvider(new MyI18nProvider())
 *     .defaultUpdateInterval(Duration.ofMillis(200))
 *     .defaultState(s -> s.define("currency", "coins"));
 *
 * Menu shopMenu = provider.menu()
 *     .title("Shop")
 *     .pane("items", ...)
 *     .build();
 * }</pre>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuFactory {

    private final @NonNull Plugin plugin;
    private MessageProvider messageProvider;
    private AsyncExecutor asyncExecutor;
    private Duration defaultUpdateInterval;
    private Consumer<StateDefaults> defaultStateConfig;

    /**
     * Creates a new MenuFactory for the given plugin.
     *
     * @param plugin The plugin instance
     * @return A new MenuFactory
     */
    @NonNull
    public static MenuFactory create(@NonNull Plugin plugin) {
        return new MenuFactory(plugin);
    }

    /**
     * Sets the MessageProvider to use for all menus created by this provider.
     * Individual menus can override this.
     *
     * @param messageProvider The message provider
     * @return This provider
     */
    @NonNull
    public MenuFactory messageProvider(@NonNull MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
        return this;
    }

    /**
     * Sets the AsyncExecutor to use for all menus created by this provider.
     * Individual menus can override this.
     *
     * @param asyncExecutor The async executor
     * @return This provider
     */
    @NonNull
    public MenuFactory asyncExecutor(@NonNull AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }

    /**
     * Sets the default update interval for menus created by this provider.
     * Individual menus can override this.
     *
     * @param interval The default update interval
     * @return This provider
     */
    @NonNull
    public MenuFactory defaultUpdateInterval(@NonNull Duration interval) {
        this.defaultUpdateInterval = interval;
        return this;
    }

    /**
     * Sets default state variables for all menus created by this provider.
     * Individual menus can add or override these defaults.
     *
     * <p>Example:
     * <pre>{@code
     * provider.defaultState(s -> s
     *     .define("currency", "coins")
     *     .define("prefix", "&7[Menu] "))
     * }</pre>
     *
     * @param stateConfig Consumer to configure default state
     * @return This provider
     */
    @NonNull
    public MenuFactory defaultState(@NonNull Consumer<StateDefaults> stateConfig) {
        this.defaultStateConfig = stateConfig;
        return this;
    }

    /**
     * Creates a new Menu builder with this provider's defaults.
     * The returned builder can override any of these defaults.
     *
     * @return A pre-configured Menu builder
     */
    @NonNull
    public Menu.Builder menu() {
        Menu.Builder builder = Menu.builder(this.plugin);

        // Apply provider defaults
        if (this.messageProvider != null) {
            builder.messageProvider(this.messageProvider);
        }
        if (this.asyncExecutor != null) {
            builder.asyncExecutor(this.asyncExecutor);
        }
        if (this.defaultUpdateInterval != null) {
            builder.updateInterval(this.defaultUpdateInterval);
        }
        if (this.defaultStateConfig != null) {
            builder.state(this.defaultStateConfig);
        }

        return builder;
    }
}
