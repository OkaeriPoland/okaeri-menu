package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.MenuHandler;
import eu.okaeri.menu.core.annotation.MenuItem;
import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import lombok.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItemMeta<V, I> {

    private static final MethodHandles.Lookup HANDLE_LOOKUP = MethodHandles.lookup();
    private final String display;
    private final String name;
    private final String position;
    private final String description;
    private final ClickHandler<V, I> clickHandler;
    private final DisplayProvider<V, I> displayProvider;

    @SneakyThrows
    public static <V, I> MenuItemMeta<V, I> of(@NonNull MenuHandler handler, @NonNull Method method) {

        MenuItem menuItem = method.getAnnotation(MenuItem.class);
        if (menuItem == null) {
            throw new IllegalArgumentException("cannot create MenuItemMeta from method without @MenuItem: " + method);
        }

        @SuppressWarnings("unchecked") DisplayProvider<V, I> displayProvider = (menuItem.displayProvider() == MenuItem.DEFAULT_DISPLAY_PROVIDER.class)
            ? null
            : menuItem.displayProvider().newInstance();

        ClickHandler<V, I> clickHandler = new ClickHandler<V, I>() {

            private boolean resolved = false;
            private int viewerIndex = -1;
            private int menuItemIndex = -1;
            private int itemIndex = -1;
            private int slotIndex = -1;

            private Parameter[] methodParameters = method.getParameters();
            private int methodParameterCount = method.getParameterCount();
            private MethodHandle methodHandle = HANDLE_LOOKUP.unreflect(method).bindTo(handler);

            @Override
            @SneakyThrows
            public boolean onClick(@NonNull V viewer, @NonNull MenuItemMeta<V, I> menuItem, I item, int slot) {

                if (this.resolved) {
                    Object[] call = new Object[this.methodParameterCount];
                    if (this.viewerIndex != -1) call[this.viewerIndex] = viewer;
                    if (this.menuItemIndex != -1) call[this.menuItemIndex] = menuItem;
                    if (this.itemIndex != -1) call[this.itemIndex] = item;
                    if (this.slotIndex != -1) call[this.slotIndex] = slot;
                    Object result = this.methodHandle.invokeWithArguments(call);
                    return (result instanceof Boolean) && (boolean) result;
                }

                for (int index = 0; index < this.methodParameterCount; index++) {

                    Parameter parameter = this.methodParameters[index];
                    Class<?> parameterType = parameter.getType();

                    if (parameterType.isAssignableFrom(viewer.getClass()) && (this.viewerIndex == -1)) {
                        this.viewerIndex = index;
                    } else if (parameterType.isAssignableFrom(item.getClass()) && (this.itemIndex == -1)) {
                        this.itemIndex = index;
                    } else if (parameterType.isAssignableFrom(menuItem.getClass()) && (this.menuItemIndex == -1)) {
                        this.menuItemIndex = index;
                    } else if (parameterType.isAssignableFrom(int.class) && (this.slotIndex == -1)) {
                        this.slotIndex = index;
                    } else {
                        throw new IllegalArgumentException("unknown or duplicate parameter: " + parameter + " of " + method);
                    }
                }

                this.resolved = true;
                return this.onClick(viewer, menuItem, item, slot);
            }
        };

        return new MenuItemMeta<>(menuItem.display(), menuItem.name(), menuItem.position(), menuItem.description(), clickHandler, displayProvider);
    }

    public int getPositionAsInt() {
        try {
            return Integer.parseInt(this.position);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("cannot parse position: " + this.position);
        }
    }

    public int[] getPositionAsIntArr() {
        try {
            return Arrays.stream(this.position.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("cannot parse position: " + this.position);
        }
    }
}
