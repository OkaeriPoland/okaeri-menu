package eu.okaeri.menu.test.example;

import eu.okaeri.menu.Menu;
import eu.okaeri.menu.item.Fillers;
import eu.okaeri.menu.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.StaticPane.staticPane;

/**
 * Examples demonstrating interactive slots.
 * Shows different patterns for slots that allow item pickup and placement.
 */
public class InteractiveSlotExample {

    /**
     * Creates a simple item deposit/withdraw menu.
     * Player can place items and take them back.
     * Interactive slots manage their own state - no reactive materials needed!
     */
    public static Menu createDepositBoxMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<gold>Deposit Box")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .filler(Fillers.GRAY_GLASS_PANE)
                // Slot 1: Fully interactive (pickup + placement)
                // No material/name specified - starts empty, displays whatever item is placed
                .item(1, 1, item()
                    .interactive()  // Allows both pickup and placement
                    .onItemChange(ctx -> {
                        if (ctx.wasItemPlaced()) {
                            ctx.sendMessage("<green>Item deposited!");
                            ctx.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                        } else if (ctx.wasItemRemoved()) {
                            ctx.sendMessage("<yellow>Item withdrawn!");
                            ctx.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                        } else if (ctx.wasItemSwapped()) {
                            ctx.sendMessage("<aqua>Item swapped!");
                        } else if (ctx.wasItemAmountIncreased()) {
                            ctx.sendMessage("<green>Added to stack!");
                            ctx.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        } else if (ctx.wasItemAmountDecreased()) {
                            ctx.sendMessage("<yellow>Took some items!");
                            ctx.playSound(Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.9f);
                        }
                    })
                    .build())
                // Slot 2: Another interactive slot
                .item(3, 1, item()
                    .interactive()
                    .build())
                // Slot 3: Another interactive slot
                .item(5, 1, item()
                    .interactive()
                    .build())
                // Info button
                .item(8, 0, item()
                    .material(Material.BOOK)
                    .name("<yellow>Info")
                    .lore("""
                        <gray>You can place items in the
                        <gray>empty slots and take them back.
                        
                        <green>Try it out!""")
                    .build())
                .build())
            .build();
    }

    /**
     * Creates a trading/shop menu where player places items to sell.
     */
    public static Menu createSellShopMenu(Plugin plugin) {
        int[] totalValue = {0};

        return Menu.builder(plugin)
            .title("<green>Sell Items")
            .rows(4)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 4)
                .filler(Fillers.GRAY_GLASS_PANE)
                // Info button
                .item(0, 0, item()
                    .material(Material.BOOK)
                    .name("<yellow>Item Values")
                    .lore("""
                        <gray>Place items in the
                        <gray>empty slots to sell
                        
                        <green>Item Values:
                        <aqua>Diamond: <yellow>100 coins
                        <aqua>Emerald: <yellow>75 coins
                        <aqua>Gold Ingot: <yellow>50 coins
                        <aqua>Iron Ingot: <yellow>25 coins
                        <aqua>Coal: <yellow>10 coins
                        <aqua>Other: <yellow>5 coins
                        
                        <gray>Click the Sell button!""")
                    .build())
                // Input slots (placement only)
                .item(1, 1, createSellSlot(totalValue))
                .item(2, 1, createSellSlot(totalValue))
                .item(3, 1, createSellSlot(totalValue))
                .item(4, 1, createSellSlot(totalValue))
                .item(5, 1, createSellSlot(totalValue))
                // Value display
                .item(7, 1, item()
                    .material(Material.EMERALD)
                    .name("<green>Total Value")
                    .lore("""
                            <gray>Coins: <yellow><value>
                            
                            <gray>Place items to see value""",
                        ctx -> Map.of("value", totalValue[0]))
                    .build())
                // Sell button
                .item(7, 2, item()
                    .material(Material.LIME_CONCRETE)
                    .name("<green><b>Sell All Items")
                    .lore("""
                            <gray>Total: <yellow><value> coins
                            
                            <yellow>Click to confirm sale!""",
                        ctx -> Map.of("value", totalValue[0]))
                    .onClick(ctx -> {
                        if (totalValue[0] > 0) {
                            ctx.sendMessage("<green>Sold items for " + totalValue[0] + " coins!");
                            ctx.playSound(Sound.ENTITY_PLAYER_LEVELUP);
                            // Clear slots and reset value
                            totalValue[0] = 0;
                            ctx.refresh();
                        } else {
                            ctx.sendMessage("<red>No items to sell!");
                            ctx.playSound(Sound.ENTITY_VILLAGER_NO);
                        }
                    })
                    .build())
                .build())
            .build();
    }

    private static MenuItem createSellSlot(int[] totalValue) {
        return item()
            .allowPlacement(true)  // Can only place, not take back
            .onItemChange(ctx -> {
                // Calculate value of all items
                int value = 0;
                if (ctx.getNewItem() != null) {
                    value += calculateItemValue(ctx.getNewItem());
                }
                totalValue[0] = value * 10; // Simplified calculation
                ctx.sendMessage("<yellow>Item value: " + value + " coins");
                ctx.refresh();  // Update value display
            })
            .build();
    }

    /**
     * Creates an input/output processor menu (e.g., crafting, smelting).
     */
    public static Menu createProcessorMenu(Plugin plugin) {
        ItemStack[] inputSlots = new ItemStack[2];

        return Menu.builder(plugin)
            .title("<aqua>Item Processor")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .filler(Fillers.CYAN_GLASS_PANE)
                // Info button
                .item(0, 0, item()
                    .material(Material.BOOK)
                    .name("<yellow>Crafting Recipes")
                    .lore("""
                        <gray>Place items in the two
                        <gray>empty slots to craft
                        
                        <aqua>Available Recipes:
                        <yellow>Iron Ingot + Stick
                        <white>  → Iron Sword
                        <yellow>Diamond + Stick
                        <white>  → Diamond Sword
                        
                        <gray>Result appears on the right!""")
                    .build())
                // Input slot 1 - can place and remove
                .item(2, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        // Clone the new item to track it independently
                        inputSlots[0] = ctx.getNewItem() != null ? ctx.getNewItem().clone() : null;
                        ItemStack output = processItems(inputSlots);
                        // Auto-detect pane from event context - no need to specify "main"
                        ctx.setSlotItem(6, 1, output);
                    })
                    .build())
                // Input slot 2 - can place and remove
                .item(3, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        // Clone the new item to track it independently
                        inputSlots[1] = ctx.getNewItem() != null ? ctx.getNewItem().clone() : null;
                        ItemStack output = processItems(inputSlots);
                        // Auto-detect pane from event context - no need to specify "main"
                        ctx.setSlotItem(6, 1, output);
                    })
                    .build())
                // Process arrow
                .item(4, 1, item()
                    .material(Material.ARROW)
                    .name("<yellow>Processing...")
                    .lore("""
                        <gray>Place items in the
                        <gray>empty slots to craft
                        
                        <gray>Examples:
                        <yellow>Iron + Stick = Iron Sword
                        <yellow>Diamond + Stick = Diamond Sword""")
                    .build())
                // Output slot - can only take
                .item(6, 1, item()
                    .allowPickup(true)  // Can only take, not place
                    .onItemChange(ctx -> {
                        if (ctx.wasItemRemoved()) {
                            // Consume 1 item from each input slot
                            if (inputSlots[0] != null && inputSlots[0].getAmount() > 1) {
                                inputSlots[0].setAmount(inputSlots[0].getAmount() - 1);
                                ctx.setSlotItem(2, 1, inputSlots[0]);
                            } else {
                                inputSlots[0] = null;
                                ctx.setSlotItem(2, 1, null);
                            }

                            if (inputSlots[1] != null && inputSlots[1].getAmount() > 1) {
                                inputSlots[1].setAmount(inputSlots[1].getAmount() - 1);
                                ctx.setSlotItem(3, 1, inputSlots[1]);
                            } else {
                                inputSlots[1] = null;
                                ctx.setSlotItem(3, 1, null);
                            }

                            // Recalculate output for remaining items
                            ItemStack newOutput = processItems(inputSlots);
                            ctx.setSlotItem(6, 1, newOutput);

                            ctx.sendMessage("<green>✓ Crafted item! <gray>(<gold><remaining><gray> materials remaining)",
                                Map.of("remaining", (inputSlots[0] != null ? inputSlots[0].getAmount() : 0) +
                                    (inputSlots[1] != null ? inputSlots[1].getAmount() : 0)));
                            ctx.playSound(Sound.ENTITY_ITEM_PICKUP);
                        }
                    })
                    .build())
                .build())
            .build();
    }

    /**
     * Creates an armor/equipment display menu with swappable slots.
     * Shows syncing with player's actual equipment.
     */
    public static Menu createEquipmentMenu(Plugin plugin, Player player) {
        return Menu.builder(plugin)
            .title("<gold>Equipment")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .filler(Fillers.GRAY_GLASS_PANE)
                // Info button
                .item(0, 0, item()
                    .material(Material.BOOK)
                    .name("<yellow>Equipment Slots")
                    .lore("""
                        <gray>Place armor pieces in
                        <gray>the four empty slots
                        
                        <gray>Your actual equipment
                        <gray>will be updated!
                        
                        <yellow>Try equipping armor!""")
                    .build())
                // Helmet slot
                .item(2, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        player.getInventory().setHelmet(ctx.getNewItem());
                        ctx.sendMessage("<green>Helmet equipped!");
                    })
                    .build())
                // Chestplate slot
                .item(3, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        player.getInventory().setChestplate(ctx.getNewItem());
                        ctx.sendMessage("<green>Chestplate equipped!");
                    })
                    .build())
                // Leggings slot
                .item(5, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        player.getInventory().setLeggings(ctx.getNewItem());
                        ctx.sendMessage("<green>Leggings equipped!");
                    })
                    .build())
                // Boots slot
                .item(6, 1, item()
                    .interactive()
                    .onItemChange(ctx -> {
                        player.getInventory().setBoots(ctx.getNewItem());
                        ctx.sendMessage("<green>Boots equipped!");
                    })
                    .build())
                .build())
            .build();
    }

    /**
     * Creates a validation example - only accepts specific items.
     */
    public static Menu createValidationMenu(Plugin plugin) {
        return Menu.builder(plugin)
            .title("<red>Validation Example")
            .rows(3)
            .pane("main", staticPane()
                .name("main")
                .bounds(0, 0, 9, 3)
                .filler(Fillers.RED_GLASS_PANE)
                // Info button
                .item(0, 0, item()
                    .material(Material.BOOK)
                    .name("<yellow>Validation Rules")
                    .lore("""
                        <gray>This menu shows how to
                        <gray>validate placed items
                        
                        <aqua>Left slot:
                        <yellow>Only accepts Diamonds
                        <gray>(rejects other items)
                        
                        <aqua>Right slot:
                        <yellow>Only accepts 1 item
                        <gray>(rejects stacks)
                        
                        <red>Try placing invalid items!""")
                    .build())
                // Only accepts diamonds
                .item(2, 1, item()
                    .allowPlacement(true)
                    .onItemChange(ctx -> {
                        if (ctx.wasItemPlaced()) {
                            ItemStack placed = ctx.getNewItem();
                            if (placed.getType() != Material.DIAMOND) {
                                // Reject the item
                                ctx.cancel();
                                ctx.sendMessage("<red>Only diamonds allowed!");
                                ctx.playSound(Sound.ENTITY_VILLAGER_NO);
                            } else {
                                ctx.sendMessage("<green>Diamond accepted!");
                                ctx.playSound(Sound.ENTITY_PLAYER_LEVELUP);
                            }
                        }
                    })
                    .build())
                // Only accepts max stack size of 1
                .item(6, 1, item()
                    .allowPlacement(true)
                    .onItemChange(ctx -> {
                        if (ctx.wasItemPlaced()) {
                            ItemStack placed = ctx.getNewItem();
                            if (placed.getAmount() > 1) {
                                ctx.cancel();
                                ctx.sendMessage("<red>Only one item at a time!");
                                ctx.playSound(Sound.ENTITY_VILLAGER_NO);
                            } else {
                                ctx.sendMessage("<green>Item accepted!");
                            }
                        }
                    })
                    .build())
                .build())
            .build();
    }

    // Utility methods

    private static int calculateItemValue(ItemStack item) {
        if (item == null) return 0;

        return switch (item.getType()) {
            case DIAMOND -> 100;
            case EMERALD -> 75;
            case GOLD_INGOT -> 50;
            case IRON_INGOT -> 25;
            case COAL -> 10;
            default -> 5;
        };
    }

    private static ItemStack processItems(ItemStack[] inputs) {
        if (inputs[0] != null && inputs[1] != null) {
            // Simple example: combine two items
            if (inputs[0].getType() == Material.IRON_INGOT && inputs[1].getType() == Material.STICK) {
                return new ItemStack(Material.IRON_SWORD);
            } else if (inputs[0].getType() == Material.DIAMOND && inputs[1].getType() == Material.STICK) {
                return new ItemStack(Material.DIAMOND_SWORD);
            }
        }
        return null;
    }
}
