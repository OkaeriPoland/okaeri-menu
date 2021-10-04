package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.InventoryDisplayProvider;
import eu.okaeri.menu.core.MenuProvider;
import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuDeclaration;
import eu.okaeri.menu.core.meta.MenuItemDeclaration;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BukkitMenuProvider implements MenuProvider<HumanEntity, ItemStack, BukkitMenu> {

    private final Map<Inventory, BukkitMenuInstance> knownMenuMap = new HashMap<>();
    private final DisplayProvider<HumanEntity, ItemStack> defaultDisplayProvider;

    public void trackInstance(@NonNull Inventory inventory, @NonNull BukkitMenuInstance menuInstance) {
        this.knownMenuMap.put(inventory, menuInstance);
    }

    public boolean knowsInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.containsKey(inventory);
    }

    public BukkitMenuInstance findInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.get(inventory);
    }

    public BukkitMenuInstance removeInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.remove(inventory);
    }

    public static BukkitMenuProvider create(@NonNull Plugin plugin) {
        return create(plugin, new InventoryDisplayProvider());
    }

    private static BukkitMenuProvider create(@NonNull Plugin plugin, @NonNull DisplayProvider<HumanEntity, ItemStack> displayProvider) {

        BukkitMenuProvider provider = new BukkitMenuProvider(displayProvider);
        BukkitMenuListener listener = new BukkitMenuListener(provider);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(listener, plugin);

        return provider;
    }

    @Override
    public BukkitMenu create(@NonNull MenuDeclaration<HumanEntity, ItemStack> menu) {

        Map<Integer, MenuItemDeclaration<HumanEntity, ItemStack>> itemMap = new LinkedHashMap<>();
        Map<Integer, DisplayProvider<HumanEntity, ItemStack>> providerMap = new LinkedHashMap<>();
        DisplayProvider<HumanEntity, ItemStack> menuDisplayProvider = menu.getDisplayProvider();

        int size = menu.getMenuChestSize();
        int lastItemPosition = 0;

        for (MenuItemDeclaration<HumanEntity, ItemStack> item : menu.getItems()) {

            int[] itemPositions = item.getPositionAsIntArr();

            for (int itemPosition : itemPositions) {

                if (itemPosition == -1) {
                    itemPosition = lastItemPosition + 1;
                }

                if (itemPosition > size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + itemPosition + " > " + size + ")");
                }

                DisplayProvider<HumanEntity, ItemStack> itemDisplayProvider = item.getDisplayProvider();
                DisplayProvider<HumanEntity, ItemStack> currentDisplayProvider = (itemDisplayProvider != null)
                        ? itemDisplayProvider
                        : ((menuDisplayProvider != null)
                        ? menuDisplayProvider
                        : this.defaultDisplayProvider);

                if (providerMap.put(itemPosition, currentDisplayProvider) != null) {
                    throw new IllegalArgumentException("item position cannot be overridden: " + itemPosition);
                }

                lastItemPosition = itemPosition;
                itemMap.put(itemPosition, item);
            }
        }

        return new BukkitMenu(menu, itemMap, providerMap, this);
    }
}
