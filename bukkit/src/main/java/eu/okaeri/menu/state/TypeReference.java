package eu.okaeri.menu.state;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Helper class for capturing generic type information at runtime.
 * Works around Java's type erasure by using anonymous inner classes.
 *
 * <p>Usage:
 * <pre>{@code
 * List<String> names = ctx.get("names", new TypeReference<List<String>>() {});
 * Map<String, Integer> scores = ctx.get("scores", new TypeReference<Map<String, Integer>>() {});
 * }</pre>
 *
 * @param <T> The type to capture
 */
@Getter
public abstract class TypeReference<T> {

    private final Type type;

    protected TypeReference() {
        Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType parameterizedType) {
            this.type = parameterizedType.getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("TypeReference must be parameterized");
        }
    }

    /**
     * Gets the raw class for this type.
     * For generic types like List&lt;String&gt;, returns List.class.
     */
    @SuppressWarnings("unchecked")
    public Class<T> getRawType() {
        if (this.type instanceof ParameterizedType parameterizedType) {
            return (Class<T>) parameterizedType.getRawType();
        }
        if (this.type instanceof Class) {
            return (Class<T>) this.type;
        }
        throw new IllegalStateException("Cannot extract raw type from: " + this.type);
    }
}
