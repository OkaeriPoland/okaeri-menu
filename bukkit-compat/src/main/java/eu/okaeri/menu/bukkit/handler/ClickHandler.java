package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuContext;
import lombok.NonNull;

/**
 * @deprecated Use the new 2.0 API with {@link eu.okaeri.menu.item.MenuItem.Builder#onClick(java.util.function.Consumer)}
 */
@Deprecated(since = "0.0.18", forRemoval = true)
public interface ClickHandler {
    void onClick(@NonNull MenuContext ctx);
}
