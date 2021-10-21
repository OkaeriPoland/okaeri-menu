package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuInputMeta;
import lombok.NonNull;

public interface InputHandler<V, I> {
    boolean onInput(@NonNull V viewer, @NonNull MenuInputMeta menuInput, @NonNull I item);
}
