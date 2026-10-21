package androidx.media3.common;

import java.util.function.Consumer;

public interface DiscMenuController {

    enum OpenResult {
        RUNTIME_MISSING,
        UNAVAILABLE,
        TIMED_OUT
    }

    default boolean isNavigationPlayback() {
        return false;
    }

    default boolean isActive() {
        return false;
    }

    default boolean supportsPointer() {
        return false;
    }

    default boolean sendAction(String action) {
        return false;
    }

    default boolean open(String action, Consumer<OpenResult> callback) {
        return false;
    }
}
