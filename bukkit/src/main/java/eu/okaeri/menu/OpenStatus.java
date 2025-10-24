package eu.okaeri.menu;

/**
 * Status of a menu open operation.
 * Indicates whether the menu opened successfully and how async data was loaded.
 */
public enum OpenStatus {

    /**
     * Menu opened immediately with no async components.
     * All data was available synchronously.
     */
    IMMEDIATE,

    /**
     * Menu opened after all async components loaded successfully.
     * Data was fully preloaded before showing to player.
     */
    PRELOADED,

    /**
     * Menu opened on timeout with partial data.
     * Some async components may still be loading or in error state.
     */
    TIMEOUT,

    /**
     * Menu did not open because player went offline during loading.
     */
    PLAYER_OFFLINE,

    /**
     * Menu did not open due to an error.
     */
    ERROR
}
