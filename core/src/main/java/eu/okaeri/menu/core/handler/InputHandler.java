package eu.okaeri.menu.core.handler;

import eu.okaeri.menu.core.meta.MenuInputDeclaration;
import lombok.NonNull;

public interface InputHandler<V, I> {
    void onInput(@NonNull V viewer, MenuInputDeclaration menuInput, I item);
}
