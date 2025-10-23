package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.handler.ClickHandler;
import eu.okaeri.menu.bukkit.handler.CloseHandler;
import eu.okaeri.menu.bukkit.handler.UpdateHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Legacy menu builder for the 1.x API.
 * <p>
 * This builder provides a fluent API for configuring menus using the old metadata-based approach.
 * All configuration is stored in private fields until {@link #build()} is called, which creates
 * a {@link MenuMeta} object containing all the settings.
 * <p>
 * This class is part of the compatibility layer and will be removed in a future version.
 * For new code, use the 2.0 API with {@link eu.okaeri.menu.bukkit.Menu.Builder} instead.
 *
 * @deprecated Use {@link eu.okaeri.menu.bukkit.Menu.Builder} in the 2.0 API
 * @see eu.okaeri.menu.bukkit.Menu.Builder
 * @see MenuMeta
 */
@Deprecated(since = "0.0.18", forRemoval = true)
@NoArgsConstructor
public class MenuBuilder {

    private Function<MenuMeta, Inventory> factory = meta -> Bukkit.createInventory(null, meta.getMenuChestSize(), meta.getName());
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

    /**
     * Sets a custom inventory factory function.
     *
     * @param factory the factory function that creates an Inventory from MenuMeta
     * @return this builder for method chaining
     */
    public MenuBuilder factory(@NonNull Function<MenuMeta, Inventory> factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Sets the inventory type to use for the menu.
     * <p>
     * This is a convenience method that sets the factory to create an inventory
     * of the specified type.
     *
     * @param type the Bukkit inventory type
     * @return this builder for method chaining
     */
    public MenuBuilder type(@NonNull InventoryType type) {
        this.factory = meta -> Bukkit.createInventory(null, type, meta.getName());
        return this;
    }

    /**
     * Sets the menu title/name.
     *
     * @param name the menu title
     * @return this builder for method chaining
     */
    public MenuBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the number of rows as a string.
     * <p>
     * Can be a numeric string or "-1" for automatic sizing based on items.
     *
     * @param rows the number of rows as a string
     * @return this builder for method chaining
     */
    public MenuBuilder rows(@NonNull String rows) {
        this.rows = rows;
        return this;
    }

    /**
     * Sets the number of rows.
     * <p>
     * Use -1 for automatic sizing based on items.
     *
     * @param rows the number of rows
     * @return this builder for method chaining
     */
    public MenuBuilder rows(int rows) {
        this.rows = String.valueOf(rows);
        return this;
    }

    /**
     * Sets the menu update interval.
     *
     * @param update the duration between updates
     * @return this builder for method chaining
     */
    public MenuBuilder update(@NonNull Duration update) {
        this.update = update;
        return this;
    }

    /**
     * Sets the menu update interval and update handler.
     *
     * @param update the duration between updates
     * @param refreshHook the handler to call on each update
     * @return this builder for method chaining
     */
    public MenuBuilder update(@NonNull Duration update, @NonNull UpdateHandler refreshHook) {
        this.update = update;
        this.updateHook = refreshHook;
        return this;
    }

    /**
     * Sets the update handler.
     * <p>
     * The update interval must be set before calling this method.
     *
     * @param refreshHook the handler to call on each update
     * @return this builder for method chaining
     * @throws IllegalStateException if update interval is not set
     */
    public MenuBuilder updateHook(@NonNull UpdateHandler refreshHook) {
        if (this.update == null) {
            throw new IllegalStateException("Set refresh rate before defining custom refreshHook!");
        }
        this.updateHook = refreshHook;
        return this;
    }

    /**
     * Sets the display provider for dynamic item rendering.
     *
     * @param displayProvider the display provider
     * @return this builder for method chaining
     */
    public MenuBuilder displayProvider(@NonNull DisplayProvider displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    /**
     * Adds items to the menu.
     *
     * @param items the items to add
     * @return this builder for method chaining
     */
    public MenuBuilder items(@NonNull Iterable<MenuItemMeta> items) {
        return this.items(items, false);
    }

    /**
     * Adds items to the menu from a stream.
     *
     * @param stream the stream of items to add
     * @return this builder for method chaining
     */
    public MenuBuilder items(@NonNull Stream<MenuItemMeta> stream) {
        return this.items(stream.collect(Collectors.toList()), false);
    }

    /**
     * Adds or replaces items in the menu.
     *
     * @param items the items to add
     * @param replace if true, replaces all existing items; if false, adds to existing items
     * @return this builder for method chaining
     */
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

    /**
     * Adds items to the menu by transforming source objects.
     *
     * @param iterable the source objects
     * @param function the transformation function from source object to MenuItemBuilder
     * @param <A> the type of source objects
     * @return this builder for method chaining
     */
    public <A> MenuBuilder items(@NonNull Iterable<A> iterable, @NonNull Function<A, MenuItemBuilder> function) {
        return this.items(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    /**
     * Adds items to the menu by transforming source objects from a stream.
     *
     * @param stream the stream of source objects
     * @param function the transformation function from source object to MenuItemBuilder
     * @param <A> the type of source objects
     * @return this builder for method chaining
     */
    public <A> MenuBuilder items(@NonNull Stream<A> stream, @NonNull Function<A, MenuItemBuilder> function) {
        return this.items(stream.map(function).map(MenuItemBuilder::build).collect(Collectors.toList()), false);
    }

    /**
     * Adds a single item to the menu.
     *
     * @param item the item to add
     * @return this builder for method chaining
     */
    public MenuBuilder item(@NonNull MenuItemMeta item) {
        this.items.add(item);
        return this;
    }

    /**
     * Adds input slots to the menu.
     *
     * @param inputs the input slots to add
     * @return this builder for method chaining
     */
    public MenuBuilder inputs(@NonNull Iterable<MenuInputMeta> inputs) {
        return this.inputs(inputs, false);
    }

    /**
     * Adds input slots to the menu from a stream.
     *
     * @param stream the stream of input slots to add
     * @return this builder for method chaining
     */
    public MenuBuilder inputs(@NonNull Stream<MenuInputMeta> stream) {
        return this.inputs(stream.collect(Collectors.toList()), false);
    }

    /**
     * Adds or replaces input slots in the menu.
     *
     * @param inputs the input slots to add
     * @param replace if true, replaces all existing inputs; if false, adds to existing inputs
     * @return this builder for method chaining
     */
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

    /**
     * Adds input slots to the menu by transforming source objects.
     *
     * @param iterable the source objects
     * @param function the transformation function from source object to MenuInputBuilder
     * @param <A> the type of source objects
     * @return this builder for method chaining
     */
    public <A> MenuBuilder inputs(@NonNull Iterable<A> iterable, Function<A, MenuInputBuilder> function) {
        return this.inputs(StreamSupport.stream(iterable.spliterator(), false), function);
    }

    /**
     * Adds input slots to the menu by transforming source objects from a stream.
     *
     * @param stream the stream of source objects
     * @param function the transformation function from source object to MenuInputBuilder
     * @param <A> the type of source objects
     * @return this builder for method chaining
     */
    public <A> MenuBuilder inputs(@NonNull Stream<A> stream, Function<A, MenuInputBuilder> function) {
        return this.inputs(stream.map(function).map(MenuInputBuilder::build).collect(Collectors.toList()), false);
    }

    /**
     * Adds a single input slot to the menu.
     *
     * @param input the input slot to add
     * @return this builder for method chaining
     */
    public MenuBuilder input(@NonNull MenuInputMeta input) {
        this.inputs.add(input);
        return this;
    }

    /**
     * Sets the handler for clicks outside the menu inventory.
     *
     * @param outsideClickHandler the click handler
     * @return this builder for method chaining
     */
    public MenuBuilder outsideClick(@NonNull ClickHandler outsideClickHandler) {
        this.outsideClickHandler = outsideClickHandler;
        return this;
    }

    /**
     * Sets the fallback handler for clicks that don't match any item or input.
     *
     * @param fallbackClickHandler the click handler
     * @return this builder for method chaining
     */
    public MenuBuilder fallbackClick(@NonNull ClickHandler fallbackClickHandler) {
        this.fallbackClickHandler = fallbackClickHandler;
        return this;
    }

    /**
     * Sets the handler for menu close events.
     *
     * @param closeHandler the close handler
     * @return this builder for method chaining
     */
    public MenuBuilder close(@NonNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    /**
     * Builds the menu metadata object.
     * <p>
     * This creates a {@link MenuMeta} containing all the configured settings.
     * The actual conversion to the v2 Menu API happens in OkaeriMenu when opening the menu.
     *
     * @return the built menu metadata
     */
    public MenuMeta build() {
        return new MenuMeta(
            this.factory,
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
