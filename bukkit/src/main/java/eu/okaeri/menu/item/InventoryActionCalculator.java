package eu.okaeri.menu.item;

import lombok.NonNull;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for calculating the result of inventory actions.
 * Allows determining the new item state directly from event data without scheduled tasks.
 */
public final class InventoryActionCalculator {

    /**
     * Calculates the resulting slot item for a given inventory action.
     * Returns null if the calculation cannot be performed (complex actions).
     *
     * @param event The inventory click event
     * @return The new item that will be in the slot, or null if cannot be calculated
     */
    public static ItemStack calculateNewSlotItem(@NonNull InventoryClickEvent event) {

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        InventoryAction action = event.getAction();

        switch (action) {
            // Pickup actions - remove from slot
            case PICKUP_ALL:
                return null;

            case PICKUP_HALF:
                return calculatePickupHalf(currentItem);

            case PICKUP_ONE:
                return calculatePickupOne(currentItem);

            case PICKUP_SOME:
                return calculatePickupSome(currentItem, cursor);

            // Place actions - add to slot
            case PLACE_ALL:
                return calculatePlaceAll(currentItem, cursor);

            case PLACE_SOME:
                return calculatePlaceSome(currentItem, cursor);

            case PLACE_ONE:
                return calculatePlaceOne(currentItem, cursor);

            // Swap action
            case SWAP_WITH_CURSOR:
                return cloneOrNull(cursor);

            // Drop actions
            case DROP_ALL_SLOT:
                return null;

            case DROP_ONE_SLOT:
                return calculateDropOne(currentItem);

            // Hotbar swap
            case HOTBAR_SWAP:
                return calculateHotbarSwap(event);

            // Nothing happens
            case NOTHING:
                return cloneOrNull(currentItem);

            // Complex actions that we cannot/should not support for interactive slots
            case MOVE_TO_OTHER_INVENTORY:      // Shift-click - requires full inventory scan
            case COLLECT_TO_CURSOR:            // Double-click collect - requires full inventory scan
            case HOTBAR_MOVE_AND_READD:        // Complex inventory shuffling
            case CLONE_STACK:                  // Creative mode only
            case DROP_ALL_CURSOR:              // Doesn't affect slot
            case DROP_ONE_CURSOR:              // Doesn't affect slot
            case UNKNOWN:
            default:
                return null; // Cannot calculate - should block these actions
        }
    }

    /**
     * Checks if an action can be calculated and is supported for interactive slots.
     */
    public static boolean isActionSupported(@NonNull InventoryAction action) {
        switch (action) {
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
            case HOTBAR_SWAP:
            case NOTHING:
                return true;
            default:
                return false;
        }
    }

    // Helper calculation methods

    private static ItemStack calculatePickupHalf(ItemStack currentItem) {
        if (isEmpty(currentItem)) {
            return null;
        }

        int currentAmount = currentItem.getAmount();
        int remainingAmount = currentAmount / 2; // Round down for odd numbers

        if (remainingAmount == 0) {
            return null;
        }

        ItemStack result = currentItem.clone();
        result.setAmount(remainingAmount);
        return result;
    }

    private static ItemStack calculatePickupOne(ItemStack currentItem) {
        if (isEmpty(currentItem)) {
            return null;
        }

        int currentAmount = currentItem.getAmount();
        if (currentAmount <= 1) {
            return null;
        }

        ItemStack result = currentItem.clone();
        result.setAmount(currentAmount - 1);
        return result;
    }

    private static ItemStack calculatePickupSome(ItemStack currentItem, ItemStack cursor) {
        if (isEmpty(currentItem) || isEmpty(cursor)) {
            return cloneOrNull(currentItem);
        }

        if (!isSimilar(currentItem, cursor)) {
            return cloneOrNull(currentItem);
        }

        int maxStack = cursor.getMaxStackSize();
        int cursorAmount = cursor.getAmount();
        int slotAmount = currentItem.getAmount();

        int transferAmount = Math.min(slotAmount, maxStack - cursorAmount);
        int remainingAmount = slotAmount - transferAmount;

        if (remainingAmount == 0) {
            return null;
        }

        ItemStack result = currentItem.clone();
        result.setAmount(remainingAmount);
        return result;
    }

    private static ItemStack calculatePlaceAll(ItemStack currentItem, ItemStack cursor) {
        if (isEmpty(cursor)) {
            return cloneOrNull(currentItem);
        }

        // If slot is empty, just place all
        if (isEmpty(currentItem)) {
            return cursor.clone();
        }

        // If items are similar, combine them
        if (isSimilar(currentItem, cursor)) {
            ItemStack result = currentItem.clone();
            result.setAmount(currentItem.getAmount() + cursor.getAmount());
            return result;
        }

        // Different items - cannot place
        return cloneOrNull(currentItem);
    }

    private static ItemStack calculatePlaceSome(ItemStack currentItem, ItemStack cursor) {
        if (isEmpty(cursor)) {
            return cloneOrNull(currentItem);
        }

        // If slot is empty, place what fits
        if (isEmpty(currentItem)) {
            ItemStack result = cursor.clone();
            result.setAmount(Math.min(cursor.getAmount(), cursor.getMaxStackSize()));
            return result;
        }

        if (!isSimilar(currentItem, cursor)) {
            return cloneOrNull(currentItem);
        }

        int maxStack = currentItem.getMaxStackSize();
        int currentAmount = currentItem.getAmount();
        int cursorAmount = cursor.getAmount();

        int transferAmount = Math.min(cursorAmount, maxStack - currentAmount);

        ItemStack result = currentItem.clone();
        result.setAmount(currentAmount + transferAmount);
        return result;
    }

    private static ItemStack calculatePlaceOne(ItemStack currentItem, ItemStack cursor) {
        if (isEmpty(cursor)) {
            return cloneOrNull(currentItem);
        }

        // If slot is empty, place one item
        if (isEmpty(currentItem)) {
            ItemStack result = cursor.clone();
            result.setAmount(1);
            return result;
        }

        // If items are similar, add one
        if (isSimilar(currentItem, cursor)) {
            int currentAmount = currentItem.getAmount();
            int maxStack = currentItem.getMaxStackSize();

            // Check if we can add one more
            if (currentAmount >= maxStack) {
                return cloneOrNull(currentItem);
            }

            ItemStack result = currentItem.clone();
            result.setAmount(currentAmount + 1);
            return result;
        }

        // Different items, can't place
        return cloneOrNull(currentItem);
    }

    private static ItemStack calculateDropOne(ItemStack currentItem) {
        if (isEmpty(currentItem)) {
            return null;
        }

        int currentAmount = currentItem.getAmount();
        if (currentAmount <= 1) {
            return null;
        }

        ItemStack result = currentItem.clone();
        result.setAmount(currentAmount - 1);
        return result;
    }

    private static ItemStack calculateHotbarSwap(InventoryClickEvent event) {
        int hotbarButton = event.getHotbarButton();

        if ((hotbarButton < 0) || (hotbarButton > 8)) {
            return cloneOrNull(event.getCurrentItem());
        }

        HumanEntity player = event.getWhoClicked();
        ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);

        return cloneOrNull(hotbarItem);
    }

    // Helper methods

    private static ItemStack cloneOrNull(ItemStack item) {
        return ((item != null) && !item.getType().isAir()) ? item.clone() : null;
    }

    private static boolean isEmpty(ItemStack item) {
        return (item == null) || item.getType().isAir();
    }

    private static boolean isSimilar(ItemStack item1, ItemStack item2) {
        return isEmpty(item1) && isEmpty(item2) || !isEmpty(item1) && !isEmpty(item2) && item1.isSimilar(item2);
    }
}
