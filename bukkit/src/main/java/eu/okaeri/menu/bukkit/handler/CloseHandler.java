package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuContext;
import lombok.NonNull;

public interface CloseHandler {
    void onClose(@NonNull MenuContext ctx);
}
