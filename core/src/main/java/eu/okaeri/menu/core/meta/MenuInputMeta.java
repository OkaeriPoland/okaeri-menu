package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.handler.InputHandler;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuInputMeta<V, I, C> {

    private final String position;
    private final InputHandler<V, I, C> inputHandler;

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
