package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import eu.okaeri.menu.core.handler.SimpleClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class MenuItemBuilder<V, I, C> {

    private String display;
    private String name;
    private String position = "-1";
    private String description;
    private ClickHandler<V, I, C> clickHandler;
    private DisplayProvider<V, I, C> displayProvider;

    public MenuItemBuilder<V, I, C> display(@NonNull String display) {
        this.display = display;
        return this;
    }

    public MenuItemBuilder<V, I, C> name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public MenuItemBuilder<V, I, C> position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuItemBuilder<V, I, C> position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuItemBuilder<V, I, C> description(@NonNull String description) {
        this.description = description;
        return this;
    }

    public MenuItemBuilder<V, I, C> clickHandler(@NonNull ClickHandler<V, I, C> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public MenuItemBuilder<V, I, C> clickHandler(@NonNull SimpleClickHandler<V, I, C> clickHandler) {
        this.clickHandler = (viewer, menuItem, item, slot, clickType) -> {
            clickHandler.onClick(viewer, menuItem, clickType);
            return false;
        };
        return this;
    }

    public MenuItemBuilder<V, I, C> displayProvider(@NonNull DisplayProvider<V, I, C> displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuItemMeta<V, I, C> build() {
        return new MenuItemMeta<>(this.display, this.name, this.position, this.description, this.clickHandler, this.displayProvider);
    }
}
