package eu.okaeri.menu.state;

import lombok.NonNull;

import java.util.Map;

/**
 * Provides default values for primitive types and their wrappers.
 */
public final class PrimitiveDefaults {

    private static final Map<Class<?>, Object> DEFAULTS = Map.of(
        Boolean.class, false,
        Integer.class, 0,
        Long.class, 0L,
        Double.class, 0.0d,
        Float.class, 0.0f,
        Short.class, (short) 0,
        Byte.class, (byte) 0,
        Character.class, '\u0000'
    );

    /**
     * Gets the default value for a primitive wrapper type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDefault(@NonNull Class<T> type) {
        return (T) DEFAULTS.get(type);
    }

    /**
     * Checks if a type has a primitive default.
     */
    public static boolean hasPrimitiveDefault(@NonNull Class<?> type) {
        return DEFAULTS.containsKey(type);
    }

    private PrimitiveDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }
}
