package eu.okaeri.menu.async;

import eu.okaeri.menu.MenuContext;
import lombok.NonNull;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Manages reactive data sources and triggers async loads.
 * Used by both Menu and MenuItem to deduplicate reactive loading logic.
 */
public class ReactiveLoader {

    private final Map<String, ReactiveDataSource> sources = new LinkedHashMap<>();

    /**
     * Registers a reactive data source.
     *
     * @param key    Unique identifier for this data source
     * @param loader Function that loads the data (receives MenuContext)
     * @param ttl    Time-to-live for cached data
     */
    public void register(@NonNull String key, @NonNull Function<MenuContext, ?> loader, @NonNull Duration ttl) {
        this.sources.put(key, new ReactiveDataSource(key, loader, ttl));
    }

    /**
     * Triggers all registered reactive loads for the given context.
     * Idempotent - respects TTL and won't reload if cached.
     *
     * @param context The menu context for the current viewer
     */
    public void trigger(@NonNull MenuContext context) {
        for (ReactiveDataSource source : this.sources.values()) {
            context.loadAsync(source.key(), () -> source.loader().apply(context), source.ttl());
        }
    }

    /**
     * Gets all registered cache keys (for preloading).
     *
     * @return Set of cache keys
     */
    @NonNull
    public Set<String> getKeys() {
        return this.sources.keySet();
    }

    /**
     * Checks if this loader has any registered sources.
     *
     * @return true if no sources are registered
     */
    public boolean isEmpty() {
        return this.sources.isEmpty();
    }

    /**
     * Helper record to store reactive data source configuration.
     */
    record ReactiveDataSource(
        String key,
        Function<MenuContext, ?> loader,
        Duration ttl
    ) {}
}
