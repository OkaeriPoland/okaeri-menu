package eu.okaeri.menu.core.handler;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface OutsideClickHandler<V, I, C> {
    void onClick(@NonNull V viewer, @Nullable I cursor, @NonNull C clickType);
}
