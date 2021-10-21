package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuItemMeta;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface FallbackClickHandler<V, I> extends ClickHandler<V, I> {
    @Override
    void onClick(@NonNull V viewer, @Nullable MenuItemMeta<V, I> menuItem, @Nullable I item);
}
