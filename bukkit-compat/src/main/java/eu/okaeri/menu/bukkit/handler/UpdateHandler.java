package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuInstance;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;

/**
 * @deprecated Use the new 2.0 API with reactive properties instead of update handlers
 */
@Deprecated(since = "0.0.18", forRemoval = true)
public interface UpdateHandler {
    void onUpdate(@NonNull MenuInstance instance, @NonNull HumanEntity viewer);
}
