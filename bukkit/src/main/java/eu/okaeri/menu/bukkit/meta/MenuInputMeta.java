package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.handler.InputHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

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
