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
public class MenuItemDeclaration<V, I> {

    private static final MethodHandles.Lookup HANDLE_LOOKUP = MethodHandles.lookup();

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <V, I> MenuItemDeclaration<V, I> of(@NonNull MenuHandler handler, @NonNull Method method) {

        MenuItem menuItem = method.getAnnotation(MenuItem.class);
        if (menuItem == null) {
            throw new IllegalArgumentException("cannot create MenuItemDeclaration from method without @MenuItem: " + method);
        }

        DisplayProvider<V, I> displayProvider = (menuItem.displayProvider() == MenuItem.DEFAULT_DISPLAY_PROVIDER.class)
                ? null
                : menuItem.displayProvider().newInstance();

        ClickHandler<V, I> clickHandler = new ClickHandler<V, I>() {

            private boolean resolved = false;
            private int viewerIndex = -1;
            private int menuItemIndex = -1;
            private int itemIndex = -1;

            private Parameter[] methodParameters = method.getParameters();
            private int methodParameterCount = method.getParameterCount();
            private MethodHandle methodHandle = HANDLE_LOOKUP.unreflect(method).bindTo(handler);

            @Override
            @SneakyThrows
            public void onClick(V viewer, MenuItemDeclaration menuItem, I item) {

                if (this.resolved) {
                    Object[] call = new Object[this.methodParameterCount];
                    if (this.viewerIndex != -1) call[this.viewerIndex] = viewer;
                    if (this.itemIndex != -1) call[this.itemIndex] = item;
                    if (this.menuItemIndex != -1) call[this.menuItemIndex] = menuItem;
                    this.methodHandle.invokeWithArguments(call);
                    return;
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
                    } else {
                        throw new IllegalArgumentException("unknown or duplicate parameter: " + parameter + " of " + method);
                    }
                }

                this.resolved = true;
                this.onClick(viewer, menuItem, item);
            }
        };

        return new MenuItemDeclaration<>(menuItem.display(), menuItem.name(), menuItem.position(), menuItem.description(), clickHandler, displayProvider);
    }

    private final String display;
    private final String name;
    private final String position;
    private final String description;
    private final ClickHandler<V, I> clickHandler;
    private final DisplayProvider<V, I> displayProvider;

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
