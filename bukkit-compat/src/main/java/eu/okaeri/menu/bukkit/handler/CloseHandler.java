package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuContext;
import lombok.NonNull;

/**
 * @deprecated Use the new 2.0 API - close events are handled automatically
 */
@Deprecated(since = "0.0.18", forRemoval = true)
public interface CloseHandler {
    void onClose(@NonNull MenuContext ctx);
}
