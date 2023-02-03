package eu.okaeri.menu.bukkit;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.meta.MenuInputMeta;
import eu.okaeri.menu.bukkit.meta.MenuItemMeta;
import eu.okaeri.menu.bukkit.meta.MenuMeta;
import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuProvider {

    private final Plugin plugin;
    private final Map<Inventory, MenuInstance> knownMenuMap = new ConcurrentHashMap<>();
    private @Getter @Setter boolean warnUnoptimizedRender = false;

    public static MenuProvider create(@NonNull Plugin plugin) {
        return create(plugin, 5);
    }

    public static MenuProvider create(@NonNull Plugin plugin, int updateTicks) {

        MenuProvider provider = new MenuProvider(plugin);
        MenuListener listener = new MenuListener(plugin, provider);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(listener, plugin);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, () -> provider.update(!Bukkit.isPrimaryThread()), updateTicks, updateTicks);
        scheduler.runTaskTimer(plugin, () -> provider.update(!Bukkit.isPrimaryThread()), updateTicks, updateTicks);

        return provider;
    }

    public void trackInstance(@NonNull Inventory inventory, @NonNull MenuInstance menuInstance) {
        this.knownMenuMap.put(inventory, menuInstance);
    }

    public boolean knowsInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.containsKey(inventory);
    }

    public Optional<MenuInstance> findInstance(@NonNull Inventory inventory) {
        return Optional.ofNullable(this.knownMenuMap.get(inventory));
    }

    public void removeInstance(@NonNull MenuInstance instance) {
        this.close(instance);
        this.removeInstance(instance.getInventory());
    }

    @Nullable
    public MenuInstance removeInstance(@NonNull Inventory inventory) {
        return this.knownMenuMap.remove(inventory);
    }

    public void update() {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> this.update(!Bukkit.isPrimaryThread()));
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.update(!Bukkit.isPrimaryThread()));
    }

    public void update(boolean lastRenderAsync) {
        for (MenuInstance instance : this.knownMenuMap.values()) {

            if (instance.getLastRenderTime() == null) {
                continue;
            }

            MenuMeta menuMeta = instance.getMeta();
            Duration updateRate = menuMeta.getUpdate();
            if (updateRate == null) {
                return;
            }

            if (instance.isLastRenderAsync() != lastRenderAsync) {
                return;
            }

            Instant nextRender = instance.getLastRenderTime().plus(updateRate);
            if (nextRender.isAfter(Instant.now())) {
                continue;
            }

            instance.update();
        }
    }

    public void render(@NonNull HumanEntity viewer) {
        this.findInstance(viewer.getOpenInventory().getTopInventory()).ifPresent(instance -> instance.render(viewer));
    }

    public void close(@NonNull HumanEntity viewer) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, viewer::closeInventory);
    }

    public void close(@NonNull MenuInstance instance) {
        instance.getInventory().getViewers().forEach(HumanEntity::closeInventory);
    }

    public void open(MenuInstance parent, @NonNull MenuInstance instance, @NonNull HumanEntity viewer) {

        if ((parent != null) && !parent.getMenu().getProvider().knowsInstance(parent.getInventory())) {
            // ignore due to untracked parent instance & make sure to untrack this instance too
            this.removeInstance(instance);
            this.close(parent);
            return;
        }

        if (!this.knowsInstance(instance.getInventory())) {
            // ignore due to untracked instance
            this.close(instance);
            return;
        }

        viewer.openInventory(instance.getInventory());
    }

    public void openSafely(MenuInstance parent, @NonNull MenuInstance instance, @NonNull HumanEntity viewer) {

        if (Bukkit.isPrimaryThread()) {
            this.open(parent, instance, viewer);
            return;
        }

        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.open(parent, instance, viewer));
    }

    public OkaeriMenu create(@NonNull MenuMeta menu) {

        Map<Integer, MenuItemMeta> itemMap = new LinkedHashMap<>();
        Map<Integer, MenuInputMeta> inputMap = new LinkedHashMap<>();
        Map<Integer, DisplayProvider> providerMap = new LinkedHashMap<>();
        DisplayProvider menuDisplayProvider = menu.getDisplayProvider();

        int size = menu.getMenuChestSize();
        int lastPosition = -1;

        for (MenuItemMeta item : menu.getItems()) {

            int[] positions = item.getPositionAsIntArr();

            for (int position : positions) {

                if (position == -1) {
                    position = lastPosition + 1;
                }

                if (position > size) {
                    throw new IllegalArgumentException("position cannot be greater than menu size (" + position + " > " + size + ")");
                }

                DisplayProvider itemDisplayProvider = item.getDisplayProvider();
                DisplayProvider currentDisplayProvider = (itemDisplayProvider != null)
                    ? itemDisplayProvider
                    : menuDisplayProvider;

                if (currentDisplayProvider == null) {
                    throw new IllegalArgumentException("item position does not have a displayProvider: " + position);
                }

                if (providerMap.put(position, currentDisplayProvider) != null) {
                    throw new IllegalArgumentException("item position cannot be overridden: " + position);
                }

                lastPosition = position;
                itemMap.put(position, item);
            }
        }

        for (MenuInputMeta input : menu.getInputs()) {

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

        return new OkaeriMenu(menu, itemMap, inputMap, providerMap, this);
    }
}
