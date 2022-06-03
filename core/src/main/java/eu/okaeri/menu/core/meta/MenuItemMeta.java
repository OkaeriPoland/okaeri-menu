package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.display.DisplayProvider;
import eu.okaeri.menu.core.handler.ClickHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuItemMeta<V, I, C> {

    private static final MethodHandles.Lookup HANDLE_LOOKUP = MethodHandles.lookup();

    private final String position;
    private final ClickHandler<V, I, C> clickHandler;
    private final DisplayProvider<V, I, C> displayProvider;

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
