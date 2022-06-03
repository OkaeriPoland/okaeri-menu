package eu.okaeri.menu.core.handler;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface FallbackClickHandler<V, I, C> {
    boolean onClick(@NonNull V viewer, @Nullable I item, int slot, @NonNull C clickType);
}
