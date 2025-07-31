package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.OkaeriMenu;
import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.handler.ClickHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NoArgsConstructor
public class MenuItemBuilder {

    private String position = "-1";
    private ClickHandler clickHandler;
    private DisplayProvider displayProvider;

    public MenuItemBuilder position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuItemBuilder position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuItemBuilder display(@NonNull DisplayProvider displayProvider) {
        this.displayProvider = displayProvider;
        return this;
    }

    public MenuItemBuilder display(@NonNull MenuDisplayStack displayStack) {
        this.displayProvider = (viewer, menuItem) -> displayStack.apply(viewer);
        return this;
    }

    public MenuItemBuilder stack(@NonNull Consumer<MenuDisplayStack> consumer) {
        this.displayProvider = (viewer, menuItem) -> {
            MenuDisplayStack stack = OkaeriMenu.stack();
            consumer.accept(stack);
            return stack.apply(viewer);
        };
        return this;
    }

    public MenuItemBuilder display(@NonNull Supplier<ItemStack> displaySupplier) {
        this.displayProvider = (viewer, menuItem) -> displaySupplier.get();
        return this;
    }

    public MenuItemBuilder display(@NonNull Function<MenuItemMeta, ItemStack> displayFunction) {
        this.displayProvider = (viewer, menuItem) -> displayFunction.apply(menuItem);
        return this;
    }

    public MenuItemBuilder click(@NonNull ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public MenuItemMeta build() {
        return new MenuItemMeta(this.position, this.clickHandler, this.displayProvider);
    }
}
