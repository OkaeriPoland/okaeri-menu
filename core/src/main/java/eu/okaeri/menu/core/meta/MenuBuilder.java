package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.CloseHandler;
import eu.okaeri.menu.core.handler.FallbackClickHandler;
import eu.okaeri.menu.core.handler.OutsideClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
public class MenuBuilder<V, I> {

    private String name;
    private String rows = "-1";

    private DisplayProvider<V, I> displayProvider;
    private List<MenuItemMeta<V, I>> items = new ArrayList<>();
    private List<MenuInputMeta<V, I>> inputs = new ArrayList<>();

    private OutsideClickHandler<V> outsideClickHandler;
    private FallbackClickHandler<V, I> fallbackClickHandler;
    private CloseHandler<V> closeHandler;

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

    public MenuBuilder<V, I> displayProvider(@NonNull DisplayProvider<V, I> displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuBuilder<V, I> items(@NonNull List<MenuItemMeta<V, I>> items) {
        this.items = items;
        return this;
    }

    public MenuBuilder<V, I> item(@NonNull MenuItemMeta<V, I> item) {
        this.items.add(item);
        return this;
    }

    public MenuBuilder<V, I> inputs(@NonNull List<MenuInputMeta<V, I>> inputs) {
        this.inputs = inputs;
        return this;
    }

    public MenuBuilder<V, I> input(@NonNull MenuInputMeta<V, I> input) {
        this.inputs.add(input);
        return this;
    }

    public MenuBuilder<V, I> outsideClickHandler(@NonNull OutsideClickHandler<V> outsideClickHandler) {
        this.outsideClickHandler = outsideClickHandler;
        return this;
    }

    public MenuBuilder<V, I> fallbackClickHandler(@NonNull FallbackClickHandler<V, I> fallbackClickHandler) {
        this.fallbackClickHandler = fallbackClickHandler;
        return this;
    }

    public MenuBuilder<V, I> closeHandler(@NonNull CloseHandler<V> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public MenuMeta<V, I> build() {
        return new MenuMeta<>(
                this.name,
                this.rows,
                this.displayProvider,
                Collections.unmodifiableList(this.items),
                Collections.unmodifiableList(this.inputs),
                this.outsideClickHandler,
                this.fallbackClickHandler,
                this.closeHandler
        );
    }
}
