package eu.okaeri.menu.core;

import eu.okaeri.menu.core.meta.MenuMeta;
import lombok.NonNull;

public interface MenuProvider<V, I, M> {

    M create(@NonNull MenuMeta<V, I> menu);
}
