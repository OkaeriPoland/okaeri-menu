package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.CloseHandler;
import eu.okaeri.menu.core.handler.FallbackClickHandler;
import eu.okaeri.menu.core.handler.OutsideClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
public class MenuBuilder<V, I> {

    private String name;
    private String rows = "-1";

    private DisplayProvider<V, I> displayProvider;
    private List<MenuItemMeta<V, I>> items = new ArrayList<>();
    private List<MenuInputMeta<V, I>> inputs = new ArrayList<>();

    private OutsideClickHandler<V, I> outsideClickHandler;
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

    public MenuBuilder<V, I> items(@NonNull Iterable<MenuItemMeta<V, I>> items) {
        return this.items(items, false);
    }

    public MenuBuilder<V, I> items(@NonNull Stream<MenuItemMeta<V, I>> stream) {
        return this.items(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I> items(@NonNull Iterable<MenuItemMeta<V, I>> items, boolean replace) {

        List<MenuItemMeta<V, I>> list;
        if (items instanceof List) {
            list = (List<MenuItemMeta<V, I>>) items;
        } else {
            list = new ArrayList<>();
            Iterator<MenuItemMeta<V, I>> iterator = items.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.items = list;
        } else {
            this.items.addAll(list);
        }

        return this;
    }

    public MenuBuilder<V, I> item(@NonNull MenuItemMeta<V, I> item) {
        this.items.add(item);
        return this;
    }

    public MenuBuilder<V, I> inputs(@NonNull Iterable<MenuInputMeta<V, I>> inputs) {
        return this.inputs(inputs, false);
    }

    public MenuBuilder<V, I> inputs(@NonNull Stream<MenuInputMeta<V, I>> stream) {
        return this.inputs(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I> inputs(@NonNull Iterable<MenuInputMeta<V, I>> inputs, boolean replace) {

        List<MenuInputMeta<V, I>> list;
        if (inputs instanceof List) {
            list = (List<MenuInputMeta<V, I>>) inputs;
        } else {
            list = new ArrayList<>();
            Iterator<MenuInputMeta<V, I>> iterator = inputs.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.inputs = list;
        } else {
            this.inputs.addAll(list);
        }

        return this;
    }

    public MenuBuilder<V, I> input(@NonNull MenuInputMeta<V, I> input) {
        this.inputs.add(input);
        return this;
    }

    public MenuBuilder<V, I> outsideClickHandler(@NonNull OutsideClickHandler<V, I> outsideClickHandler) {
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
