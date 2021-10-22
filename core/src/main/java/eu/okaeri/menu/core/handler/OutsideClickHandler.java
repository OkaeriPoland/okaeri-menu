package eu.okaeri.menu.core.handler;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface OutsideClickHandler<V, I> {
    void onClick(@NonNull V viewer, @Nullable I cursor);
}
