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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NoArgsConstructor
public class MenuBuilder<V, I, C> {

    private String name;
    private String rows = "-1";

    private DisplayProvider<V, I, C> displayProvider;
    private List<MenuItemMeta<V, I, C>> items = new ArrayList<>();
    private List<MenuInputMeta<V, I, C>> inputs = new ArrayList<>();

    private OutsideClickHandler<V, I, C> outsideClickHandler;
    private FallbackClickHandler<V, I, C> fallbackClickHandler;
    private CloseHandler<V> closeHandler;

    public MenuBuilder<V, I, C> name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public MenuBuilder<V, I, C> rows(@NonNull String rows) {
        this.rows = rows;
        return this;
    }

    public MenuBuilder<V, I, C> rows(int rows) {
        this.rows = String.valueOf(rows);
        return this;
    }

    public MenuBuilder<V, I, C> displayProvider(@NonNull DisplayProvider<V, I, C> displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuBuilder<V, I, C> items(@NonNull Iterable<MenuItemMeta<V, I, C>> items) {
        return this.items(items, false);
    }

    public MenuBuilder<V, I, C> items(@NonNull Stream<MenuItemMeta<V, I, C>> stream) {
        return this.items(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I, C> items(@NonNull Iterable<MenuItemMeta<V, I, C>> items, boolean replace) {

        List<MenuItemMeta<V, I, C>> list;
        if (items instanceof List) {
            list = (List<MenuItemMeta<V, I, C>>) items;
        } else {
            list = new ArrayList<>();
            Iterator<MenuItemMeta<V, I, C>> iterator = items.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.items = list;
        } else {
            this.items.addAll(list);
        }

        return this;
    }

    public <A> MenuBuilder<V, I, C> items(@NonNull Iterable<A> iterable, @NonNull Function<A, MenuItemBuilder<V, I, C>> function) {
        return this.items(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    public <A> MenuBuilder<V, I, C> items(@NonNull Stream<A> stream, @NonNull Function<A, MenuItemBuilder<V, I, C>> function) {
        return this.items(stream.map(function).map(MenuItemBuilder::build).collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I, C> item(@NonNull MenuItemMeta<V, I, C> item) {
        this.items.add(item);
        return this;
    }

    public MenuBuilder<V, I, C> inputs(@NonNull Iterable<MenuInputMeta<V, I, C>> inputs) {
        return this.inputs(inputs, false);
    }

    public MenuBuilder<V, I, C> inputs(@NonNull Stream<MenuInputMeta<V, I, C>> stream) {
        return this.inputs(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I, C> inputs(@NonNull Iterable<MenuInputMeta<V, I, C>> inputs, boolean replace) {

        List<MenuInputMeta<V, I, C>> list;
        if (inputs instanceof List) {
            list = (List<MenuInputMeta<V, I, C>>) inputs;
        } else {
            list = new ArrayList<>();
            Iterator<MenuInputMeta<V, I, C>> iterator = inputs.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.inputs = list;
        } else {
            this.inputs.addAll(list);
        }

        return this;
    }

    public <A> MenuBuilder<V, I, C> inputs(@NonNull Iterable<A> iterable, Function<A, MenuInputBuilder<V, I, C>> function) {
        return this.inputs(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    public <A> MenuBuilder<V, I, C> inputs(@NonNull Stream<A> stream, Function<A, MenuInputBuilder<V, I, C>> function) {
        return this.inputs(stream.map(function).map(MenuInputBuilder::build).collect(Collectors.toList()), false);
    }

    public MenuBuilder<V, I, C> input(@NonNull MenuInputMeta<V, I, C> input) {
        this.inputs.add(input);
        return this;
    }

    public MenuBuilder<V, I, C> outsideClickHandler(@NonNull OutsideClickHandler<V, I, C> outsideClickHandler) {
        this.outsideClickHandler = outsideClickHandler;
        return this;
    }

    public MenuBuilder<V, I, C> fallbackClickHandler(@NonNull FallbackClickHandler<V, I, C> fallbackClickHandler) {
        this.fallbackClickHandler = fallbackClickHandler;
        return this;
    }

    public MenuBuilder<V, I, C> closeHandler(@NonNull CloseHandler<V> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public MenuMeta<V, I, C> build() {
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
