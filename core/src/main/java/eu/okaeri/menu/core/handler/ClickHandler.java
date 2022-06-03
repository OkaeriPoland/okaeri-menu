package eu.okaeri.menu.core.handler;

import lombok.NonNull;

public interface ClickHandler<V, I, C> {
    void onClick(@NonNull C ctx);
}
