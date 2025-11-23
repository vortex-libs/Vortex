package fr.vortex.structrurate.mapper;

import fr.vortex.structrurate.adapters.TypeAdapter;
import fr.vortex.structrurate.annotations.ConfigConverter;
import fr.vortex.structrurate.annotations.ConfigIgnore;
import fr.vortex.structrurate.annotations.ConfigKey;
import fr.vortex.structrurate.node.ConfigNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PojoMapper {

    private static final Logger log = Logger.getLogger(PojoMapper.class.getName());

    private final Map<Class<?>, Map<String, Field>> fieldsCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, TypeAdapter<?>> adapters = new ConcurrentHashMap<>();

    public <T> void registerAdapter(Class<T> type, TypeAdapter<T> adapter) {
        adapters.put(type, adapter);
    }

    public <T> T instantiateDefaults(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate default of " + clazz, e);
        }
    }

    private Map<String, Field> fieldsFor(Class<?> clazz) {
        return fieldsCache.computeIfAbsent(clazz, c -> {
            Map<String, Field> m = new LinkedHashMap<>();
            Class<?> cur = c;
            while (cur != null && cur != Object.class) {
                for (Field f : cur.getDeclaredFields()) {
                    if (f.isAnnotationPresent(ConfigIgnore.class)) continue;
                    f.setAccessible(true);
                    String key = f.getName();
                    ConfigKey ck = f.getAnnotation(ConfigKey.class);
                    if (ck != null && !ck.value().isEmpty()) key = ck.value();
                    if (!m.containsKey(key)) m.put(key, f);
                }
                cur = cur.getSuperclass();
            }
            return m;
        });
    }

    public <T> T fromNode(Class<T> clazz, ConfigNode node) {
        T instance = instantiateDefaults(clazz);
        Map<String, Field> fields = fieldsFor(clazz);
        for (Map.Entry<String, Field> e : fields.entrySet()) {
            String key = e.getKey();
            Field f = e.getValue();
            Object raw = node.get(key);
            try {
                Object value;
                if (raw == null) {
                    continue;
                }
                TypeAdapter<?> adapter = findAdapterForField(f);
                if (adapter != null) {
                    value = adapter.fromYaml(raw);
                } else {
                    value = convertValue(raw, f.getType(), f.getGenericType());
                }
                f.set(instance, value);
            } catch (Exception ex) {
                log.warning("Failed to set field " + f.getName() + " on " + clazz.getSimpleName() + " from raw " + raw + ": " + ex.toString());
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public ConfigNode toNode(Object instance) {
        ConfigNode root = new ConfigNode();
        Map<String, Field> fields = fieldsFor(instance.getClass());
        for (Map.Entry<String, Field> e : fields.entrySet()) {
            String key = e.getKey();
            Field f = e.getValue();
            try {
                Object val = f.get(instance);
                if (val == null) continue;
                TypeAdapter adapter = findAdapterForField(f);
                Object out;
                if (adapter != null) {
                    out = adapter.toYaml(val);
                } else {
                    out = serializeValue(val, f.getType(), f.getGenericType());
                }
                root.set(key, out);
            } catch (Exception ex) {
                log.warning("Failed to read field " + f.getName() + " on " + instance.getClass().getSimpleName() + ": " + ex.toString());
            }
        }
        return root;
    }

    private TypeAdapter<?> findAdapterForField(Field f) {
        ConfigConverter conv = f.getAnnotation(ConfigConverter.class);
        if (conv != null) {
            try {
                Object o = conv.value().getDeclaredConstructor().newInstance();
                if (o instanceof TypeAdapter) return (TypeAdapter<?>) o;
            } catch (Exception ignored) { }
        }
        return adapters.get(f.getType());
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(Object raw, Class<?> target, Type genericType) {
        if (raw == null || target.isInstance(raw)) return raw;

        if (target == String.class) return raw.toString();

        if (raw instanceof Number num) {
            if (target == int.class || target == Integer.class) return num.intValue();
            if (target == long.class || target == Long.class) return num.longValue();
            if (target == double.class || target == Double.class) return num.doubleValue();
            if (target == float.class || target == Float.class) return num.floatValue();
        }

        if (target == boolean.class || target == Boolean.class) {
            return raw instanceof Boolean ? raw : Boolean.parseBoolean(raw.toString());
        }

        if (target.isEnum()) {
            String s = raw.toString();
            for (Object c : target.getEnumConstants()) {
                if (((Enum<?>) c).name().equalsIgnoreCase(s)) return c;
            }
            return Enum.valueOf((Class<Enum>) target, s);
        }

        if (Collection.class.isAssignableFrom(target) && raw instanceof Collection) {
            Class<?> elementType = Object.class;
            if (genericType instanceof ParameterizedType) {
                Type t = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (t instanceof Class) elementType = (Class<?>) t;
            }
            List<Object> out = new ArrayList<>();
            for (Object r : (Collection<?>) raw) {
                out.add(convertValue(r, elementType, elementType));
            }
            return out;
        }

        if (Map.class.isAssignableFrom(target) && raw instanceof Map) {
            return raw;
        }

        if (target == UUID.class) return UUID.fromString(raw.toString());
        if (target == Instant.class) return Instant.parse(raw.toString());
        if (target == Duration.class) return Duration.parse(raw.toString());

        if (raw instanceof Map) {
            ConfigNode node = new ConfigNode((Map<String, Object>) raw);
            return fromNode(target, node);
        }
        return raw;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object serializeValue(Object val, Class<?> type, Type genericType) {
        if (val == null) return null;
        if (val instanceof Number || val instanceof Boolean || val instanceof String || val instanceof Enum) return val;

        if (val instanceof UUID || val instanceof Instant || val instanceof Duration) return val.toString();

        if (val instanceof Collection coll) {
            return coll.stream()
                    .map(o -> serializeValue(o, o.getClass(), o.getClass()))
                    .collect(Collectors.toList());
        }

        if (val instanceof Map) {
            return val;
        }

        return toNode(val).asMap();
    }
}
