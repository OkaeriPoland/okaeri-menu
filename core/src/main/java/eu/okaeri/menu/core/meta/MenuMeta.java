package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import eu.okaeri.menu.core.handler.CloseHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuMeta<V, I, C> {

    @Getter private final String name;
    @Getter private final String rows;
    @Getter private final DisplayProvider<V, I, C> displayProvider;
    @Getter private final List<MenuItemMeta<V, I, C>> items;
    @Getter private final List<MenuInputMeta<V, I, C>> inputs;
    @Getter private final ClickHandler<V, I, C> outsideClickHandler;
    @Getter private final ClickHandler<V, I, C> fallbackClickHandler;
    @Getter private final CloseHandler<V> closeHandler;
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
