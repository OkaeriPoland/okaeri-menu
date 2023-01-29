package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.*;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.Bukkit;
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
public class BukkitMenu {

    private final MenuMeta<HumanEntity, ItemStack, BukkitMenuContext> meta;
    private final Map<Integer, MenuItemMeta<HumanEntity, ItemStack, BukkitMenuContext>> itemMap;
    private final Map<Integer, MenuInputMeta<HumanEntity, ItemStack, BukkitMenuContext>> inputMap;
    private final Map<Integer, DisplayProvider<HumanEntity, ItemStack, BukkitMenuContext>> providerMap;
    private final BukkitMenuProvider menuProvider;

    public static MenuBuilder<HumanEntity, ItemStack, BukkitMenuContext> builder() {
        return new MenuBuilder<>();
    }

    public static MenuItemBuilder<HumanEntity, ItemStack, BukkitMenuContext> item() {
        return new MenuItemBuilder<>();
    }

    public static MenuInputBuilder<HumanEntity, ItemStack, BukkitMenuContext> input() {
        return new MenuInputBuilder<>();
    }

    public static MenuMeta<HumanEntity, ItemStack, BukkitMenuContext> editor(@NonNull List<ItemStack> stacks, String name, @NonNull Consumer<List<ItemStack>> callback) {

        if (stacks.size() > (6 * 9)) {
            throw new IllegalArgumentException("Stacks is too large for the 6 row editor (size: " + stacks + ")");
        }

        return BukkitMenu.builder()
            .rows(6)
            .name(name)
            .items(stacks.stream()
                .map(item -> BukkitMenu.item()
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

    public BukkitMenuInstance createInstance() {

        int size = this.meta.getMenuChestSize();
        Inventory inventory = Bukkit.createInventory(null, size, this.meta.getName());

        BukkitMenuInstance menuInstance = new BukkitMenuInstance(inventory, this);
        this.menuProvider.trackInstance(inventory, menuInstance);

        return menuInstance;
    }

    public BukkitMenuInstance open(@NonNull HumanEntity viewer) {
        BukkitMenuInstance menuInstance = this.createInstance().render(viewer);
        viewer.openInventory(menuInstance.getInventory());
        return menuInstance;
    }
}
