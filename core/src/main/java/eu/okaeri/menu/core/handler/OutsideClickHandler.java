package eu.okaeri.menu.core.handler;

import lombok.NonNull;

public interface OutsideClickHandler<V> {
    void onClick(@NonNull V viewer);
}
