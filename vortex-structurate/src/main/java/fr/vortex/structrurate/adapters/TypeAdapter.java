package fr.vortex.structrurate.adapters;

public interface TypeAdapter<T> {
    Object toYaml(T obj);
    T fromYaml(Object raw);
}
