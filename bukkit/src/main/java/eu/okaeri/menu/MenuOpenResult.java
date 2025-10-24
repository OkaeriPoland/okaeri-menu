package eu.okaeri.menu;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Result of a menu open operation.
 * Provides detailed information about async data loading and open status.
 *
 * <p>Use this to determine if a menu opened successfully, how long it took,
 * and which async components succeeded or failed.
 *
 * <p>Example usage:
 * <pre>{@code
 * menu.open(player, Duration.ofSeconds(5))
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             player.sendMessage("Menu opened!");
 *         } else {
 *             player.sendMessage("Failed: " + result.getStatus());
 *         }
 *     });
 * }</pre>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MenuOpenResult {

    /**
     * The final status of the open operation.
     */
    @NonNull
    private final OpenStatus status;

    /**
     * Time elapsed from open call to completion.
     */
    @NonNull
    private final Duration elapsed;

    /**
     * Async components that loaded successfully.
     * Empty if no async components or immediate open.
     */
    @NonNull
    private final Set<String> successfulComponents;

    /**
     * Async components that failed to load.
     * Empty if all succeeded or immediate open.
     */
    @NonNull
    private final Set<String> failedComponents;

    /**
     * Async components still loading when menu opened (timeout case).
     * Empty if not timeout or all completed.
     */
    @NonNull
    private final Set<String> pendingComponents;

    /**
     * Optional error that caused the operation to fail.
     */
    private final Throwable error;

    // ========================================
    // Factory Methods
    // ========================================

    /**
     * Creates result for immediate open (no async components).
     */
    static MenuOpenResult immediate() {
        return new MenuOpenResult(
            OpenStatus.IMMEDIATE,
            Duration.ZERO,
            Set.of(),
            Set.of(),
            Set.of(),
            null
        );
    }

    /**
     * Creates result for successful preload.
     */
    static MenuOpenResult preloaded(Duration elapsed, Set<String> successful, Set<String> failed) {
        return new MenuOpenResult(
            OpenStatus.PRELOADED,
            elapsed,
            successful,
            failed,
            Set.of(),
            null
        );
    }

    /**
     * Creates result for timeout.
     */
    static MenuOpenResult timeout(Duration elapsed, Set<String> successful, Set<String> failed, Set<String> pending) {
        return new MenuOpenResult(
            OpenStatus.TIMEOUT,
            elapsed,
            successful,
            failed,
            pending,
            null
        );
    }

    /**
     * Creates result for player offline.
     */
    static MenuOpenResult playerOffline(Duration elapsed) {
        return new MenuOpenResult(
            OpenStatus.PLAYER_OFFLINE,
            elapsed,
            Set.of(),
            Set.of(),
            Set.of(),
            null
        );
    }

    /**
     * Creates result for error.
     */
    static MenuOpenResult error(Throwable error) {
        return new MenuOpenResult(
            OpenStatus.ERROR,
            Duration.ZERO,
            Set.of(),
            Set.of(),
            Set.of(),
            error
        );
    }

    // ========================================
    // Convenience Methods
    // ========================================

    /**
     * Returns true if the menu successfully opened (immediate, preloaded, or timeout).
     * False if player went offline or error occurred.
     */
    public boolean isSuccess() {
        return (this.status == OpenStatus.IMMEDIATE)
            || (this.status == OpenStatus.PRELOADED)
            || (this.status == OpenStatus.TIMEOUT);
    }

    /**
     * Returns true if menu did not open.
     */
    public boolean isFailure() {
        return !this.isSuccess();
    }

    /**
     * Returns true if all async components loaded successfully.
     * Returns true for immediate open (no async components).
     */
    public boolean isFullyLoaded() {
        return this.failedComponents.isEmpty() && this.pendingComponents.isEmpty();
    }

    /**
     * Returns true if some components failed or are still pending.
     */
    public boolean hasPartialData() {
        return !this.failedComponents.isEmpty() || !this.pendingComponents.isEmpty();
    }

    /**
     * Gets the error if present.
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(this.error);
    }

    /**
     * Gets total async component count.
     */
    public int getTotalComponents() {
        return this.successfulComponents.size()
            + this.failedComponents.size()
            + this.pendingComponents.size();
    }

    /**
     * Gets success rate (0.0 to 1.0).
     * Returns 1.0 for immediate open (no async components).
     */
    public double getSuccessRate() {
        int total = this.getTotalComponents();
        if (total == 0) {
            return 1.0;
        }
        return (double) this.successfulComponents.size() / total;
    }

    @Override
    public String toString() {
        return String.format("MenuOpenResult{status=%s, elapsed=%dms, success=%d, failed=%d, pending=%d}",
            this.status,
            this.elapsed.toMillis(),
            this.successfulComponents.size(),
            this.failedComponents.size(),
            this.pendingComponents.size()
        );
    }
}
