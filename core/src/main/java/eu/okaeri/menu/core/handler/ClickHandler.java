package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemDeclaration;
import lombok.NonNull;

public interface ClickHandler<V, I> {
    void onClick(@NonNull V viewer, MenuItemDeclaration menuItem, I item);
}
