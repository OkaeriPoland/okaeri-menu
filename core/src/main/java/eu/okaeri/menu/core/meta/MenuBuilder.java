package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class MenuBuilder<V, I> {

    private String name;
    private String rows = "-1";
    private DisplayProvider displayProvider;
    private List<MenuItemDeclaration<V, I>> items = new ArrayList<>();

    public MenuBuilder<V, I> name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public MenuBuilder<V, I> rows(@NonNull String rows) {
        this.rows = rows;
        return this;
    }

    public MenuBuilder<V, I> rows(int rows) {
        this.rows = String.valueOf(rows);
        return this;
    }

    public MenuBuilder<V, I> displayProvider(@NonNull DisplayProvider displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuBuilder<V, I> items(@NonNull List<MenuItemDeclaration<V, I>> items) {
        this.items = items;
        return this;
    }

    public MenuBuilder<V, I> item(@NonNull MenuItemDeclaration<V, I> item) {
        this.items.add(item);
        return this;
    }

    @SuppressWarnings("unchecked")
    public MenuDeclaration<V, I> build() {
        return new MenuDeclaration<>(this.name, this.rows, this.displayProvider, Collections.unmodifiableList(this.items));
    }
}
