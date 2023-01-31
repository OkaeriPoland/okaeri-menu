package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.handler.ClickHandler;
import eu.okaeri.menu.bukkit.handler.CloseHandler;
import eu.okaeri.menu.bukkit.handler.UpdateHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NoArgsConstructor
public class MenuBuilder {

    private String name;
    private String rows = "-1";
    private Duration update;
    private UpdateHandler updateHook;

    private DisplayProvider displayProvider;
    private List<MenuItemMeta> items = new ArrayList<>();
    private List<MenuInputMeta> inputs = new ArrayList<>();

    private ClickHandler outsideClickHandler;
    private ClickHandler fallbackClickHandler;
    private CloseHandler closeHandler;

    public MenuBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public MenuBuilder rows(@NonNull String rows) {
        this.rows = rows;
        return this;
    }

    public MenuBuilder rows(int rows) {
        this.rows = String.valueOf(rows);
        return this;
    }

    public MenuBuilder update(@NonNull Duration refresh) {
        this.update = refresh;
        return this;
    }

    public MenuBuilder update(@NonNull Duration refresh, @NonNull UpdateHandler refreshHook) {
        this.update = refresh;
        this.updateHook = refreshHook;
        return this;
    }

    public MenuBuilder updateHook(@NonNull UpdateHandler refreshHook) {
        if (this.update == null) {
            throw new IllegalStateException("Set refresh rate before defining custom refreshHook!");
        }
        this.updateHook = refreshHook;
        return this;
    }

    public MenuBuilder displayProvider(@NonNull DisplayProvider displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuBuilder items(@NonNull Iterable<MenuItemMeta> items) {
        return this.items(items, false);
    }

    public MenuBuilder items(@NonNull Stream<MenuItemMeta> stream) {
        return this.items(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder items(@NonNull Iterable<MenuItemMeta> items, boolean replace) {

        List<MenuItemMeta> list;
        if (items instanceof List) {
            list = (List<MenuItemMeta>) items;
        } else {
            list = new ArrayList<>();
            Iterator<MenuItemMeta> iterator = items.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.items = list;
        } else {
            this.items.addAll(list);
        }

        return this;
    }

    public <A> MenuBuilder items(@NonNull Iterable<A> iterable, @NonNull Function<A, MenuItemBuilder> function) {
        return this.items(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    public <A> MenuBuilder items(@NonNull Stream<A> stream, @NonNull Function<A, MenuItemBuilder> function) {
        return this.items(stream.map(function).map(MenuItemBuilder::build).collect(Collectors.toList()), false);
    }

    public MenuBuilder item(@NonNull MenuItemMeta item) {
        this.items.add(item);
        return this;
    }

    public MenuBuilder inputs(@NonNull Iterable<MenuInputMeta> inputs) {
        return this.inputs(inputs, false);
    }

    public MenuBuilder inputs(@NonNull Stream<MenuInputMeta> stream) {
        return this.inputs(stream.collect(Collectors.toList()), false);
    }

    public MenuBuilder inputs(@NonNull Iterable<MenuInputMeta> inputs, boolean replace) {

        List<MenuInputMeta> list;
        if (inputs instanceof List) {
            list = (List<MenuInputMeta>) inputs;
        } else {
            list = new ArrayList<>();
            Iterator<MenuInputMeta> iterator = inputs.iterator();
            iterator.forEachRemaining(list::add);
        }

        if (replace) {
            this.inputs = list;
        } else {
            this.inputs.addAll(list);
        }

        return this;
    }

    public <A> MenuBuilder inputs(@NonNull Iterable<A> iterable, Function<A, MenuInputBuilder> function) {
        return this.inputs(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    public <A> MenuBuilder inputs(@NonNull Stream<A> stream, Function<A, MenuInputBuilder> function) {
        return this.inputs(stream.map(function).map(MenuInputBuilder::build).collect(Collectors.toList()), false);
    }

    public MenuBuilder input(@NonNull MenuInputMeta input) {
        this.inputs.add(input);
        return this;
    }

    public MenuBuilder outsideClick(@NonNull ClickHandler outsideClickHandler) {
        this.outsideClickHandler = outsideClickHandler;
        return this;
    }

    public MenuBuilder fallbackClick(@NonNull ClickHandler fallbackClickHandler) {
        this.fallbackClickHandler = fallbackClickHandler;
        return this;
    }

    public MenuBuilder close(@NonNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public MenuMeta build() {
        return new MenuMeta(
            this.name,
            this.rows,
            this.update,
            this.updateHook,
            this.displayProvider,
            Collections.unmodifiableList(this.items),
            Collections.unmodifiableList(this.inputs),
            this.outsideClickHandler,
            this.fallbackClickHandler,
            this.closeHandler
        );
    }
}
