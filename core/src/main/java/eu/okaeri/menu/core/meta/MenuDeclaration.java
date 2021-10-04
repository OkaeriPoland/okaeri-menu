package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.MenuHandler;
import eu.okaeri.menu.core.annotation.Menu;
import eu.okaeri.menu.core.annotation.MenuItem;
import eu.okaeri.menu.core.display.DisplayProvider;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuDeclaration<V, I> {

    @SneakyThrows
    public static <V, I, H extends MenuHandler> MenuDeclaration<V, I> of(@NonNull Class<H> clazz) {
        return of(clazz, clazz.newInstance());
    }

    @SneakyThrows
    public static <V, I, H extends MenuHandler> MenuDeclaration<V, I> of(@NonNull Class<H> clazz, @NonNull H handler) {

        Menu menu = clazz.getAnnotation(Menu.class);
        if (menu == null) {
            throw new IllegalArgumentException("cannot create MenuDeclaration from class without @Menu: " + clazz);
        }

        @SuppressWarnings("unchecked") DisplayProvider<V, I> displayProvider = (menu.displayProvider() == Menu.DEFAULT_DISPLAY_PROVIDER.class)
                ? null :
                menu.displayProvider().newInstance();

        List<MenuItemDeclaration<V, I>> items = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.getAnnotation(MenuItem.class) != null)
                .map(method -> MenuItemDeclaration.<V, I>of(handler, method))
                .collect(Collectors.toList());

        return new MenuDeclaration<V, I>(menu.name(), menu.rows(), displayProvider, Collections.unmodifiableList(items));
    }

    @Getter private final String name;
    @Getter private final String rows;
    @Getter private final DisplayProvider<V, I> displayProvider;
    @Getter private final List<MenuItemDeclaration<V, I>> items;
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
                    .flatMapToInt(declaration -> Arrays.stream(declaration.getPositionAsIntArr()))
                    .max()
                    .orElse(9);
            size = (int) (9d * (Math.ceil(Math.abs((double) maxPosition / 9d))));
        }

        this.menuChestSizeCache = size;
        return size;
    }
}
