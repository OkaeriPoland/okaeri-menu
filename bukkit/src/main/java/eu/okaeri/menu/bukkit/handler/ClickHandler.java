package eu.okaeri.menu.bukkit.handler;

import eu.okaeri.menu.bukkit.MenuContext;
import lombok.NonNull;

public interface ClickHandler {
    void onClick(@NonNull MenuContext ctx);
}
