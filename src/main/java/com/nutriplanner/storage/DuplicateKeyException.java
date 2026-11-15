package com.nutriplanner.storage;

/** Thrown when an insert or update would break a unique index. */
public class DuplicateKeyException extends StorageException {
    private final String index;

    public DuplicateKeyException(String index, String key) {
        super("Duplicate value for unique index '" + index + "': " + key);
        this.index = index;
    }

    public String index() {
        return index;
    }
}
