package com.nutriplanner.storage;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Embedded database built on H2's MVStore engine (a pure-Java, crash-safe, append-only storage engine).
 * <p>
 * All writes go through {@link #write(Supplier)}: a write transaction is serialized with a write lock, committed
 * atomically when the outermost call returns, and rolled back if it throws. Reads take a shared lock so they
 * never observe a half-applied transaction.
 */
public final class Database implements AutoCloseable {

    private final MVStore store;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final MVMap<String, Long> sequences;
    private final MVMap<String, String> meta;
    private int depth;

    private Database(MVStore store) {
        this.store = store;
        this.sequences = store.openMap("_sequences");
        this.meta = store.openMap("_meta");
    }

    /** Opens (or creates) a database file. */
    public static Database open(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new StorageException("Cannot create data directory for " + file, e);
        }
        MVStore store = new MVStore.Builder()
                .fileName(file.toAbsolutePath().toString())
                .autoCommitDisabled()
                .compress()
                .open();
        return new Database(store);
    }

    /** Creates a throw-away database that lives only in memory (used by tests). */
    public static Database inMemory() {
        return new Database(new MVStore.Builder().autoCommitDisabled().open());
    }

    public <K, V> MVMap<K, V> map(String name) {
        return store.openMap(name);
    }

    /** Runs {@code work} inside a write transaction. Nested calls join the outer transaction. */
    public <T> T write(Supplier<T> work) {
        lock.writeLock().lock();
        depth++;
        boolean success = false;
        try {
            T result = work.get();
            success = true;
            return result;
        } finally {
            depth--;
            try {
                if (depth == 0) {
                    if (success) store.commit();
                    else store.rollback();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public void write(Runnable work) {
        write(() -> {
            work.run();
            return null;
        });
    }

    /** Runs {@code work} with a consistent view of committed data. */
    public <T> T read(Supplier<T> work) {
        lock.readLock().lock();
        try {
            return work.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Allocates the next id of a named sequence. Must be called inside a write transaction. */
    public long nextId(String sequence) {
        requireWrite();
        Long current = sequences.get(sequence);
        long next = current == null ? 1 : current + 1;
        sequences.put(sequence, next);
        return next;
    }

    public String meta(String key) {
        return meta.get(key);
    }

    public void putMeta(String key, String value) {
        write(() -> {
            meta.put(key, value);
        });
    }

    void requireWrite() {
        if (!lock.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("This operation must run inside Database.write(...)");
        }
    }

    boolean inWrite() {
        return lock.isWriteLockedByCurrentThread();
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (!store.isClosed()) {
                store.commit();
                store.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
