package com.nutriplanner.storage;

import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A persistent collection of one entity type with an auto-increment id and any number of secondary indexes.
 * Each index maps a string key derived from the entity to its id; unique indexes reject duplicates. Index
 * maps are sorted, so prefix and range queries (for example "all diary entries of user 7 in March") are cheap.
 */
public final class EntityStore<T> {

    private static final char SEPARATOR = '\u0000';

    private final Database db;
    private final String name;
    private final EntityMapper<T> mapper;
    private final MVMap<Long, byte[]> data;
    private final List<Index<T>> indexes = new ArrayList<>();

    public EntityStore(Database db, String name, EntityMapper<T> mapper) {
        this.db = db;
        this.name = name;
        this.mapper = mapper;
        this.data = db.map("data:" + name);
    }

    public String name() {
        return name;
    }

    public Index<T> uniqueIndex(String indexName, Function<T, String> key) {
        return addIndex(indexName, true, key);
    }

    public Index<T> index(String indexName, Function<T, String> key) {
        return addIndex(indexName, false, key);
    }

    private Index<T> addIndex(String indexName, boolean unique, Function<T, String> key) {
        MVMap<String, Long> map = db.map("idx:" + name + ":" + indexName);
        Index<T> index = new Index<>(name + "." + indexName, unique, key, map);
        indexes.add(index);
        String versionKey = "index:" + index.name;
        if (db.meta(versionKey) == null || (map.sizeAsLong() == 0 && data.sizeAsLong() > 0)) {
            rebuild(index);
            db.putMeta(versionKey, "1");
        }
        return index;
    }

    private void rebuild(Index<T> index) {
        db.write(() -> {
            index.map.clear();
            for (Map.Entry<Long, byte[]> e : snapshot()) {
                T entity = mapper.fromDoc(e.getKey(), DocCodec.decode(e.getValue()));
                String key = index.keyOf(entity);
                if (key != null) index.map.put(index.compose(key, e.getKey()), e.getKey());
            }
        });
    }

    private List<Map.Entry<Long, byte[]>> snapshot() {
        List<Map.Entry<Long, byte[]>> list = new ArrayList<>();
        Cursor<Long, byte[]> cursor = data.cursor(null);
        while (cursor.hasNext()) {
            Long key = cursor.next();
            list.add(Map.entry(key, cursor.getValue()));
        }
        return list;
    }

    // ------------------------------------------------------------------ writes

    public T insert(T entity) {
        return db.write(() -> {
            long id = db.nextId(name);
            mapper.assignId(entity, id);
            checkUnique(entity, id);
            data.put(id, DocCodec.encode(mapper.toDoc(entity)));
            addToIndexes(entity, id);
            return entity;
        });
    }

    public T update(T entity) {
        return db.write(() -> {
            long id = mapper.id(entity);
            byte[] existing = data.get(id);
            if (existing == null) throw new StorageException(name + " #" + id + " does not exist");
            T old = mapper.fromDoc(id, DocCodec.decode(existing));
            checkUnique(entity, id);
            removeFromIndexes(old, id);
            data.put(id, DocCodec.encode(mapper.toDoc(entity)));
            addToIndexes(entity, id);
            return entity;
        });
    }

    /** Inserts entities without an id (id 0) and updates the rest. */
    public T save(T entity) {
        return mapper.id(entity) == 0 ? insert(entity) : update(entity);
    }

    public boolean delete(long id) {
        return db.write(() -> {
            byte[] existing = data.get(id);
            if (existing == null) return false;
            removeFromIndexes(mapper.fromDoc(id, DocCodec.decode(existing)), id);
            data.remove(id);
            return true;
        });
    }

    public int deleteWhere(Predicate<T> filter) {
        return db.write(() -> {
            int removed = 0;
            for (T entity : findAll()) {
                if (filter.test(entity) && delete(mapper.id(entity))) removed++;
            }
            return removed;
        });
    }

    private void checkUnique(T entity, long id) {
        for (Index<T> index : indexes) {
            if (!index.unique) continue;
            String key = index.keyOf(entity);
            if (key == null) continue;
            Long owner = index.map.get(key);
            if (owner != null && owner != id) throw new DuplicateKeyException(index.name, key);
        }
    }

    private void addToIndexes(T entity, long id) {
        for (Index<T> index : indexes) {
            String key = index.keyOf(entity);
            if (key != null) index.map.put(index.compose(key, id), id);
        }
    }

    private void removeFromIndexes(T entity, long id) {
        for (Index<T> index : indexes) {
            String key = index.keyOf(entity);
            if (key != null) index.map.remove(index.compose(key, id));
        }
    }

    // ------------------------------------------------------------------- reads

    public Optional<T> findById(long id) {
        return db.read(() -> {
            byte[] bytes = data.get(id);
            return bytes == null ? Optional.<T>empty() : Optional.of(mapper.fromDoc(id, DocCodec.decode(bytes)));
        });
    }

    public List<T> findAll() {
        return db.read(() -> {
            List<T> list = new ArrayList<>();
            for (Map.Entry<Long, byte[]> e : snapshot()) {
                list.add(mapper.fromDoc(e.getKey(), DocCodec.decode(e.getValue())));
            }
            return list;
        });
    }

    public List<T> find(Predicate<T> filter) {
        List<T> out = new ArrayList<>();
        for (T entity : findAll()) {
            if (filter.test(entity)) out.add(entity);
        }
        return out;
    }

    public long count() {
        return db.read(data::sizeAsLong);
    }

    public Optional<T> findUnique(Index<T> index, String key) {
        requireOwn(index);
        if (!index.unique) throw new IllegalArgumentException(index.name + " is not unique");
        return db.read(() -> {
            Long id = index.map.get(key);
            return id == null ? Optional.<T>empty() : findById(id);
        });
    }

    /** All entities whose exact index key equals {@code key}, ordered by id. */
    public List<T> findByKey(Index<T> index, String key) {
        requireOwn(index);
        if (index.unique) return findUnique(index, key).map(List::of).orElse(List.of());
        return findByPrefix(index, key + SEPARATOR);
    }

    /** All entities whose index key starts with {@code prefix}, in key order. */
    public List<T> findByPrefix(Index<T> index, String prefix) {
        requireOwn(index);
        return db.read(() -> {
            List<T> out = new ArrayList<>();
            Cursor<String, Long> cursor = index.map.cursor(prefix);
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (!key.startsWith(prefix)) break;
                findById(cursor.getValue()).ifPresent(out::add);
            }
            return out;
        });
    }

    /** All entities whose index key is in [{@code fromInclusive}, {@code toExclusive}), in key order. */
    public List<T> findRange(Index<T> index, String fromInclusive, String toExclusive) {
        requireOwn(index);
        return db.read(() -> {
            List<T> out = new ArrayList<>();
            Cursor<String, Long> cursor = index.map.cursor(fromInclusive);
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (key.compareTo(toExclusive) >= 0) break;
                findById(cursor.getValue()).ifPresent(out::add);
            }
            return out;
        });
    }

    private void requireOwn(Index<T> index) {
        if (!indexes.contains(index)) throw new IllegalArgumentException(index.name + " does not belong to " + name);
    }

    /** A secondary index definition bound to its storage map. */
    public static final class Index<T> {
        private final String name;
        private final boolean unique;
        private final Function<T, String> keyFunction;
        private final MVMap<String, Long> map;

        private Index(String name, boolean unique, Function<T, String> keyFunction, MVMap<String, Long> map) {
            this.name = name;
            this.unique = unique;
            this.keyFunction = keyFunction;
            this.map = map;
        }

        private String keyOf(T entity) {
            return keyFunction.apply(entity);
        }

        private String compose(String key, long id) {
            return unique ? key : key + SEPARATOR + String.format(java.util.Locale.ROOT, "%019d", id);
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Index && ((Index<?>) o).name.equals(name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
