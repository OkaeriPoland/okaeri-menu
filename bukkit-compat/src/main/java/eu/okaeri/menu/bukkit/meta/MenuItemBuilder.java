package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.handler.ClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for creating menu items in the legacy 1.x API.
 * <p>
 * This class is part of the compatibility layer and will be removed in a future version.
 * For new code, use the 2.0 API with {@link eu.okaeri.menu.item.MenuItem.Builder} instead.
 *
 * @deprecated Use {@link eu.okaeri.menu.item.MenuItem.Builder} in the 2.0 API
 * @see eu.okaeri.menu.item.MenuItem.Builder
 */
@Deprecated(since = "0.0.18", forRemoval = true)
@NoArgsConstructor
public class MenuItemBuilder {

    private String position = "-1";
    private ClickHandler clickHandler;
    private DisplayProvider displayProvider;

    /**
     * Sets the position of the item in the menu using a string representation.
     * The string can be a single number (e.g., "5") or comma-separated numbers (e.g., "1,2,3").
     *
     * @param position the position string
     * @return this builder for chaining
     */
    public MenuItemBuilder position(@NonNull String position) {
        this.position = position;
        return this;
    }

    /**
     * Sets the position of the item in the menu using an integer index.
     *
     * @param position the position index
     * @return this builder for chaining
     */
    public MenuItemBuilder position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    /**
     * Sets the display provider that generates the ItemStack for this menu item.
     *
     * @param displayProvider the display provider
     * @return this builder for chaining
     */
    public MenuItemBuilder display(@NonNull DisplayProvider displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    /**
     * Sets the display using a MenuDisplayStack configuration.
     * The stack will be applied to generate the ItemStack when displayed.
     *
     * @param displayStack the display stack configuration
     * @return this builder for chaining
     */
    public MenuItemBuilder display(@NonNull MenuDisplayStack displayStack) {
        this.displayProvider = (viewer, menuItem) -> displayStack.apply(viewer);
        return this;
    }

    /**
     * Creates a new MenuDisplayStack and passes it to the consumer for configuration.
     * This is a convenient way to configure the display inline.
     *
     * @param consumer the consumer that configures the display stack
     * @return this builder for chaining
     */
    public MenuItemBuilder stack(@NonNull Consumer<MenuDisplayStack> consumer) {
        this.displayProvider = (viewer, menuItem) -> {
            MenuDisplayStack stack = new MenuDisplayStack();
            consumer.accept(stack);
            return stack.apply(viewer);
        };
        return this;
    }

    /**
     * Sets the display using a simple ItemStack supplier.
     * The supplier will be called each time the item needs to be displayed.
     *
     * @param displaySupplier the ItemStack supplier
     * @return this builder for chaining
     */
    public MenuItemBuilder display(@NonNull Supplier<ItemStack> displaySupplier) {
        this.displayProvider = (viewer, menuItem) -> displaySupplier.get();
        return this;
    }

    /**
     * Sets the display using a function that receives the MenuItemMeta.
     * This allows the display to access metadata about the item.
     *
     * @param displayFunction the function that generates the ItemStack
     * @return this builder for chaining
     */
    public MenuItemBuilder display(@NonNull Function<MenuItemMeta, ItemStack> displayFunction) {
        this.displayProvider = (viewer, menuItem) -> displayFunction.apply(menuItem);
        return this;
    }

    /**
     * Sets the click handler for this menu item.
     * The handler will be invoked when a player clicks the item.
     *
     * @param clickHandler the click handler
     * @return this builder for chaining
     */
    public MenuItemBuilder click(@NonNull ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    /**
     * Builds and returns the configured MenuItemMeta.
     *
     * @return the configured MenuItemMeta
     */
    public MenuItemMeta build() {
        return new MenuItemMeta(this.position, this.clickHandler, this.displayProvider);
    }
}
