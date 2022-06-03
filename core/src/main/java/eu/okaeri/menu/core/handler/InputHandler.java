package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuInputMeta;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface InputHandler<V, I, C> {
    boolean onInput(@NonNull V viewer, @NonNull MenuInputMeta menuInput, @NonNull I cursor, @Nullable I item, int slot);
}
