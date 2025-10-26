package eu.okaeri.menu.state;

import eu.okaeri.menu.MenuContext;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wraps a value that can be static or reactive (supplier-based).
 * Reactive values are cached per-player in ViewerState until invalidated.
 *
 * @param <T> The type of the wrapped value
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ViewerProp<T> {

    private final Supplier<T> supplier;
    private final Function<MenuContext, T> function;

    /**
     * Creates a static reactive property (value never changes).
     *
     * @param staticValue The static value
     * @param <T>         The type
     * @return A reactive property wrapping the static value
     */
    @NonNull
    public static <T> ViewerProp<T> of(T staticValue) {
        return new ViewerProp<>(() -> staticValue, null);
    }

    /**
     * Creates a reactive property from a supplier.
     *
     * @param supplier The supplier providing the value
     * @param <T>      The type
     * @return A reactive property wrapping the supplier
     */
    @NonNull
    public static <T> ViewerProp<T> of(@NonNull Supplier<T> supplier) {
        return new ViewerProp<>(supplier, null);
    }

    /**
     * Creates a context-aware reactive property.
     *
     * @param function The function that takes MenuContext and provides the value
     * @param <T>      The type
     * @return A reactive property wrapping the function
     */
    @NonNull
    public static <T> ViewerProp<T> ofContext(@NonNull Function<MenuContext, T> function) {
        return new ViewerProp<>(null, function);
    }

    /**
     * Gets the current value, using per-player cache from ViewerState.
     * This ensures each player gets their own cached value.
     *
     * @param context The menu context (provides access to viewer and menu)
     * @return The current value
     */
    public T get(MenuContext context) {
        // Use per-player cache in ViewerState
        return context.getViewerState().getProp(this, () -> {
            if (this.function != null) {
                return this.function.apply(context);
            } else if (this.supplier != null) {
                return this.supplier.get();
            }
            return null;
        });
    }

    /**
     * Checks if this property is reactive (not a constant).
     *
     * @return true if the value can change over time
     */
    public boolean isReactive() {
        // Check if this is a function or supplier (not just a static value)
        return (this.function != null) || (this.supplier != null);
    }
}
