package com.nutriplanner.storage;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Describes how an entity type maps to documents and where its id lives. */
public interface EntityMapper<T> {

    long id(T entity);

    void assignId(T entity, long id);

    Doc toDoc(T entity);

    T fromDoc(long id, Doc doc);

    static <T> EntityMapper<T> of(ToLongFunction<T> id, BiConsumer<T, Long> assignId,
                                  Function<T, Doc> toDoc, BiFunctionWithId<T> fromDoc) {
        return new EntityMapper<>() {
            @Override
            public long id(T entity) {
                return id.applyAsLong(entity);
            }

            @Override
            public void assignId(T entity, long value) {
                assignId.accept(entity, value);
            }

            @Override
            public Doc toDoc(T entity) {
                return toDoc.apply(entity);
            }

            @Override
            public T fromDoc(long value, Doc doc) {
                return fromDoc.apply(value, doc);
            }
        };
    }

    @FunctionalInterface
    interface BiFunctionWithId<T> {
        T apply(long id, Doc doc);
    }
}
