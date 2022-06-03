package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.function.Function;
import java.util.function.Supplier;

@NoArgsConstructor
public class MenuItemBuilder<V, I, C> {

    private String position = "-1";
    private ClickHandler<V, I, C> clickHandler;
    private DisplayProvider<V, I, C> displayProvider;

    public MenuItemBuilder<V, I, C> position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuItemBuilder<V, I, C> position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuItemBuilder<V, I, C> display(@NonNull DisplayProvider<V, I, C> displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuItemBuilder<V, I, C> display(@NonNull Supplier<I> displaySupplier) {
        this.displayProvider = (viewer, menuItem) -> displaySupplier.get();
        return this;
    }

    public MenuItemBuilder<V, I, C> display(@NonNull Function<MenuItemMeta<V, I,C>, I> displayFunction) {
        this.displayProvider = (viewer, menuItem) -> displayFunction.apply(menuItem);
        return this;
    }

    public MenuItemBuilder<V, I, C> click(@NonNull ClickHandler<V, I, C> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public MenuItemMeta<V, I, C> build() {
        return new MenuItemMeta<>(this.position, this.clickHandler, this.displayProvider);
    }
}
