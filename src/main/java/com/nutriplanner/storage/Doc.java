package com.nutriplanner.storage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A self-describing record of named fields. Entities convert themselves to and from documents, which the
 * {@link DocCodec} stores in a compact binary form. Named fields keep stored data readable after the entity
 * classes evolve: unknown fields are ignored and missing fields fall back to defaults.
 */
public final class Doc {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public Doc put(String key, Object value) {
        if (value == null) {
            fields.remove(key);
            return this;
        }
        fields.put(key, normalize(value));
        return this;
    }

    Map<String, Object> fields() {
        return Collections.unmodifiableMap(fields);
    }

    void putRaw(String key, Object value) {
        fields.put(key, value);
    }

    public boolean has(String key) {
        return fields.containsKey(key);
    }

    public String getString(String key) {
        Object v = fields.get(key);
        return v == null ? null : v.toString();
    }

    public String getString(String key, String fallback) {
        String v = getString(key);
        return v == null ? fallback : v;
    }

    public long getLong(String key, long fallback) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).longValue() : fallback;
    }

    public Long getLongOrNull(String key) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    public int getInt(String key, int fallback) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).intValue() : fallback;
    }

    public Integer getIntOrNull(String key) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).intValue() : null;
    }

    public double getDouble(String key, double fallback) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : fallback;
    }

    public Double getDoubleOrNull(String key) {
        Object v = fields.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : null;
    }

    public boolean getBool(String key, boolean fallback) {
        Object v = fields.get(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    public LocalDate getDate(String key) {
        Object v = fields.get(key);
        return v instanceof LocalDate ? (LocalDate) v : null;
    }

    public Instant getInstant(String key) {
        Object v = fields.get(key);
        return v instanceof Instant ? (Instant) v : null;
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E fallback) {
        String name = getString(key);
        if (name == null) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public <E extends Enum<E>> EnumSet<E> getEnumSet(String key, Class<E> type) {
        EnumSet<E> set = EnumSet.noneOf(type);
        for (Object o : getList(key)) {
            try {
                set.add(Enum.valueOf(type, o.toString()));
            } catch (IllegalArgumentException ignored) {
                // value from an older version of the enum: skip it
            }
        }
        return set;
    }

    public Doc getDoc(String key) {
        Object v = fields.get(key);
        return v instanceof Doc ? (Doc) v : null;
    }

    public List<Object> getList(String key) {
        Object v = fields.get(key);
        if (v instanceof List<?>) return new ArrayList<>((List<?>) v);
        return new ArrayList<>();
    }

    public List<Doc> getDocList(String key) {
        List<Doc> docs = new ArrayList<>();
        for (Object o : getList(key)) {
            if (o instanceof Doc) docs.add((Doc) o);
        }
        return docs;
    }

    public List<Long> getLongList(String key) {
        List<Long> out = new ArrayList<>();
        for (Object o : getList(key)) {
            if (o instanceof Number) out.add(((Number) o).longValue());
        }
        return out;
    }

    private static Object normalize(Object value) {
        if (value instanceof String || value instanceof Long || value instanceof Double || value instanceof Boolean
                || value instanceof LocalDate || value instanceof Instant || value instanceof Doc) {
            return value;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) return ((Number) value).longValue();
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Enum<?>) return ((Enum<?>) value).name();
        if (value instanceof Collection<?>) {
            List<Object> list = new ArrayList<>();
            for (Object o : (Collection<?>) value) {
                if (o != null) list.add(normalize(o));
            }
            return list;
        }
        throw new IllegalArgumentException("Unsupported document value type: " + value.getClass().getName());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Doc && ((Doc) o).fields.equals(fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public String toString() {
        return "Doc" + fields;
    }
}
