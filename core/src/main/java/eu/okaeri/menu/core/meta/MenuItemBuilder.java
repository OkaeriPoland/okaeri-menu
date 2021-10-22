package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import eu.okaeri.menu.core.handler.SimpleClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class MenuItemBuilder<V, I> {

    private String display;
    private String name;
    private String position = "-1";
    private String description;
    private ClickHandler<V, I> clickHandler;
    private DisplayProvider<V, I> displayProvider;

    public MenuItemBuilder<V, I> display(@NonNull String display) {
        this.display = display;
        return this;
    }

    public MenuItemBuilder<V, I> name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public MenuItemBuilder<V, I> position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuItemBuilder<V, I> position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuItemBuilder<V, I> description(@NonNull String description) {
        this.description = description;
        return this;
    }

    public MenuItemBuilder<V, I> clickHandler(@NonNull ClickHandler<V, I> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public MenuItemBuilder<V, I> clickHandler(@NonNull SimpleClickHandler<V, I> clickHandler) {
        this.clickHandler = (viewer, menuItem, item, slot) -> {
            clickHandler.onClick(viewer, menuItem);
            return false;
        };
        return this;
    }

    public MenuItemBuilder<V, I> displayProvider(@NonNull DisplayProvider<V, I> displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuItemMeta<V, I> build() {
        return new MenuItemMeta<>(this.display, this.name, this.position, this.description, this.clickHandler, this.displayProvider);
    }
}
