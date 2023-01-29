package eu.okaeri.menu.core.handler;

import lombok.NonNull;

public interface CloseHandler<V, C> {
    void onClose(@NonNull C ctx);
}
