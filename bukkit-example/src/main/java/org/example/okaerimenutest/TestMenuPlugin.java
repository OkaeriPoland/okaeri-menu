package org.example.okaerimenutest;

import eu.okaeri.menu.bukkit.BukkitMenu;
import eu.okaeri.menu.bukkit.BukkitMenuInstance;
import eu.okaeri.menu.bukkit.BukkitMenuProvider;
import eu.okaeri.menu.core.meta.MenuMeta;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
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
                .outsideClickHandler((viewer) -> viewer.sendMessage("Clicked outside!"))
                .fallbackClickHandler(((viewer, menuItem, item) -> viewer.sendMessage("clicked " + menuItem + " (" + item + ")")))
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
                        .inputHandler((viewer, menuInput, item) -> {
                            viewer.sendMessage("input into " + menuInput + ": " + item);
                            return true;
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
