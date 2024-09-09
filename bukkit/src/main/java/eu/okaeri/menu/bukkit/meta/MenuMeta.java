package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.display.DisplayProvider;
import eu.okaeri.menu.bukkit.handler.ClickHandler;
import eu.okaeri.menu.bukkit.handler.CloseHandler;
import eu.okaeri.menu.bukkit.handler.UpdateHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.Inventory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuMeta {

    @Getter private final Function<MenuMeta, Inventory> factory;
    @Getter private final String name;
    @Getter private final String rows;
    @Getter private final Duration update;
    @Getter private final UpdateHandler updateHook;
    @Getter private final DisplayProvider displayProvider;
    @Getter private final List<MenuItemMeta> items;
    @Getter private final List<MenuInputMeta> inputs;
    @Getter private final ClickHandler outsideClickHandler;
    @Getter private final ClickHandler fallbackClickHandler;
    @Getter private final CloseHandler closeHandler;
    private Integer menuChestSizeCache = null;

    public int getRowsAsInt() {
        try {
            return Integer.parseInt(this.rows);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("cannot parse rows: " + this.rows);
        }
    }

    public int getMenuChestSize() {

        if (this.menuChestSizeCache != null) {
            return this.menuChestSizeCache;
        }

        int size = this.getRowsAsInt() * 9;
        if (size < 0) {
            int maxPosition = this.items.stream()
                .flatMapToInt(meta -> Arrays.stream(meta.getPositionAsIntArr()))
                .max()
                .orElse(9);
            if (maxPosition == -1) {
                maxPosition = this.items.size();
            }
            size = (int) (9d * (Math.ceil(Math.abs((double) maxPosition / 9d))));
        }

        this.menuChestSizeCache = size;
        return size;
    }
}
