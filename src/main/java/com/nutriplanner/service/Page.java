package com.nutriplanner.service;

import java.util.List;

/** One page of a longer result list. Pages are numbered from 1. */
public record Page<T>(List<T> items, int page, int pageSize, int total) {

    public static <T> Page<T> of(List<T> all, int page, int pageSize) {
        int size = Math.max(1, pageSize);
        int pages = Math.max(1, (all.size() + size - 1) / size);
        int current = Math.min(Math.max(1, page), pages);
        int from = (current - 1) * size;
        int to = Math.min(all.size(), from + size);
        return new Page<>(List.copyOf(all.subList(from, to)), current, size, all.size());
    }

    public int totalPages() {
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages();
    }

    public int firstIndex() {
        return total == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    public int lastIndex() {
        return Math.min(total, page * pageSize);
    }
}
