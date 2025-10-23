package eu.okaeri.menu.bukkit.meta;

import eu.okaeri.menu.bukkit.handler.InputHandler;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Builder for constructing {@link MenuInputMeta} instances in the legacy 1.x API.
 * <p>
 * This class is part of the compatibility layer and will be removed in a future version.
 * For new code, use the 2.0 API with {@link eu.okaeri.menu.item.MenuItem} and input handling instead.
 *
 * @deprecated Use {@link eu.okaeri.menu.item.MenuItem} with input handling in the 2.0 API
 * @see MenuInputMeta
 * @see eu.okaeri.menu.item.MenuItem
 */
@Deprecated(since = "0.0.18", forRemoval = true)
@NoArgsConstructor
public class MenuInputBuilder {

    private String position = "-1";
    private InputHandler inputHandler;

    /**
     * Sets the position(s) for this input slot.
     * <p>
     * Can be a single slot (e.g., "10") or multiple slots (e.g., "10,11,12").
     *
     * @param position the slot position(s) as a string
     * @return this builder for method chaining
     */
    public MenuInputBuilder position(@NonNull String position) {
        this.position = position;
        return this;
    }

    /**
     * Sets the position for this input slot.
     *
     * @param position the slot position as an integer
     * @return this builder for method chaining
     */
    public MenuInputBuilder position(int position) {
        this.position = String.valueOf(position);
        return this;
    }

    /**
     * Sets the input handler for this input slot.
     *
     * @param inputHandler the handler to be called when input is received
     * @return this builder for method chaining
     */
    public MenuInputBuilder input(@NonNull InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }

    /**
     * Legacy alias for {@link #input(InputHandler)}.
     *
     * @param inputHandler the handler to be called when input is received
     * @return this builder for method chaining
     * @deprecated Use {@link #input(InputHandler)} instead
     */
    @Deprecated
    public MenuInputBuilder handle(@NonNull InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        return this;
    }

    /**
     * Builds and returns a new {@link MenuInputMeta} instance with the configured properties.
     *
     * @return a new MenuInputMeta instance
     */
    public MenuInputMeta build() {
        return new MenuInputMeta(this.position, this.inputHandler);
    }
}
