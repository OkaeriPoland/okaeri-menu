package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.MenuHandler;
import eu.okaeri.menu.core.annotation.Menu;
import eu.okaeri.menu.core.annotation.MenuItem;
import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.CloseHandler;
import eu.okaeri.menu.core.handler.FallbackClickHandler;
import eu.okaeri.menu.core.handler.OutsideClickHandler;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuMeta<V, I, C> {

    @Getter private final String name;
    @Getter private final String rows;
    @Getter private final DisplayProvider<V, I, C> displayProvider;
    @Getter private final List<MenuItemMeta<V, I, C>> items;
    @Getter private final List<MenuInputMeta<V, I, C>> inputs;
    @Getter private final OutsideClickHandler<V, I, C> outsideClickHandler;
    @Getter private final FallbackClickHandler<V, I, C> fallbackClickHandler;
    @Getter private final CloseHandler<V> closeHandler;
    private Integer menuChestSizeCache = null;

    @SneakyThrows
    public static <V, I, C, H extends MenuHandler> MenuMeta<V, I, C> of(@NonNull Class<H> clazz) {
        return of(clazz, clazz.newInstance());
    }

    @SneakyThrows
    public static <V, I, C, H extends MenuHandler> MenuMeta<V, I, C> of(@NonNull Class<H> clazz, @NonNull H handler) {

        Menu menu = clazz.getAnnotation(Menu.class);
        if (menu == null) {
            throw new IllegalArgumentException("cannot create MenuMeta from class without @Menu: " + clazz);
        }

        @SuppressWarnings("unchecked") DisplayProvider<V, I, C> displayProvider = (menu.displayProvider() == Menu.DEFAULT_DISPLAY_PROVIDER.class)
            ? null :
            menu.displayProvider().newInstance();

        List<MenuItemMeta<V, I, C>> items = Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> method.getAnnotation(MenuItem.class) != null)
            .map(method -> MenuItemMeta.<V, I, C>of(handler, method))
            .collect(Collectors.toList());

        // TODO: find inputs
        List<MenuInputMeta<V, I, C>> inputs = new ArrayList<>();

        // TODO: find methods for handling outsideClickHandler/fallbackClickHandler/...
        return new MenuMeta<V, I, C>(
            menu.name(),
            menu.rows(),
            displayProvider,
            Collections.unmodifiableList(items),
            Collections.unmodifiableList(inputs),
            null,
            null,
            null
        );
    }

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
            size = (int) (9d * (Math.ceil(Math.abs((double) maxPosition / 9d))));
        }

        this.menuChestSizeCache = size;
        return size;
    }
}
