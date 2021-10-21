package eu.okaeri.menu.core.handler;

import lombok.NonNull;

public interface CloseHandler<V> {
    void onClose(@NonNull V viewer);
}
