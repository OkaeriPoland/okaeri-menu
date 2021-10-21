package eu.okaeri.menu.bukkit.display;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * stone:1 10
 */
public class InventoryDisplayProvider implements DisplayProvider<HumanEntity, ItemStack> {

    private static final Map<MenuItemMeta, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>();

    @Override
    public ItemStack displayFor(@NonNull HumanEntity viewer, @NonNull MenuItemMeta menuItem) {

        if (ITEM_CACHE.containsKey(menuItem)) {
            return ITEM_CACHE.get(menuItem).clone();
        }

        String display = menuItem.getDisplay();
        if ((display == null) || display.isEmpty()) {
            throw new IllegalArgumentException("display cannot be null or empty");
        }

        display = display.replace(" ", "_");
        display = display.toUpperCase(Locale.ROOT);

        int amount = 1;
        if (display.contains(" ")) {
            String[] parts = display.split(" ", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid display format: " + display);
            }
            display = parts[0];
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("invalid amount: " + display);
            }
        }

        short durability = 0;
        if (display.contains(":")) {
            String[] parts = display.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid display format: " + display);
            }
            String type = parts[0];
            try {
                durability = Short.parseShort(parts[1]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("invalid durability: " + display);
            }
        }

        Material material;
        try {
            material = Material.valueOf(display);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("invalid display material: " + display);
        }

        ItemStack itemStack = new ItemStack(material);
        itemStack.setAmount(amount);
        itemStack.setDurability(durability);

        String menuItemName = menuItem.getName();
        if ((menuItemName != null) && !menuItemName.isEmpty()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(menuItemName);
            itemStack.setItemMeta(itemMeta);
        }

        ITEM_CACHE.put(menuItem, itemStack.clone());
        return itemStack;
    }
}
