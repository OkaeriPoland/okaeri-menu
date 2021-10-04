package org.example.okaerimenutest;

import eu.okaeri.menu.bukkit.BukkitMenu;
import eu.okaeri.menu.bukkit.BukkitMenuInstance;
import eu.okaeri.menu.bukkit.BukkitMenuProvider;
import eu.okaeri.menu.core.meta.MenuDeclaration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class TestMenuPlugin extends JavaPlugin implements Listener {

    private BukkitMenuProvider bukkitMenuProvider;
    private MenuDeclaration<HumanEntity, ItemStack> testMenu;
    private MenuDeclaration<HumanEntity, ItemStack> testMenu2;

    @Override
    public void onEnable() {

        this.bukkitMenuProvider = BukkitMenuProvider.create(this);
        this.testMenu = MenuDeclaration.of(TestMenu.class);

        this.testMenu2 = BukkitMenu.builder()
                .name("Color selector 2")
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

        MenuDeclaration<HumanEntity, ItemStack> menu = message.contains("gui2")
                ? this.testMenu2
                : this.testMenu;

        BukkitMenuInstance menuInstance = this.bukkitMenuProvider.create(menu).open(event.getPlayer());
        this.getLogger().info("Opened menu " + menuInstance.getDeclaration().getName() + " to the " + event.getPlayer().getName());
    }
}
