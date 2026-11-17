package com.ecom.dto;

import com.ecom.repository.ProductCatalogRepository;

/**
 * Cleaned-up shop listing parameters. Records compare by value, so this is
 * also the cache key: the same filters always hit the same cache entry.
 */
public record CatalogQuery(String category, String keyword, String sort, int page, int size) {

    public static final int DEFAULT_SIZE = 24;
    public static final int MAX_SIZE = 60;
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_TEXT = 100;

    public static CatalogQuery of(String category, String keyword, String sort, Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, Math.min(page, MAX_PAGE));
        int s = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));
        String so = sort != null && ProductCatalogRepository.SORTS.contains(sort) ? sort : "featured";
        return new CatalogQuery(clean(category), clean(keyword), so, p, s);
    }

    private static String clean(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }
}