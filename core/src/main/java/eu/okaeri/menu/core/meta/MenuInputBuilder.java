package eu.okaeri.menu.core.meta;

import eu.okaeri.menu.core.handler.InputHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class MenuInputBuilder<V, I> {

    private String position = "-1";
    private InputHandler<V, I> inputHandler;

    public MenuInputBuilder<V, I> position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuInputBuilder<V, I> position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuInputBuilder<V, I> inputHandler(@NonNull InputHandler<V, I> inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }

    public MenuInputMeta<V, I> build() {
        return new MenuInputMeta<>(this.position, this.inputHandler);
    }
}
