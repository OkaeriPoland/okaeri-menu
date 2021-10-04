package org.example.okaerimenutest;

import eu.okaeri.menu.core.MenuHandler;
import eu.okaeri.menu.core.annotation.Menu;
import eu.okaeri.menu.core.annotation.MenuInput;
import eu.okaeri.menu.core.annotation.MenuItem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

@Menu(name = "Color selector")
public class TestMenu extends MenuHandler {

    @MenuItem(display = "stone", name = "Gray", position = "0,1")
    public void gray(HumanEntity viewer) {
        viewer.sendMessage("selected gray!");
    }

    @MenuItem(displayProvider = TestItemProvider.class, name = "Rainbow", position = "3")
    public void rainbow(HumanEntity viewer) {
        viewer.sendMessage("selected rainbow!");
    }

    @MenuInput(position = "4")
    public void input(HumanEntity viewer, ItemStack input) {
        viewer.sendMessage("thanks for your " + input);
    }

    @MenuItem(display = "rainbow2", displayProvider = TestItemProvider.class, name = "Rainbow 2", position = "5")
    public void rainbow2(HumanEntity viewer) {
        viewer.sendMessage("selected rainbow 2!");
    }

    @MenuItem(display = "redstone_block", name = "Red", position = "7,8")
    public void red(HumanEntity viewer) {
        viewer.sendMessage("selected red!");
    }
}
