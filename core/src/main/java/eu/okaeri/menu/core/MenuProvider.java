package eu.okaeri.menu.core;

import eu.okaeri.menu.core.meta.MenuDeclaration;
import lombok.NonNull;

public interface MenuProvider<V, I, M> {

    M create(@NonNull MenuDeclaration<V, I> menu);
}
