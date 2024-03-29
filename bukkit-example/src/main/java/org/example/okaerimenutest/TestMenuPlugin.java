package org.example.okaerimenutest;

import eu.okaeri.menu.bukkit.MenuInstance;
import eu.okaeri.menu.bukkit.MenuProvider;
import eu.okaeri.menu.bukkit.OkaeriMenu;
import eu.okaeri.menu.bukkit.meta.MenuMeta;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TestMenuPlugin extends JavaPlugin implements Listener {

    private MenuProvider menuProvider;
    private MenuMeta testMenu;

    @Override
    public void onEnable() {

        this.menuProvider = MenuProvider.create(this);
        this.testMenu = OkaeriMenu.builder()
            .name("Color selector 2")
            .close(viewer -> viewer.sendMessage("Closed menu!"))
            .outsideClick(ctx -> ctx.sendMessage("Clicked outside with " + ctx.getItem() + "!"))
            .fallbackClick(ctx -> {
                ctx.sendMessage("clicked " + ctx.getItem() + "");
                ctx.setAllowPickup(true); // allow pickup from non-static elements (e.g the one from input on position 4)
            })
            .item(OkaeriMenu.item()
                .display(() -> new ItemStack(Material.STONE))
                .position(0)
                .click(ctx -> ctx.sendMessage("gray smokes"))
                .build())
            .item(OkaeriMenu.item()
                .display(() -> new ItemStack(Material.WOOD))
                .position(3)
                .click(ctx -> ctx.sendMessage("rainbow smokes"))
                .build())
            .input(OkaeriMenu.input()
                .position(4)
                .handle((viewer, menuInput, cursor, item, slot) -> {

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
            .item(OkaeriMenu.item()
                .display(() -> new ItemStack(Material.YELLOW_FLOWER))
                .position(5)
                .click(ctx -> ctx.sendMessage("rainbow 2 smokes"))
                .build())
            .item(OkaeriMenu.item()
                .display(() -> new ItemStack(Material.REDSTONE))
                .position(8)
                .click(ctx -> ctx.sendMessage("red smokes"))
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

        // compute async (normally not required in async events
        // but we want to free chat thread pool from non-chat work)
        this.getServer().getScheduler().runTask(this, () -> {

            // compute & pre-render
            OkaeriMenu createdInstance = this.menuProvider.create(this.testMenu);
            MenuInstance renderedInstance = createdInstance.render(event.getPlayer());

            // open sync
            this.getServer().getScheduler().runTask(this, () -> {
                renderedInstance.open(event.getPlayer());
                this.getLogger().info("Opened menu " + renderedInstance.getMeta().getName() + " to the " + event.getPlayer().getName());
            });
        });
    }
}
