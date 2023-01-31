package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.handler.InputHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
public class MenuInputBuilder {

    private String position = "-1";
    private InputHandler inputHandler;

    public MenuInputBuilder position(@NonNull String position) {
        this.position = position;
        return this;
    }

    public MenuInputBuilder position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    public MenuInputBuilder handle(@NonNull InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }

    public MenuInputMeta build() {
        return new MenuInputMeta(this.position, this.inputHandler);
    }
}
