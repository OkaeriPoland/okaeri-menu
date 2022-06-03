package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.handler.InputHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class MenuInputBuilder<V, I, C> {

    private String position = "-1";
    private InputHandler<V, I, C> inputHandler;

    public MenuInputBuilder<V, I, C> position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuInputBuilder<V, I, C> position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuInputBuilder<V, I, C> inputHandler(@NonNull InputHandler<V, I, C> inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }

    public MenuInputMeta<V, I, C> build() {
        return new MenuInputMeta<>(this.position, this.inputHandler);
    }
}
