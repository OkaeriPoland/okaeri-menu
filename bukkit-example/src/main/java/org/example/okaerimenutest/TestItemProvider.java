package org.example.okaerimenutest;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.meta.MenuItemMeta;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TestItemProvider implements DisplayProvider<HumanEntity, ItemStack> {

    @Override
    public ItemStack displayFor(HumanEntity viewer, MenuItemMeta menuItem) {
        ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if ("rainbow2".equals(menuItem.getDisplay())) {
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
        } else {
            itemMeta.addEnchant(Enchantment.FIRE_ASPECT, 1, false);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
