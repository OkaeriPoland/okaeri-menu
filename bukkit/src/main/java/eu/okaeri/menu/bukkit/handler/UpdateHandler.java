package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuInstance;
import lombok.NonNull;
import org.bukkit.entity.HumanEntity;

public interface UpdateHandler {

    void onUpdate(@NonNull MenuInstance instance, @NonNull HumanEntity viewer);
}
