package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.InventoryDisplayProvider;
import eu.okaeri.menu.core.MenuProvider;
import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuInputMeta;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import eu.okaeri.menu.core.meta.MenuMeta;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BukkitMenuProvider implements MenuProvider<HumanEntity, ItemStack, BukkitMenu> {

    private final Plugin plugin;
    private final Map<Inventory, BukkitMenuInstance> knownMenuMap = new HashMap<>();
    private final DisplayProvider<HumanEntity, ItemStack> defaultDisplayProvider;

    public static BukkitMenuProvider create(@NonNull Plugin plugin) {
        return create(plugin, new InventoryDisplayProvider());
    }

    private static BukkitMenuProvider create(@NonNull Plugin plugin, @NonNull DisplayProvider<HumanEntity, ItemStack> displayProvider) {

        BukkitMenuProvider provider = new BukkitMenuProvider(plugin, displayProvider);
        BukkitMenuListener listener = new BukkitMenuListener(plugin, provider);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(listener, plugin);

        return provider;
    }

    public void trackInstance(@NonNull Inventory inventory, @NonNull BukkitMenuInstance menuInstance) {
        this.knownMenuMap.put(inventory, menuInstance);
    }

    public boolean knowsInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.containsKey(inventory);
    }

    public Optional<BukkitMenuInstance> findInstance(@NonNull Inventory inventory) {
        return Optional.ofNullable(this.knownMenuMap.get(inventory));
    }

    @Nullable
    public BukkitMenuInstance removeInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.remove(inventory);
    }

    public void render(@NonNull HumanEntity viewer) {
        this.findInstance(viewer.getOpenInventory().getTopInventory()).ifPresent(instance -> instance.render(viewer));
    }

    public void close(@NonNull HumanEntity viewer) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, viewer::closeInventory);
    }

    @Override
    public BukkitMenu create(@NonNull MenuMeta<HumanEntity, ItemStack> menu) {

        Map<Integer, MenuItemMeta<HumanEntity, ItemStack>> itemMap = new LinkedHashMap<>();
        Map<Integer, MenuInputMeta<HumanEntity, ItemStack>> inputMap = new LinkedHashMap<>();
        Map<Integer, DisplayProvider<HumanEntity, ItemStack>> providerMap = new LinkedHashMap<>();
        DisplayProvider<HumanEntity, ItemStack> menuDisplayProvider = menu.getDisplayProvider();

        int size = menu.getMenuChestSize();
        int lastPosition = -1;

        for (MenuItemMeta<HumanEntity, ItemStack> item : menu.getItems()) {

            int[] positions = item.getPositionAsIntArr();

            for (int position : positions) {

                if (position == -1) {
                    position = lastPosition + 1;
                }

                if (position > size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + position + " > " + size + ")");
                }

                DisplayProvider<HumanEntity, ItemStack> itemDisplayProvider = item.getDisplayProvider();
                DisplayProvider<HumanEntity, ItemStack> currentDisplayProvider = (itemDisplayProvider != null)
                    ? itemDisplayProvider
                    : ((menuDisplayProvider != null)
                    ? menuDisplayProvider
                    : this.defaultDisplayProvider);

                if (providerMap.put(position, currentDisplayProvider) != null) {
                    throw new IllegalArgumentException("item position cannot be overridden: " + position);
                }

                lastPosition = position;
                itemMap.put(position, item);
            }
        }

        for (MenuInputMeta<HumanEntity, ItemStack> input : menu.getInputs()) {

            int[] positions = input.getPositionAsIntArr();

            for (int position : positions) {

                if (position == -1) {
                    position = lastPosition + 1;
                }

                if (position > size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + position + " > " + size + ")");
                }

                if (itemMap.containsKey(position) || inputMap.containsKey(position)) {
                    throw new IllegalArgumentException("item/input position cannot be overridden: " + position);
                }

                lastPosition = position;
                inputMap.put(position, input);
            }
        }

        return new BukkitMenu(menu, itemMap, inputMap, providerMap, this);
    }
}
