package eu.okaeri.menu.bukkit.integration;

import eu.okaeri.menu.Menu;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.okaeri.menu.item.MenuItem.item;
import static eu.okaeri.menu.pane.PaginatedPane.pane;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expected-behavior spec for the PLAYER'S OWN inventory while a read-only menu is open.
 * <p>
 * Scenario modelled here: a "crate preview" style menu - a fully populated, paginated,
 * read-only menu (no interactive slots). While such a menu is open the player must still
 * be able to freely manage the items in their OWN (bottom) inventory. The menu may only
 * block actions that would move items across the boundary into/out of the menu.
 * <p>
 * Each test is written as the EXPECTED behavior, not the current one - a failure here is a
 * real usability bug in {@code MenuListener#handlePlayerInventoryClickWhileMenuOpen}.
 */
class PlayerInventoryInteractionTest extends MenuListenerTestBase {

    private static final int ROWS = 6;
    private static final int MENU_SIZE = ROWS * 9; // top inventory raw slots: 0..53

    /**
     * Opens a read-only, fully-populated paginated menu - the structural equivalent of the
     * crate preview (display items only, no interactive/pickup/placement slots).
     */
    private Menu openPreviewMenu() {
        List<String> rewards = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            rewards.add("Reward " + i);
        }

        Menu menu = Menu.builder(this.plugin)
            .title("Crate Preview")
            .rows(ROWS)
            .pane(pane(String.class)
                .name("rewards")
                .bounds(0, 0, ROWS, 9)
                .items(rewards)
                .renderer((ctx, reward, index) -> item()
                    .material(Material.DIAMOND)
                    .name(reward)
                    .build()) // no allowPickup / allowPlacement -> non-interactive
                .build())
            .build();

        menu.open(this.player);
        return menu;
    }

    /** A raw slot that falls inside the player's OWN inventory (below the open menu). */
    private int ownInventoryRawSlot() {
        return MENU_SIZE + 4;
    }

    private InventoryClickEvent ownInventoryClick(InventoryAction action) {
        return new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            this.ownInventoryRawSlot(),
            ClickType.LEFT,
            action
        );
    }

    @Test
    @DisplayName("Sanity: an own-inventory raw slot resolves to the bottom inventory, not the menu")
    void sanityClickResolvesToPlayerInventory() {
        this.openPreviewMenu();

        InventoryClickEvent event = this.ownInventoryClick(InventoryAction.PICKUP_ALL);
        Inventory top = this.player.getOpenInventory().getTopInventory();

        assertThat(event.getClickedInventory())
            .as("raw slot %d must resolve to the player's own inventory", this.ownInventoryRawSlot())
            .isNotNull()
            .isNotSameAs(top);
    }

    @Test
    @DisplayName("CRUX: a plain left-click pickup in the player's own inventory must NOT be cancelled")
    void plainLeftClickPickupIsAllowed() {
        this.openPreviewMenu();

        InventoryClickEvent event = this.ownInventoryClick(InventoryAction.PICKUP_ALL);
        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("the player must be able to pick up their own items with a normal left-click while a preview is open")
            .isFalse();
    }

    @ParameterizedTest(name = "own-inventory action {0} must be allowed")
    @EnumSource(value = InventoryAction.class, names = {
        "PICKUP_ALL", "PICKUP_HALF", "PICKUP_ONE", "PICKUP_SOME",
        "PLACE_ALL", "PLACE_ONE", "PLACE_SOME",
        "SWAP_WITH_CURSOR",
        "DROP_ONE_SLOT", "DROP_ALL_SLOT",
        "HOTBAR_SWAP", "HOTBAR_MOVE_AND_READD",
        "NOTHING"
    })
    @DisplayName("Own-inventory management actions must NOT be cancelled")
    void ownInventoryManagementIsAllowed(InventoryAction action) {
        this.openPreviewMenu();

        InventoryClickEvent event;
        if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
            // number-key swap targeting a hotbar slot that is ALSO in the player's own inventory
            event = new InventoryClickEvent(
                this.player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                this.ownInventoryRawSlot(),
                ClickType.NUMBER_KEY,
                action,
                3 // hotbar button 0..8, the swap stays inside the player's inventory
            );
        } else {
            event = this.ownInventoryClick(action);
        }

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("action %s only touches the player's own inventory and must not be blocked while a read-only menu is open", action)
            .isFalse();
    }

    @Test
    @DisplayName("Drag confined to the player's own inventory must NOT be cancelled")
    void dragWithinOwnInventoryIsAllowed() {
        this.openPreviewMenu();

        InventoryDragEvent event = new InventoryDragEvent(
            this.player.getOpenInventory(),
            null,
            new ItemStack(Material.COBBLESTONE),
            false,
            Map.of(
                MENU_SIZE + 4, new ItemStack(Material.COBBLESTONE),
                MENU_SIZE + 5, new ItemStack(Material.COBBLESTONE)
            )
        );

        this.listener.onInventoryDrag(event);

        assertThat(event.isCancelled())
            .as("spreading items across the player's own slots must work while a preview is open")
            .isFalse();
    }

    @Test
    @DisplayName("Shift-click from own inventory must be cancelled (would dump items into a read-only menu)")
    void shiftClickIntoMenuIsBlocked() {
        this.openPreviewMenu();

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            this.ownInventoryRawSlot(),
            ClickType.SHIFT_LEFT,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("shift-click while a chest GUI is open sends the item into the top inventory - it must be blocked for a read-only preview")
            .isTrue();
    }

    @Test
    @DisplayName("Double-click collect-to-cursor must be cancelled (would pull items out of the menu)")
    void collectToCursorIsBlocked() {
        this.openPreviewMenu();

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            this.ownInventoryRawSlot(),
            ClickType.DOUBLE_CLICK,
            InventoryAction.COLLECT_TO_CURSOR
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("collect-to-cursor would sweep matching items out of the menu and must be blocked")
            .isTrue();
    }

    @Test
    @DisplayName("Drag that touches a menu slot must be cancelled")
    void dragIntoMenuIsBlocked() {
        this.openPreviewMenu();

        InventoryDragEvent event = new InventoryDragEvent(
            this.player.getOpenInventory(),
            null,
            new ItemStack(Material.COBBLESTONE),
            false,
            Map.of(
                3, new ItemStack(Material.COBBLESTONE),             // a menu slot
                MENU_SIZE + 4, new ItemStack(Material.COBBLESTONE)  // an own-inventory slot
            )
        );

        this.listener.onInventoryDrag(event);

        assertThat(event.isCancelled())
            .as("a drag that places items into the menu must be blocked")
            .isTrue();
    }

    @ParameterizedTest(name = "number-key {0} on a MENU slot must still be cancelled")
    @EnumSource(value = InventoryAction.class, names = {"HOTBAR_SWAP", "HOTBAR_MOVE_AND_READD"})
    @DisplayName("Number-key swap targeting a menu slot must stay blocked (no pulling items out of the menu)")
    void hotbarSwapOnMenuSlotIsBlocked(InventoryAction action) {
        this.openPreviewMenu();

        // raw slot 4 is inside the menu (top inventory) and holds a non-interactive display item;
        // pressing a number key here would swap that menu item into the player's hotbar.
        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.CONTAINER,
            4,
            ClickType.NUMBER_KEY,
            action,
            3
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("a number-key swap hovering a menu slot would move a menu item into the player's hotbar and must stay blocked")
            .isTrue();
    }

    @Test
    @DisplayName("Clicking outside the window while a menu is open must be cancelled (no accidental drops)")
    void clickOutsideWhileMenuOpenIsBlocked() {
        this.openPreviewMenu();

        InventoryClickEvent event = new InventoryClickEvent(
            this.player.getOpenInventory(),
            InventoryType.SlotType.OUTSIDE,
            -999,
            ClickType.LEFT,
            InventoryAction.DROP_ALL_CURSOR
        );

        this.listener.onInventoryClick(event);

        assertThat(event.isCancelled())
            .as("dropping the cursor stack outside the window while a menu is open must be blocked")
            .isTrue();
    }
}
