package eu.okaeri.menu.reactive;

import eu.okaeri.menu.MenuContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps a value that can be static or reactive (supplier-based).
 * Reactive values are cached until explicitly invalidated.
 *
 * @param <T> The type of the wrapped value
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReactiveProperty<T> {

    private final Supplier<T> supplier;
    private T cachedValue;

    private @Getter boolean dirty = true;
    private boolean hasCachedValue = false;

    /**
     * Creates a static reactive property (value never changes).
     *
     * @param staticValue The static value
     * @param <T>         The type
     * @return A reactive property wrapping the static value
     */
    @NonNull
    public static <T> ReactiveProperty<T> of(T staticValue) {
        return new ReactiveProperty<>(() -> staticValue);
    }

    /**
     * Creates a reactive property from a supplier.
     *
     * @param supplier The supplier providing the value
     * @param <T>      The type
     * @return A reactive property wrapping the supplier
     */
    @NonNull
    public static <T> ReactiveProperty<T> of(@NonNull Supplier<T> supplier) {
        return new ReactiveProperty<>(supplier);
    }

    /**
     * Creates a context-aware reactive property.
     *
     * @param function The function that takes MenuContext and provides the value
     * @param <T>      The type
     * @return A reactive property wrapping the function
     */
    @NonNull
    public static <T> ReactiveProperty<T> ofContext(@NonNull Function<MenuContext, T> function) {
        return new ReactiveProperty<T>(null) {
            private T cachedValue;
            private boolean hasCached = false;
            private boolean isDirty = true;

            @Override
            public T get(MenuContext context) {
                if (this.isDirty || !this.hasCached) {
                    this.cachedValue = function.apply(context);
                    this.isDirty = false;
                    this.hasCached = true;
                }
                return this.cachedValue;
            }

            @Override
            public void invalidate() {
                this.isDirty = true;
                this.hasCached = false;
            }
        };
    }

    /**
     * Gets the current value, evaluating the supplier if dirty or first access.
     *
     * @param context The menu context (provides access to viewer and menu)
     * @return The current value
     */
    public T get(MenuContext context) {
        if (this.dirty || !this.hasCachedValue) {
            this.cachedValue = this.supplier.get();
            this.dirty = false;
            this.hasCachedValue = true;
        }
        return this.cachedValue;
    }

    /**
     * Marks this property as dirty, forcing re-evaluation on next get().
     */
    public void invalidate() {
        this.dirty = true;
        this.hasCachedValue = false;
    }

    /**
     * Checks if this property is reactive (not a constant).
     *
     * @return true if the value can change over time
     */
    public boolean isReactive() {
        // Simple heuristic: if value changes between calls, it's reactive
        // For now, we'll assume all supplier-based properties are potentially reactive
        return this.supplier != null;
    }
}
