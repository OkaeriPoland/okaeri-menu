package eu.okaeri.menu.state;

import lombok.NonNull;

import java.util.Map;

/**
 * State defaults configuration.
 * Used in Menu.builder().state() to define default values.
 */
public class StateDefaults {
    private final Map<String, Object> defaults;

    public StateDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }

    /**
     * Defines a default value for a state variable.
     *
     * @param key          The state variable name
     * @param defaultValue The default value
     * @return This configurator
     */
    public StateDefaults define(@NonNull String key, @NonNull Object defaultValue) {
        this.defaults.put(key, defaultValue);
        return this;
    }
}
