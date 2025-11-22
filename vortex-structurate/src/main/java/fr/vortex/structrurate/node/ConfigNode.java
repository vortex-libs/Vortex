package fr.vortex.structrurate.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record ConfigNode(Map<String, Object> map) {

    public ConfigNode() {
        this(new LinkedHashMap<>());
    }

    public ConfigNode(Map<String, Object> map) {
        this.map = map instanceof LinkedHashMap ? map : new LinkedHashMap<>(map);
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    public ConfigNode node(String key) {
        Object v = map.get(key);
        if (v instanceof Map) {
            return new ConfigNode((Map<String, Object>) v);
        } else {
            Map<String, Object> m = new LinkedHashMap<>();
            map.put(key, m);
            return new ConfigNode(m);
        }
    }

    public void set(String key, Object value) {
        map.put(key, value);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(map);
    }
}
