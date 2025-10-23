package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.handler.InputHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Holds input slot configuration data for the legacy 1.x API.
 * <p>
 * This class is part of the compatibility layer and will be removed in a future version.
 * For new code, use the 2.0 API with {@link eu.okaeri.menu.item.MenuItem} and input handling instead.
 *
 * @deprecated Use {@link eu.okaeri.menu.item.MenuItem} with input handling in the 2.0 API
 * @see eu.okaeri.menu.item.MenuItem
 */
@Deprecated(since = "0.0.18", forRemoval = true)
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuInputMeta {

    private final String position;
    private final InputHandler inputHandler;

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
