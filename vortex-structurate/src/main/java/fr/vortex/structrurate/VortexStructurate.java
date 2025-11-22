package fr.vortex.structrurate;

import fr.vortex.structrurate.adapters.TypeAdapter;
import fr.vortex.structrurate.annotations.ConfigVersion;
import fr.vortex.structrurate.loader.ConfigIO;
import fr.vortex.structrurate.mapper.PojoMapper;
import fr.vortex.structrurate.migrations.ConfigMigration;
import fr.vortex.structrurate.node.ConfigNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VortexStructurate<T> {

    private final Class<T> type;
    private final Path file;
    private final boolean autoSave;
    private final boolean autoUpdate;
    private final boolean preserveComments;
    private final boolean failOnValidationErrors;
    private final boolean failOnUnknownFields;

    private final PojoMapper mapper;
    private final ConfigIO io;
    private final Map<Integer, ConfigMigration> migrations;

    private final List<String> headerComments = new ArrayList<>();

    private final Object lock = new Object();

    VortexStructurate(Builder<T> b) {
        this.type = b.type;
        this.file = b.file;
        this.autoSave = b.autoSave;
        this.autoUpdate = b.autoUpdate;
        this.preserveComments = b.preserveComments;
        this.failOnValidationErrors = b.failOnValidationErrors;
        this.failOnUnknownFields = b.failOnUnknownFields;
        this.mapper = b.mapper;
        this.io = new ConfigIO();
        this.migrations = new ConcurrentHashMap<>(b.migrations);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public T load() {
        synchronized (lock) {
            ConfigIO.ReadResult rr = io.readWithHeader(file);
            headerComments.clear();
            if (preserveComments) headerComments.addAll(rr.headerComments());

            ConfigNode rawNode = new ConfigNode(rr.map());
            int current = nodeVersion(rawNode);
            int target = codeVersion();
            if (current < target) {
                for (int v = current + 1; v <= target; v++) {
                    ConfigMigration mig = migrations.get(v);
                    if (mig != null) mig.migrate(rawNode);
                }
                rawNode.set("_config_version", target);
            }

            T defaults = mapper.instantiateDefaults(type);
            ConfigNode defaultNode = mapper.toNode(defaults);
            mergeDefaults(rawNode, defaultNode);

            T obj = mapper.fromNode(type, rawNode);

            if (autoUpdate) {
                ConfigNode outNode = mapper.toNode(obj);
                if (rawNode.get("_config_version") == null) outNode.set("_config_version", codeVersion());
                Map<String, Object> outMap = new LinkedHashMap<>(outNode.asMap());
                io.writeWithHeader(file, outMap, preserveComments ? headerComments : null);
            }

            return obj;
        }
    }

    public void save(T instance) {
        synchronized (lock) {
            ConfigNode node = mapper.toNode(instance);
            if (node.get("_config_version") == null) node.set("_config_version", codeVersion());
            io.writeWithHeader(file, new LinkedHashMap<>(node.asMap()), preserveComments ? headerComments : null);
        }
    }

    public void reload() {
        synchronized (lock) {
            headerComments.clear();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerTypeAdapter(Class<?> cls, TypeAdapter<?> adapter) {
        mapper.registerAdapter((Class) cls, (TypeAdapter) adapter);
    }

    @SuppressWarnings("unchecked")
    private void mergeDefaults(ConfigNode target, ConfigNode defaults) {
        for (String k : defaults.keys()) {
            Object dv = defaults.get(k);
            if (!target.has(k)) {
                target.set(k, dv);
            } else {
                Object tv = target.get(k);
                if (dv instanceof Map && tv instanceof Map) {
                    mergeDefaults(new ConfigNode((Map<String,Object>)tv), new ConfigNode((Map<String,Object>)dv));
                }
            }
        }
    }

    private int nodeVersion(ConfigNode node) {
        Object v = node.get("_config_version");
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (Exception ignored) {}
        }
        return 1;
    }

    private int codeVersion() {
        if (type.isAnnotationPresent(ConfigVersion.class)) {
            return type.getAnnotation(ConfigVersion.class).value();
        }
        return 1;
    }

    @RequiredArgsConstructor
    public static class Builder<T> {
        private Class<T> type;
        private Path file;
        private boolean autoSave = false;
        private boolean autoUpdate = true;
        private boolean preserveComments = true;
        private boolean failOnValidationErrors = false;
        private boolean failOnUnknownFields = false;

        private final PojoMapper mapper = new PojoMapper();
        private final Map<Integer, ConfigMigration> migrations = new LinkedHashMap<>();

        public Builder<T> type(Class<T> type) { this.type = type; return this; }
        public Builder<T> file(Path file) { this.file = file; return this; }
        public Builder<T> autoSave(boolean b) { this.autoSave = b; return this; }
        public Builder<T> autoUpdate(boolean b) { this.autoUpdate = b; return this; }
        public Builder<T> preserveComments(boolean b) { this.preserveComments = b; return this; }
        public Builder<T> failOnValidationErrors(boolean b) { this.failOnValidationErrors = b; return this; }
        public Builder<T> failOnUnknownFields(boolean b) { this.failOnUnknownFields = b; return this; }

        public <C> Builder<T> registerTypeAdapter(Class<C> cls, TypeAdapter<C> adapter) {
            this.mapper.registerAdapter(cls, adapter);
            return this;
        }

        public Builder<T> registerMigration(int toVersion, ConfigMigration migration) {
            this.migrations.put(toVersion, migration);
            return this;
        }

        public VortexStructurate<T> build() {
            if (type == null) throw new IllegalStateException("type() required");
            if (file == null) throw new IllegalStateException("file() required");
            return new VortexStructurate<>(this);
        }
    }
}