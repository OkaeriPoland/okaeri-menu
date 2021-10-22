package org.example.okaerimenutest;

import eu.okaeri.menu.bukkit.BukkitMenu;
import eu.okaeri.menu.bukkit.BukkitMenuInstance;
import eu.okaeri.menu.bukkit.BukkitMenuProvider;
import eu.okaeri.menu.core.meta.MenuMeta;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TestMenuPlugin extends JavaPlugin implements Listener {

    private BukkitMenuProvider bukkitMenuProvider;
    private MenuMeta<HumanEntity, ItemStack> testMenu;
    private MenuMeta<HumanEntity, ItemStack> testMenu2;

    @Override
    public void onEnable() {

        this.bukkitMenuProvider = BukkitMenuProvider.create(this);
        this.testMenu = MenuMeta.of(TestMenu.class);

        this.testMenu2 = BukkitMenu.builder()
                .name("Color selector 2")
                .closeHandler((viewer) -> viewer.sendMessage("Closed menu!"))
                .outsideClickHandler((viewer, cursor) -> viewer.sendMessage("Clicked outside with " + cursor + "!"))
                .fallbackClickHandler((viewer, item, slot) -> {
                    viewer.sendMessage("clicked " + item + "");
                    return true; // allow pickup from non-static elements (e.g the one from input on position 4)
                })
                .item(BukkitMenu.item()
                        .display("stone")
                        .name("Gray")
                        .position(0)
                        .clickHandler((viewer, item) -> viewer.sendMessage("gray smokes"))
                        .build())
                .item(BukkitMenu.item()
                        .displayProvider(new TestItemProvider())
                        .name("Rainbow")
                        .position(3)
                        .clickHandler((viewer, item) -> viewer.sendMessage("rainbow smokes"))
                        .build())
                .input(BukkitMenu.input()
                        .position(4)
                        .inputHandler((viewer, menuInput, cursor, item, slot) -> {

                            // get inventory in this tick
                            InventoryView inventory = viewer.getOpenInventory();

                            // schedule modifications of placed item for the next tick
                            //
                            // item is not null in drag event but direct modification
                            // does not effect resulting item that will be placed
                            //
                            this.getServer().getScheduler().runTask(this, () -> {

                                // get item and just for safety check nullity
                                ItemStack stack = inventory.getItem(slot);
                                if (stack == null) {
                                    return;
                                }

                                // apply modifications
                                ItemMeta itemMeta = stack.getItemMeta();
                                itemMeta.setDisplayName("GUI!");
                                stack.setItemMeta(itemMeta);
                            });

                            viewer.sendMessage("input into " + menuInput + ": " + item);
                            return true; // allow input
                        })
                        .build())
                .item(BukkitMenu.item()
                        .display("rainbow2")
                        .displayProvider(new TestItemProvider())
                        .name("Rainbow 2")
                        .position(5)
                        .clickHandler((viewer, item) -> viewer.sendMessage("rainbow 2 smokes"))
                        .build())
                .item(BukkitMenu.item()
                        .display("redstone_block")
                        .name("Red")
                        .position(8)
                        .clickHandler((viewer, item) -> viewer.sendMessage("red smokes"))
                        .build())
                .build();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void handleChat(AsyncPlayerChatEvent event) {

        String message = event.getMessage();
        if (!message.contains("gui")) {
            return;
        }

        MenuMeta<HumanEntity, ItemStack> menu = message.contains("gui2")
                ? this.testMenu2
                : this.testMenu;

        // compute async (normally not required in async events
        // but we want to free chat thread pool for non-chat work)
        this.getServer().getScheduler().runTask(this, () -> {

            // compute
            BukkitMenu createdInstance = this.bukkitMenuProvider.create(menu);

            // open sync
            this.getServer().getScheduler().runTask(this, () -> {
                BukkitMenuInstance menuInstance = createdInstance.open(event.getPlayer());
                this.getLogger().info("Opened menu " + menuInstance.getMeta().getName() + " to the " + event.getPlayer().getName());
            });
        });
    }
}
