package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.*;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Data
public class OkaeriMenu {

    private final MenuMeta meta;
    private final Map<Integer, MenuItemMeta> itemMap;
    private final Map<Integer, MenuInputMeta> inputMap;
    private final Map<Integer, DisplayProvider> providerMap;
    private final MenuProvider provider;

    public static MenuBuilder builder() {
        return new MenuBuilder();
    }

    public static MenuItemBuilder item() {
        return new MenuItemBuilder();
    }

    public static MenuDisplayStack stack() {
        return new MenuDisplayStack();
    }

    public static MenuInputBuilder input() {
        return new MenuInputBuilder();
    }

    public static MenuMeta editor(@NonNull List<ItemStack> stacks, String name, @NonNull Consumer<List<ItemStack>> callback) {

        if (stacks.size() > (6 * 9)) {
            throw new IllegalArgumentException("Stacks is too large for the 6 row editor (size: " + stacks + ")");
        }

        return OkaeriMenu.builder()
            .rows(6)
            .name(name)
            .items(stacks.stream()
                .map(item -> OkaeriMenu.item()
                    .display(() -> item)
                    .click(ctx -> ctx.setAllowPickup(true))
                    .build()))
            .close(ctx -> callback.accept(Arrays.stream(ctx.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(item -> (item.getType() != Material.AIR) && !item.getType().name().contains("_AIR"))
                .collect(Collectors.toList())))
            .fallbackClick(ctx -> {
                ctx.setAllowInput(true);
                ctx.setAllowPickup(true);
            })
            .build();
    }

    public MenuInstance createInstance() {
        Inventory inventory = this.getMeta().getFactory().apply(this.getMeta());
        MenuInstance menuInstance = new MenuInstance(inventory, this);
        this.getProvider().trackInstance(inventory, menuInstance);
        return menuInstance;
    }

    public MenuInstance render(@NonNull HumanEntity viewer) {
        return this.createInstance().render(viewer);
    }

    /**
     * @deprecated Forces synchronous render. Consider using asynchronous {@link #render(HumanEntity)}
     * then synchronous {@link MenuInstance#open(HumanEntity)} instead.
     */
    @Deprecated
    public MenuInstance open(@NonNull HumanEntity viewer) {
        return this.render(viewer).open(viewer);
    }
}
